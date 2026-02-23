package com.mekheainteractive.Transaction_Microservice.Event;

import com.mekheainteractive.Transaction_Microservice.Entity.TransactionEntity;

public interface EventPublisher {

    void publishTransactionNotification(TransactionEntity tx); // new method for WS
}