package com.mekheainteractive.Transaction_Microservice.TopUp_Service.Service;

import com.mekheainteractive.Transaction_Microservice.TopUp_Service.Entity.TopUpOrder;
import com.mekheainteractive.Transaction_Microservice.TopUp_Service.Repository.TopUpOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.Instant;
import java.util.Random;

@Service
public class TopUpService {

    @Autowired
    private TopUpOrderRepository repository;

    // Create a new top-up order
    public TopUpOrder createOrder(String playerId, String pack, double amount) {

        String code = generateCode();

        TopUpOrder order = new TopUpOrder();
        order.setPlayerId(playerId);
        order.setPack(pack);
        order.setAmount(amount);
        order.setCode(code);
        order.setStatus("PENDING");
        order.setCreatedAt(Instant.now());

        repository.save(order);
        return order;
    }

    // Verify payment (called from Telegram listener)
    public boolean verifyPayment(String code, double amount) {
        return repository.findByCode(code).map(order -> {

            if (!order.getStatus().equals("PENDING")) return false;
            if (order.getAmount() != amount) return false;

            order.setStatus("PAID");
            order.setPaidAt(Instant.now());
            repository.save(order);

            // call your coin delivery logic here
            System.out.println("Coins delivered to player: " + order.getPlayerId());
            return true;

        }).orElse(false);
    }

    private String generateCode() {
        return "KK" + (1000 + new Random().nextInt(9000));
    }
}