package com.mekheainteractive.Transaction_Microservice.Event;

import com.mekheainteractive.Transaction_Microservice.Entity.TransactionEntity;
import com.mekheainteractive.Transaction_Microservice.Auth.JwtAuth;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class EventPublisherImpl implements EventPublisher {

    private final RestTemplate restTemplate;
    @Value("${websocket.url}")
    private String WS_SERVER_URL;
    private final JwtAuth jwtService;

    @PostConstruct
    public void init() {System.out.println("WS_SERVER_URL = " + WS_SERVER_URL);
    }
    public EventPublisherImpl(JwtAuth jwtService) {
        this.jwtService = jwtService;

        // Set timeouts to avoid long blocking
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000); // 3s
        factory.setReadTimeout(5000);    // 5s
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public void publishTransactionNotification(TransactionEntity tx) {
        // Run async so that notification failure doesn't break transfer
        new Thread(() -> {
            try {
                Map<String, Object> payload = Map.of(
                        "receiverId", tx.getReceiverId(),
                        "facebookId", tx.getFacebookId(),
                        "senderName", tx.getSenderName(),
                        "referenceId", tx.getReferenceId(),
                        "transactionDate", tx.getTransactionDate(),
                        "currency", tx.getCurrency(),
                        "amount", tx.getAmount(),
                        "message", "You received " + tx.getAmount() + " " + tx.getCurrency() + " from " + tx.getSenderName()
                );

                String token = jwtService.generateServiceToken();

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(token);

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

                restTemplate.postForEntity(WS_SERVER_URL, request, Void.class);

                System.out.println("✅ Notification sent to receiver: " + tx.getReceiverId());

            } catch (Exception e) {
                // Don't throw: just log
                System.err.println("❌ Failed to send notification for tx: " + tx.getReferenceId());
                e.printStackTrace();
            }
        }).start();
    }
}

