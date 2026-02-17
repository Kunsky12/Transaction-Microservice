package com.mekheainteractive.Transaction_Microservice.Service;

import com.mekheainteractive.Transaction_Microservice.Entity.LedgerEntity;
import com.mekheainteractive.Transaction_Microservice.Repository.LedgerRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class LedgerService {

    private final PlayFabService playFabService;
    private final LockService lockService;
    private final EventPublisher eventPublisher;
    private final LedgerRepository ledgerRepo;

    public LedgerService(PlayFabService playFabService,
                         LockService lockService,
                         EventPublisher eventPublisher,
                         LedgerRepository repo) {
        this.playFabService = playFabService;
        this.lockService = lockService;
        this.eventPublisher = eventPublisher;
        this.ledgerRepo = repo;
    }

    @Transactional
    public List<LedgerEntity> transfer(String idempotencyKey,
                                       String senderId,
                                       String receiverId,
                                       int amount,
                                       String currency) {

        if (senderId == null || receiverId == null || currency == null) {
            throw new IllegalArgumentException("Sender, receiver, and currency must be non-null");
        }

        if (senderId.equals(receiverId)) {
            throw new RuntimeException("Cannot transfer to yourself");
        }

        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        // Idempotency: return existing entries if findByIdempotencyKey already processed
        List<LedgerEntity> existing = ledgerRepo.findByIdempotencyKey(idempotencyKey);
        if (!existing.isEmpty()) return existing;

        // Lock sender to prevent concurrent transactions
        String lockKey = "lock:" + senderId;
        if (!lockService.lock(lockKey)) {
            throw new RuntimeException("Another transaction in progress for sender: " + senderId);
        }

        try {
            // Get current balance safely
            int senderBalance = playFabService.getSenderCurrency(senderId);
            if (senderBalance < amount) {
                throw new RuntimeException("Insufficient balance");
            }

            String transactionId = UUID.randomUUID().toString();
            // 1️⃣ Debit sender
            int newSenderBalance = playFabService.subtractCurrency(senderId, currency, amount);

            LedgerEntity debitEntry = new LedgerEntity();
            debitEntry.setTransactionId(transactionId); // same as debit
            debitEntry.setIdempotencyKey(idempotencyKey);
            debitEntry.setSenderId(senderId);
            debitEntry.setReceiverId(receiverId);
            debitEntry.setAmount(-amount);
            debitEntry.setBalanceAfter(newSenderBalance);
            debitEntry.setType("SENDER");
            debitEntry.setCurrency(currency);
            debitEntry.setStatus("SUCCESS");
            debitEntry.setTransferredDate(LocalDateTime.now());
            ledgerRepo.save(debitEntry);

            // 2️⃣ Credit receiver
            int newReceiverBalance = playFabService.addCurrency(receiverId, currency, amount);

            if (newReceiverBalance < 0) {
                // Rollback if credit fails
                playFabService.addCurrency(senderId, currency, amount); // revert debit
                debitEntry.setStatus("REVERSED");
                ledgerRepo.save(debitEntry);
                throw new RuntimeException("Credit failed, transaction reversed");
            }

            LedgerEntity creditEntry = new LedgerEntity();
            creditEntry.setTransactionId(transactionId); // same as debit
            creditEntry.setIdempotencyKey(idempotencyKey);
            creditEntry.setSenderId(senderId);
            creditEntry.setReceiverId(receiverId);
            creditEntry.setAmount(amount);
            creditEntry.setBalanceAfter(newReceiverBalance);
            creditEntry.setType("RECEIVER");
            creditEntry.setCurrency(currency);
            creditEntry.setStatus("SUCCESS");
            creditEntry.setTransferredDate(LocalDateTime.now());
            ledgerRepo.save(creditEntry);

            // Publish success events safely
            eventPublisher.publishSuccess(debitEntry);
            eventPublisher.publishSuccess(creditEntry);

            return List.of(debitEntry, creditEntry);

        } catch (Exception e) {
            // Publish failed event safely
            eventPublisher.publishFailed(null);
            throw e;
        } finally {
            lockService.unlock(lockKey);
        }
    }
}
