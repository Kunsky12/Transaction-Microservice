package com.mekheainteractive.Transaction_Microservice.Service;

import com.mekheainteractive.Transaction_Microservice.DTO.TransactionDTO;
import com.mekheainteractive.Transaction_Microservice.Entity.TransactionEntity;
import com.mekheainteractive.Transaction_Microservice.Event.EventPublisherImpl;
import com.mekheainteractive.Transaction_Microservice.Repository.LedgerRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class LedgerService {

    private final PlayFabWalletService playFabService;
    private final LockService lockService;
    private final EventPublisherImpl eventPublisherImpl;
    private final LedgerRepository ledgerRepo;

    public LedgerService(PlayFabWalletService playFabService,
                         LockService lockService,
                         EventPublisherImpl eventPublisherImpl,
                         LedgerRepository ledgerRepo) {
        this.playFabService = playFabService;
        this.lockService = lockService;
        this.eventPublisherImpl = eventPublisherImpl;
        this.ledgerRepo = ledgerRepo;
    }

    @Transactional
    public TransactionDTO transfer(
            String idempotencyKey,
            String senderId,
            String senderName,
            String facebookId,
            String receiverId,
            int amount,
            String currency) {

        String lockKey = "lock:" + senderId;

        System.out.println(senderId);
        System.out.println("Sender Name" + senderName);

        if(senderId.equals(receiverId) || amount <= 0)
            throw new RuntimeException("Invalid request");

        if (!lockService.lock(lockKey))
            throw new RuntimeException("Another transaction in progress for sender: " + senderId);

        try {

            if (idempotencyKey == null || idempotencyKey.isEmpty())
                throw new RuntimeException("Missing idempotency key");

            List<TransactionEntity> existing = ledgerRepo.findBySenderIdAndIdempotencyKey(senderId, idempotencyKey);

            if (!existing.isEmpty()) {
                TransactionEntity t = existing.get(0);

                return new TransactionDTO(
                        t.getReferenceId(),
                        t.getFacebookId(),
                        t.getSenderName(),
                        Math.abs(t.getAmount()),
                        t.getCurrency(),
                        t.getTransactionDate(),
                        t.getStatus()
                );
            }

            // Validate sender balance
            int senderBalance = playFabService.getSenderCurrency(senderId);
            if (senderBalance < amount) throw new RuntimeException("Insufficient balance");

            String referenceId = UUID.randomUUID().toString();

            // Debit/Deduct sender balance
            PlayFabWalletService.CurrencyResult debitResult = playFabService.subtractCurrency(senderId, currency, amount);
            if (!debitResult.success())
                throw new RuntimeException("Failed to debit sender in PlayFab");

            // Prepare DB entries
            TransactionEntity debitEntry = new TransactionEntity();
            debitEntry.setTransactionId(UUID.randomUUID());
            debitEntry.setReferenceId(referenceId);
            debitEntry.setIdempotencyKey(idempotencyKey);
            debitEntry.setSenderName(senderName);
            debitEntry.setFacebookId(facebookId);
            debitEntry.setSenderId(senderId);
            debitEntry.setReceiverId(receiverId);
            debitEntry.setAmount(-amount);
            debitEntry.setBalanceAfter(debitResult.getBalance());
            debitEntry.setType(TransactionEntity.TransactionType.SENDER);
            debitEntry.setCurrency(currency);
            debitEntry.setStatus("PENDING");
            debitEntry.setTransactionDate(LocalDateTime.now());

            // Credit receiver in PlayFab
            PlayFabWalletService.CurrencyResult creditResult = playFabService.addCurrency(receiverId, currency, amount);

            TransactionEntity creditEntry = new TransactionEntity();
            creditEntry.setTransactionId(UUID.randomUUID());
            creditEntry.setReferenceId(referenceId);
            //creditEntry.setIdempotencyKey(idempotencyKey);
            creditEntry.setSenderName(senderName);
            creditEntry.setSenderId(senderId);
            creditEntry.setFacebookId(facebookId);
            creditEntry.setReceiverId(receiverId);
            creditEntry.setAmount(amount);
            creditEntry.setBalanceAfter(creditResult.getBalance());
            creditEntry.setType(TransactionEntity.TransactionType.RECEIVER);
            creditEntry.setCurrency(currency);
            creditEntry.setStatus("PENDING");
            creditEntry.setTransactionDate(LocalDateTime.now());

            // Save DB entries (still PENDING)
            ledgerRepo.save(debitEntry);
            ledgerRepo.save(creditEntry);

            if (!creditResult.success()) {
                // Compensation: refund sender
                playFabService.addCurrency(senderId, currency, amount);

                // Update DB entries
                debitEntry.setStatus("FAILED");
                creditEntry.setStatus("FAILED");
                ledgerRepo.save(debitEntry);
                ledgerRepo.save(creditEntry);

                throw new RuntimeException("Failed to credit receiver");
            }

            // Mark SUCCESS
            debitEntry.setStatus("SUCCESS");
            creditEntry.setStatus("SUCCESS");
            ledgerRepo.save(debitEntry);
            ledgerRepo.save(creditEntry);

            eventPublisherImpl.publishTransactionNotification(creditEntry);

            return new TransactionDTO(
                    referenceId,
                    facebookId,
                    senderName,
                    creditEntry.getAmount(),
                    currency,
                    creditEntry.getTransactionDate(),
                    "Success"
            );

        } finally {
            lockService.unlock(lockKey);
        }
    }
}
