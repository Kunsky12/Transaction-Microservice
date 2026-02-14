package com.mekheainteractive.Transaction_Microservice.Service;

import com.mekheainteractive.Transaction_Microservice.Entity.WalletTransaction;
import com.mekheainteractive.Transaction_Microservice.Repository.PlayerWalletRepository;
import com.mekheainteractive.Transaction_Microservice.Repository.WalletTransactionRepository;
import com.mekheainteractive.Transaction_Microservice.Service.PlayFabService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
public class WalletService {

    private final PlayerWalletRepository walletRepo;
    private final WalletTransactionRepository trxRepo;
    private final PlayFabService playFabService;

    public WalletService(PlayerWalletRepository walletRepo, WalletTransactionRepository trxRepo, PlayFabService playFabService) {
        this.walletRepo = walletRepo;
        this.trxRepo = trxRepo;
        this.playFabService = playFabService;
    }

    @Transactional
    public Mono<WalletTransaction> transfer(String senderId, String receiverId, int amount, String currencyCode) {
        if (senderId.equals(receiverId)) return Mono.error(new RuntimeException("Cannot transfer to yourself"));

        WalletTransaction trx = new WalletTransaction();
        trx.setSenderId(senderId);
        trx.setReceiverId(receiverId);
        trx.setAmount(amount);
        trx.setStatus("PENDING");
        trxRepo.save(trx);

        return playFabService.getCurrencyBalance(senderId, currencyCode)
                .flatMap(balance -> {
                    if (balance < amount) return Mono.error(new RuntimeException("Insufficient balance"));
                    return playFabService.subtractCurrency(senderId, currencyCode, amount);
                })
                .flatMap(newBalance -> playFabService.addCurrency(receiverId, currencyCode, amount))
                .map(finalBalance -> {
                    trx.setStatus("SUCCESS");
                    return trxRepo.save(trx);
                })
                .onErrorResume(err -> {
                    trx.setStatus("FAILED");
                    trxRepo.save(trx);
                    return Mono.error(err);
                });
    }
}
