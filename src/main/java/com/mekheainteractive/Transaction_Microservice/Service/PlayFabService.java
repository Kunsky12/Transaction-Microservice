package com.mekheainteractive.Transaction_Microservice.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Service
public class PlayFabService {

    @Value("${playfab.title-id}")
    private String titleId;

    @Value("${playfab.secret-key}")
    private String secretKey;

    private RestClient getClient() {
        return RestClient.builder()
                .baseUrl("https://" + titleId + ".playfabapi.com/Server")
                .defaultHeader("X-SecretKey", secretKey)
                .build();
    }

    // ✅ Get Currency Balance
    public int getSenderCurrency(String playfabId) {
        try {
            Map response = getClient()
                    .post()
                    .uri("/GetUserInventory")
                    .body(Map.of("PlayFabId", playfabId))
                    .retrieve()
                    .body(Map.class);

            Map<String, Object> data =
                    (Map<String, Object>) response.get("data");

            Map<String, Integer> currency =
                    (Map<String, Integer>) data.get("VirtualCurrency");

            return currency.getOrDefault("RP", 0);

        } catch (RestClientResponseException ex) {
            System.err.println("Get inventory error: " + ex.getResponseBodyAsString());
            return 0;
        }
    }

    // Subtract Currency
    public int subtractCurrency(String playfabId, String currencyCode, int amount) {
        try {
            Map response = getClient()
                    .post()
                    .uri("/SubtractUserVirtualCurrency")
                    .body(Map.of(
                            "PlayFabId", playfabId,
                            "VirtualCurrency", currencyCode,
                            "Amount", amount
                    ))
                    .retrieve()
                    .body(Map.class);

            Map<String, Object> data = (Map<String, Object>) response.get("data");

            return (Integer) data.get("Balance");

        } catch (RestClientResponseException ex) {
            System.err.println("Subtract error: " + ex.getResponseBodyAsString());
            return -1;
        }
    }

    // Add Currency
    public int addCurrency(String playfabId, String currencyCode, int amount)
    {
        try {
            Map response = getClient()
                    .post()
                    .uri("/AddUserVirtualCurrency")
                    .body(Map.of(
                            "PlayFabId", playfabId,
                            "VirtualCurrency", currencyCode,
                            "Amount", amount
                    ))
                    .retrieve()
                    .body(Map.class);

            Map<String, Object> data = (Map<String, Object>) response.get("data");

            return (Integer) data.get("Balance");

        } catch (RestClientResponseException ex) {
            System.err.println("Add error: " + ex.getResponseBodyAsString());
            return -1;
        }
    }
}
