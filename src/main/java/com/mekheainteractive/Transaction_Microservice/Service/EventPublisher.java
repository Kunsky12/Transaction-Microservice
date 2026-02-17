package com.mekheainteractive.Transaction_Microservice.Service;

import com.mekheainteractive.Transaction_Microservice.Entity.LedgerEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class EventPublisher {

    private final KafkaTemplate<String, Object> kafka;

    public EventPublisher(KafkaTemplate<String, Object> kafka) {
        this.kafka = kafka;
    }

    public void publishSuccess(LedgerEntity trx) {
        kafka.send("transaction.success", trx);
    }

    public void publishFailed(LedgerEntity trx) {
        kafka.send("transaction.failed", trx);
    }
}