package com.mekheainteractive.Transaction_Microservice.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class TransactionDTO {

    // Getters
    private final String referenceId;
    private final int amount;
    private final String currency;
    private final String status;
    private final String facebookId;
    private final String senderName;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime transactionDate;

    // Full constructor
    public TransactionDTO(String referenceId,
                          String facebookId,
                          String senderName,
                          int amount,
                          String currency,
                          LocalDateTime transactionDate,
                          String status) {
        this.referenceId = referenceId;
        this.facebookId = facebookId;
        this.senderName = senderName;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.transactionDate = transactionDate;
    }
}