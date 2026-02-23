package com.mekheainteractive.Transaction_Microservice.Service;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Service
public class PlayFabWalletService {

    @Value("${playfab.title-id}")
    private String titleId;

    @Value("${playfab.secret-key}")
    private String secretKey;

    private RestClient restClient() {
        return RestClient.builder()
                .baseUrl("https://" + titleId + ".playfabapi.com/Server")
                .defaultHeader("X-SecretKey", secretKey)
                .build();
    }
    // -----------------------------
    // Currency Result wrapper
    // -----------------------------
    public static class CurrencyResult {
        private final boolean success;
        @Getter
        private final int balance;

        public CurrencyResult(boolean success, int balance) {
            this.success = success;
            this.balance = balance;
        }

        public boolean success() {
            return success;
        }
    }

    // Get User Balance
    public int getSenderCurrency(String playfabId) {
        try {
            Map response = restClient()
                    .post()
                    .uri("/GetUserInventory")
                    .body(Map.of("PlayFabId", playfabId))
                    .retrieve()
                    .body(Map.class);

            Map<String, Object> data = (Map<String, Object>) response.get("data");
            Map<String, Integer> currency = (Map<String, Integer>) data.get("VirtualCurrency");

            return currency.getOrDefault("RP", 0);

        } catch (RestClientResponseException ex) {
            System.err.println("Get inventory error: " + ex.getResponseBodyAsString());
            return 0;
        }
    }

    // Subtract Currency
    public CurrencyResult subtractCurrency(String playfabId, String currency, int amount) {
        try {
            Map response = restClient()
                    .post()
                    .uri("/SubtractUserVirtualCurrency")
                    .body(Map.of(
                            "PlayFabId", playfabId,
                            "VirtualCurrency", currency,
                            "Amount", amount
                    ))
                    .retrieve()
                    .body(Map.class);

            Map<String, Object> data = (Map<String, Object>) response.get("data");
            int balance = (Integer) data.get("Balance");

            return new CurrencyResult(true, balance);

        } catch (RestClientResponseException ex) {
            System.err.println("Subtract error: " + ex.getResponseBodyAsString());
            return new CurrencyResult(false, -1);
        }
    }

    // Add Currency
    public CurrencyResult addCurrency(String playfabId, String currency, int amount) {
        try {
            Map response = restClient()
                    .post()
                    .uri("/AddUserVirtualCurrency")
                    .body(Map.of(
                            "PlayFabId", playfabId,
                            "VirtualCurrency", currency,
                            "Amount", amount
                    ))
                    .retrieve()
                    .body(Map.class);

            Map<String, Object> data = (Map<String, Object>) response.get("data");
            int balance = (Integer) data.get("Balance");

            return new CurrencyResult(true, balance);

        } catch (RestClientResponseException ex) {
            System.err.println("Add error: " + ex.getResponseBodyAsString());
            return new CurrencyResult(false, -1);
        }
    }

    // Verify PlayFab session ticket
    public String verifySessionTicket(String sessionTicket) {
        try {
            Map<String, Object> body = Map.of("SessionTicket", sessionTicket);

            Map response = restClient()
                    .post()
                    .uri("/AuthenticateSessionTicket")
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                System.err.println("PlayFab response is null");
                return null;
            }

            Map<String, Object> data = (Map<String, Object>) response.get("data");
            Map<String, Object> userInfo = (Map<String, Object>) data.get("UserInfo");

            if (userInfo == null) {
                System.err.println("PlayFab UserInfo is null");
                return null;
            }

            String playFabId = (String) userInfo.get("PlayFabId");
            System.out.println("Extracted PlayFabId " + playFabId);
            return playFabId;

        } catch (RestClientResponseException ex) {
            System.err.println("PlayFab verify session error: " + ex.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
