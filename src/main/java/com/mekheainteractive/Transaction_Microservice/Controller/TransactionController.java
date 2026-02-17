package com.mekheainteractive.Transaction_Microservice.Controller;

import com.mekheainteractive.Transaction_Microservice.Entity.LedgerEntity;
import com.mekheainteractive.Transaction_Microservice.Service.JwtService;
import com.mekheainteractive.Transaction_Microservice.Service.LedgerService;
import com.mekheainteractive.Transaction_Microservice.Service.PlayFabService;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TransactionController {

    private final LedgerService ledgerService;
    private final PlayFabService playFabService;
    private final JwtService jwtService;

    public TransactionController(
            LedgerService ledgerService,
            PlayFabService playFabService,
            JwtService jwtService
    ) {
        this.ledgerService = ledgerService;
        this.playFabService = playFabService;
        this.jwtService = jwtService;
    }

    // 🔐 LOGIN
    @PostMapping("/auth/login")
    public String login(@RequestBody LoginRequest request) {
        String sessionTicket = playFabService.verifySessionTicket(request.getSessionTicket());
        return jwtService.generateToken(sessionTicket);
    }

    @PostMapping("/transfer")
    public List<LedgerEntity> transfer(
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

        String senderId = jwtService.extractPlayfabId(token);

        if (senderId == null || request.getReceiverId() == null || request.currency == null) {
            throw new IllegalArgumentException("Sender, receiver, and currency must be non-null");
        }

        if (request.amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        return ledgerService.transfer(
                request.getIdempotencyKey(),
                senderId,
                request.getReceiverId(),
                request.getAmount(),
                request.getCurrency()
        );
    }

    @Data
    @NoArgsConstructor
    public static class LoginRequest {
        private String sessionTicket;
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
    }
}

