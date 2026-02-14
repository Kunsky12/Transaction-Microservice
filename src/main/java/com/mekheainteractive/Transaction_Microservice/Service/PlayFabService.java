package com.mekheainteractive.Transaction_Microservice.Service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class PlayFabService {

    @Value("${playfab.title-id}")
    private String titleId;

    @Value("${playfab.secret-key}")
    private String secretKey;

    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder()
                .baseUrl("https://" + titleId + ".playfabapi.com/Server")
                .build();
    }

    private WebClient webClient;

    public Mono<Integer> getCurrencyBalance(String playfabId, String currencyCode) {
        return webClient.post()
                .uri("/GetUserInventoryRequest")
                .header("X-SecretKey", secretKey)
                .bodyValue(Map.of("PlayFabId", playfabId))
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    Map<String, Object> data = (Map<String, Object>) response.get("data");
                    Map<String, Integer> vc = (Map<String, Integer>) data.get("VirtualCurrency");
                    return vc.getOrDefault(currencyCode, 0);
                });
    }

    public Mono<Integer> subtractCurrency(String playfabId, String currencyCode, int amount) {
        return webClient.post()
                .uri("/SubtractUserVirtualCurrency")
                .header("X-SecretKey", secretKey)
                .bodyValue(Map.of(
                        "PlayFabId", playfabId,
                        "VirtualCurrency", currencyCode,
                        "Amount", amount
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .map(resp -> (Integer)((Map<String, Object>)resp.get("data")).get("Balance"));
    }

    public Mono<Integer> addCurrency(String playfabId, String currencyCode, int amount) {
        return webClient.post()
                .uri("/AddUserVirtualCurrency")
                .header("X-SecretKey", secretKey)
                .bodyValue(Map.of(
                        "PlayFabId", playfabId,
                        "VirtualCurrency", currencyCode,
                        "Amount", amount
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .map(resp -> (Integer)((Map<String, Object>)resp.get("data")).get("Balance"));
    }
}
