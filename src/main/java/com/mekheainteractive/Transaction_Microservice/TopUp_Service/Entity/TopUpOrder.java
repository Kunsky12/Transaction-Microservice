package com.mekheainteractive.Transaction_Microservice.TopUp_Service.Entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "topup_orders")
public class TopUpOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String playerId;
    private String pack;
    private double amount;
    @Column(unique = true)
    private String code; // KKXXXX

    private String status; // PENDING, PAID, FAILED
    private Instant createdAt;
    private Instant paidAt;

    // Getters and setters
    public Long getId() { return id; }
    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
    public String getPack() { return pack; }
    public void setPack(String pack) { this.pack = pack; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
}