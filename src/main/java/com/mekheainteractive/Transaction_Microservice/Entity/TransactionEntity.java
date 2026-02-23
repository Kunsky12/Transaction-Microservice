package com.mekheainteractive.Transaction_Microservice.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(
        name = "transaction_entity",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"senderId", "idempotencyKey"})
        }
)
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // row-level PK

    @Column(unique = true, nullable = false)
    private UUID transactionId; // manually assigned

    @Column(nullable = false)
    private String referenceId;

    private String idempotencyKey; // safe for retries

    @Column(nullable = false)
    private String senderId;
    @Column(nullable = false)
    private String receiverId;
    @Column(nullable = false)
    private String senderName;
    private String facebookId;
    @Column(nullable = false)
    private int amount; // negative = debit, positive = credit

    @Enumerated(EnumType.STRING)
    private TransactionType type; // optional for readabilityR

    private int balanceAfter;
    private String currency;
    private String status;

    private LocalDateTime transactionDate;

    @PrePersist
    protected void onCreate() {transactionDate = LocalDateTime.now();
    }

    public enum TransactionType {
        SENDER, RECEIVER
    }
}
