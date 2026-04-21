package com.example.fintechcompliance.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RiskScoringService {

    public double calculateRisk(List<String> violations, double amount) {

        double score = 0;

        for (String v : violations) {

            if (v.equals("High Amount Rule")) score += 50;
            if (v.equals("Structuring Rule")) score += 40;
            if (v.equals("Graph Cycle Fraud")) score += 70;
            if (v.equals("High Frequency Rule")) score += 30;
            if (v.equals("AI Anomaly Detection")) score += 60;
        }

        
        if (amount > 2000000) score += 10;

        return Math.min(score, 100); 
    }
}
