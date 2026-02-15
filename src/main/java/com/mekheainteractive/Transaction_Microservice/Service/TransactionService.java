package com.mekheainteractive.Transaction_Microservice.Service;

import com.mekheainteractive.Transaction_Microservice.Entity.TransactionEntity;
import com.mekheainteractive.Transaction_Microservice.Repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    private final TransactionRepository trxRepo;
    private final PlayFabService playFabService;

    public TransactionService(TransactionRepository trxRepo,
                              PlayFabService playFabService) {
        this.trxRepo = trxRepo;
        this.playFabService = playFabService;
    }

    @Transactional
    public TransactionEntity transfer(String senderId, String receiverId, int amount, String currencyCode) {

        if (senderId.equals(receiverId)) {
            throw new RuntimeException("Cannot transfer to yourself");
        }

        TransactionEntity trx = new TransactionEntity();
        trx.setSenderId(senderId);
        trx.setReceiverId(receiverId);
        trx.setAmount(amount);
        trx.setCurrencyCode(currencyCode);
        trx.setStatus("PENDING");


        try {
            int senderBalance = playFabService.getSenderCurrency(senderId);

            if (senderBalance < amount) {
                trx.setStatus("FAILED");
                trxRepo.save(trx);
                throw new RuntimeException("Insufficient balance");
            }

            int newSenderBalance = playFabService.subtractCurrency(senderId, "RP", amount);

            if (newSenderBalance < 0) {
                trx.setStatus("FAILED");
                trxRepo.save(trx);
                throw new RuntimeException("Failed to subtract currency");
            }

            int newReceiverBalance = playFabService.addCurrency(receiverId, "RP", amount);

            if (newReceiverBalance < 0) {
                trx.setStatus("FAILED");
                trxRepo.save(trx);
                throw new RuntimeException("Failed to add currency");
            }

            trx.setStatus("SUCCESS");
            return trxRepo.save(trx);

        } catch (Exception ex) {
            trx.setStatus("FAILED");
            trxRepo.save(trx);
            throw ex;
        }
    }
}
