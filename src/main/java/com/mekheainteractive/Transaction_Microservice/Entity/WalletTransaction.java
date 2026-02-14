package com.mekheainteractive.Transaction_Microservice.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

@Entity
public class WalletTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String senderId;
    private String receiverId;
    private int amount;
    private String status;
    private LocalDateTime createdAt = LocalDateTime.now();

    public void setSenderId(String senderId) {
    }

    public void setReceiverId(String receiverId) {
    }

    public void setAmount(int amount) {
    }

    public void setStatus(String pending) {
    }
}
