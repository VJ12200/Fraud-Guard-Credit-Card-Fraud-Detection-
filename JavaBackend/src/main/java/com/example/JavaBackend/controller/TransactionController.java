package com.example.fintechcompliance.controller;

import com.example.fintechcompliance.model.FinancialTransaction;
import com.example.fintechcompliance.service.TransactionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private ObjectMapper objectMapper;

    
    @PostMapping
    public List<String> process(@RequestBody String rawBody) {
        List<FinancialTransaction> transactions = new ArrayList<>();

        try {
            JsonNode body = objectMapper.readTree(rawBody); 

            if (body.isArray()) {
                for (JsonNode node : body) {
                    FinancialTransaction txn = objectMapper.treeToValue(node, FinancialTransaction.class);
                    validateTransaction(txn);
                    transactions.add(txn);
                }
            } else {
                FinancialTransaction txn = objectMapper.treeToValue(body, FinancialTransaction.class);
                validateTransaction(txn);
                transactions.add(txn);
            }

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid request body: " + e.getMessage());
        }

        return transactionService.processBatchTransactions(transactions);
    }

    
    private void validateTransaction(FinancialTransaction t) {

        if (t.getSenderId() == null || t.getSenderId().isBlank()) {
            throw new IllegalArgumentException("senderId is required");
        }

        if (t.getReceiverId() == null || t.getReceiverId().isBlank()) {
            throw new IllegalArgumentException("receiverId is required");
        }

        if (t.getAmount() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }

    
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public List<String> handleValidationException(IllegalArgumentException ex) {
        return List.of("ERROR: " + ex.getMessage());
    }
}