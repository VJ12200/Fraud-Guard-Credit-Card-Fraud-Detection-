package com.example.fintechcompliance.controller;

import com.example.fintechcompliance.model.FinancialTransaction;
import com.example.fintechcompliance.service.TransactionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/ui")
public class ViewController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping
    public String showForm() {
        return "UI";
    }

    
    @PostMapping("/submit")
    public String submitSingle(
            @RequestParam String senderId,
            @RequestParam String receiverId,
            @RequestParam double amount,
            @RequestParam(defaultValue = "misc_net") String category,
            @RequestParam(defaultValue = "M")        String gender,
            @RequestParam(defaultValue = "0")        int    age,
            @RequestParam(defaultValue = "0")        int    cityPop,
            @RequestParam(defaultValue = "0.0")      double lat,
            @RequestParam(defaultValue = "0.0")      double lon,
            @RequestParam(defaultValue = "0.0")      double merchLat,
            @RequestParam(defaultValue = "0.0")      double merchLong,
            Model model) {

        FinancialTransaction txn = new FinancialTransaction();
        txn.setSenderId(senderId);
        txn.setReceiverId(receiverId);
        txn.setAmount(amount);
        txn.setCategory(category);
        txn.setGender(gender);
        txn.setAge(age);
        txn.setCityPop(cityPop);
        txn.setLat(lat);
        txn.setLon(lon);
        txn.setMerchLat(merchLat);
        txn.setMerchLong(merchLong);

        List<String> results = transactionService.processBatchTransactions(List.of(txn));
        model.addAttribute("results", results);
        return "UI";
    }

    
    @PostMapping("/submitBatch")
    public String submitBatch(
            @RequestParam String batchJson,
            Model model) {

        try {
            List<FinancialTransaction> transactions = objectMapper.readValue(
                    batchJson,
                    new TypeReference<List<FinancialTransaction>>() {}
            );
            List<String> results = transactionService.processBatchTransactions(transactions);
            model.addAttribute("results", results);

        } catch (Exception e) {
            model.addAttribute("error", "Invalid JSON: " + e.getMessage());
        }

        return "UI";
    }
}