package com.example.fintechcompliance.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExplanationService {

    public String generate(List<String> violations) {
        if (violations.isEmpty()) return "No violations";
        return String.format("%d violation(s) detected: %s",
                violations.size(),
                String.join(", ", violations));
    }
}