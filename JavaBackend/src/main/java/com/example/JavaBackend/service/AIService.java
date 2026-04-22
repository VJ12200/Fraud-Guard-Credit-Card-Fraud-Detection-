package com.example.fintechcompliance.service;

import com.example.fintechcompliance.model.FinancialTransaction;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class AIService {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String AI_URL = "http:

    public boolean isAnomalous(FinancialTransaction txn,
                               Map<String, Object> features) {
        try {
            
            Map<String, Object> request = new HashMap<>();
            request.put("amt",                     txn.getAmount());
            request.put("city_pop",                features.getOrDefault("city_pop", 0));
            request.put("age",                     features.getOrDefault("age", 0));
            request.put("hour",                    txn.getTimestamp().getHour());
            request.put("lat",                     features.getOrDefault("lat", 0.0));
            request.put("long_",                   features.getOrDefault("lon", 0.0));   
            request.put("merch_lat",               features.getOrDefault("merch_lat", 0.0));
            request.put("merch_long",              features.getOrDefault("merch_long", 0.0));
            request.put("category",               features.getOrDefault("category", "misc_net"));
            request.put("gender",                  features.getOrDefault("gender", "M"));

            request.put("time_since_last_txn",     features.getOrDefault("time_since_last_txn", 999999.0));
            request.put("txn_count_per_card_day",  features.getOrDefault("txn_count_per_card_day", 1));
            request.put("amt_vs_card_mean",        features.getOrDefault("amt_vs_card_mean", 0.0));
            request.put("amt_vs_card_std",         features.getOrDefault("amt_vs_card_std", 1.0));
            request.put("geo_distance",            features.getOrDefault("geo_distance", 0.0));
            Map response = restTemplate.postForObject(
                    AI_URL, request, Map.class);
            System.out.println("[AIService] Sending to Python: " + request);
            System.out.println("[AIService] Response: " + response);

            
            if (response == null || response.get("fraud") == null) {
                System.out.println("[AIService] Warning: null response from Python API, skipping AI check");
                return false;
            }

            return Integer.parseInt(response.get("fraud").toString()) == 1;

        } catch (ResourceAccessException e) {
            
            System.out.println("[AIService] Python API unreachable, skipping AI check: " + e.getMessage());
            return false;

        } catch (Exception e) {
            
            System.out.println("[AIService] Unexpected error in AI check: " + e.getMessage());
            return false;
        }


    }
}