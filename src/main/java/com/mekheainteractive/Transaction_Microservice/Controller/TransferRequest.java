package com.mekheainteractive.Transaction_Microservice.Controller;

import lombok.Getter;
import lombok.Setter;

@Setter
public class TransferRequest {
    private String senderId;
    private String receiverId;
    @Getter
    private int amount;
    private String currencyCode;

    public String getSenderId() {
        return senderId != null ? senderId.trim() : null;
    }

    public String getReceiverId() {
        return receiverId != null ? receiverId.trim() : null;
    }

    public String getCurrencyCode() {
        return currencyCode != null ? currencyCode.trim() : null;
    }

}
