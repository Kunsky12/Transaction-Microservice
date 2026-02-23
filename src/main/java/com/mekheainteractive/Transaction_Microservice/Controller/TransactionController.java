package com.mekheainteractive.Transaction_Microservice.Controller;

import com.mekheainteractive.Transaction_Microservice.DTO.TransactionDTO;
import com.mekheainteractive.Transaction_Microservice.Auth.JwtAuth;
import com.mekheainteractive.Transaction_Microservice.Service.LedgerService;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class TransactionController {

    private final LedgerService ledgerService;
    private final JwtAuth jwtService;

    public TransactionController(
            LedgerService ledgerService,
            JwtAuth jwtService
    ) {
        this.ledgerService = ledgerService;
        this.jwtService = jwtService;
    }

    @PostMapping("/transfer")
    public TransactionDTO transfer(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody TransferRequest request
    ) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);

        if (!jwtService.validateToken(token)) {
            throw new RuntimeException("Invalid JWT token");
        }

        String senderId = jwtService.extractPlayFabId(token);

        if (senderId == null || request.getReceiverId() == null || request.currency == null) {
            throw new IllegalArgumentException("Sender, receiver, and currency must be non-null");
        }

        if (request.amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        return ledgerService.transfer(
                request.getIdempotencyKey(),
                senderId,
                request.getSenderName(),
                request.getFacebookId(),
                request.getReceiverId(),
                request.getAmount(),
                request.getCurrency()
        );
    }
    // DTO
    @Data
    @NoArgsConstructor
    public static class TransferRequest
    {
        private String idempotencyKey;
        private String receiverId;
        private int amount;
        private String currency;
        private String facebookId;
        private String senderName;
    }
}

