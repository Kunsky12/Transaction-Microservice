package com.mekheainteractive.Transaction_Microservice.Controller;

import com.mekheainteractive.Transaction_Microservice.Entity.TransactionEntity;
import com.mekheainteractive.Transaction_Microservice.Service.TransactionService;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/transfer")
    public TransactionEntity transfer(@RequestBody TransferRequest request) {
        if (request.getSenderId() == null || request.getReceiverId() == null) {
            throw new RuntimeException("SenderId or ReceiverId cannot be null");
        }
        return transactionService.transfer(
                request.getSenderId(),
                request.getReceiverId(),
                request.getAmount(),
                request.getCurrencyCode()
        );
    }

    @Data
    @NoArgsConstructor
    public static class TransferRequest {
        private String senderId;
        private String receiverId;
        private int amount;
        private String currencyCode;
    }
}

