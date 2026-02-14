package com.mekheainteractive.Transaction_Microservice.Controller;

import com.mekheainteractive.Transaction_Microservice.Entity.WalletTransaction;
import com.mekheainteractive.Transaction_Microservice.Service.WalletService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("/transfer")
    public Mono<WalletTransaction> transfer(@RequestBody TransferRequest request) {
        return walletService.transfer(
                request.getSenderId(),
                request.getReceiverId(),
                request.getAmount(),
                request.getCurrencyCode()
        );
    }
}

