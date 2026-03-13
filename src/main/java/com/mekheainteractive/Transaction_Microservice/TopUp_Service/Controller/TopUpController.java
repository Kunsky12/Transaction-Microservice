package com.mekheainteractive.Transaction_Microservice.TopUp_Service.Controller;

import com.mekheainteractive.Transaction_Microservice.TopUp_Service.Entity.TopUpOrder;
import com.mekheainteractive.Transaction_Microservice.TopUp_Service.Service.TopUpService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/topup")
public class TopUpController {

    @Autowired
    private TopUpService service;

    // Create new order
    @PostMapping("/create")
    public TopUpOrder createOrder(@RequestParam String playerId,
                                  @RequestParam String pack,
                                  @RequestParam double amount) {
        return service.createOrder(playerId, pack, amount);
    }

    // Verify payment (called by Telegram bot)
    @PostMapping("/verify")
    public String verifyPayment(@RequestParam String code, @RequestParam double amount){
        boolean success = service.verifyPayment(code, amount);
        return success ? "PAID" : "FAILED";
    }
}