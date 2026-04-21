package com.example.fintechcompliance.service;

import com.example.fintechcompliance.model.FinancialAlert;
import com.example.fintechcompliance.model.FinancialTransaction;
import com.example.fintechcompliance.repository.AlertRepository;
import com.example.fintechcompliance.repository.TransactionRepository;
import com.example.fintechcompliance.ruleengine.RuleEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private RiskScoringService riskScoringService;

    @Autowired
    private ExplanationService explanationService;

    @Autowired
    private AIService aiServiceClient;

    @Autowired
    private GraphService graphService;

    @Autowired
    private FeatureService featureService;

    @Autowired
    private RuleEngine ruleEngine;

    
    public List<String> processBatchTransactions(List<FinancialTransaction> transactions) {

        
        List<FinancialTransaction> savedTxns = new ArrayList<>();
        for (FinancialTransaction txn : transactions) {
            if (txn.getTimestamp() == null) {
                txn.setTimestamp(LocalDateTime.now());
            }
            savedTxns.add(transactionRepository.save(txn));
        }

        

        graphService.invaIlidateCache();
        Map<String, List<String>> graph = graphService.buildGraph();



        
        List<String> responses = new ArrayList<>();

        for (FinancialTransaction txn : savedTxns) {
            try {
                List<String> violations = new ArrayList<>(ruleEngine.evaluate(txn));

                
                Map<String, Object> features = featureService.computeFeatures(txn);

                double timeGap = Math.max(
                        ((Number) features.getOrDefault("time_gap", 60)).doubleValue(), 1);
                double avgAmount = Math.max(
                        ((Number) features.getOrDefault("avg_amount", txn.getAmount())).doubleValue(), 1);
                int txnCount = ((Number) features.getOrDefault("txn_count", 1)).intValue();

                features.put("time_gap", timeGap);
                features.put("avg_amount", avgAmount);
                features.put("txn_count", txnCount);

                boolean isAnomaly = aiServiceClient.isAnomalous(txn, features);
                if (isAnomaly && !violations.contains("AI Anomaly Detection")) {
                    violations.add("AI Anomaly Detection");
                }

                
                

                CycleDetection cycleDetection = new CycleDetection();
                if (cycleDetection.hasCycleFromNode(txn.getSenderId(), graph) ||
                        cycleDetection.hasCycleFromNode(txn.getReceiverId(), graph)) {
                    violations.add("Graph Cycle Fraud");
                }

                double riskScore = riskScoringService.calculateRisk(violations, txn.getAmount());
                String explanation = explanationService.generate(violations);

                
                for (String violation : violations) {
                    FinancialAlert alert = new FinancialAlert();
                    alert.setTransactionId(txn.getId());
                    alert.setRuleViolated(violation);
                    alert.setRiskScore(riskScore);
                    alert.setExplanation(explanation);  
                    alertRepository.save(alert);
                }

                responses.add(violations.isEmpty()
                        ? "Transaction OK"
                        : "FLAGGED | Risk: " + riskScore + " | " + explanation);

            } catch (Exception e) {
                
                responses.add("FAILED: " + e.getMessage());
            }
        }

        return responses;
    }
}