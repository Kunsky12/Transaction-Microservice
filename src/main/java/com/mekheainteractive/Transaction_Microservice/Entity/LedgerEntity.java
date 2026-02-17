package com.mekheainteractive.Transaction_Microservice.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
public class LedgerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // auto-generate primary key
    private Long Id;
    @Column(unique = true, nullable = false)
    private String transactionId;
    @Column(unique = true)
    private String idempotencyKey;
    private String senderId;
    private String receiverId;
    private int amount;
    private String type; // SENDER / RECEIVER
    private int balanceAfter;
    private String currency;
    private String status;

    private LocalDateTime transferredDate;
}
