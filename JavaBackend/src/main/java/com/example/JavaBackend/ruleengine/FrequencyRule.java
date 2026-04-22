package com.example.fintechcompliance.ruleengine;

import com.example.fintechcompliance.model.FinancialTransaction;
import com.example.fintechcompliance.repository.TransactionRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class FrequencyRule implements Rule {

    private final TransactionRepository transactionRepository;

    public FrequencyRule(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public boolean apply(FinancialTransaction t) {

        LocalDateTime timestamp = t.getTimestamp() != null
                ? t.getTimestamp()
                : LocalDateTime.now();

        LocalDateTime oneHourAgo = timestamp.minusHours(1);

        List<FinancialTransaction> recent =
                transactionRepository.findRecentTransactions(t.getSenderId(), oneHourAgo);

        return recent.size() >= 10;
    }

    @Override
    public String getName() {
        return "High Frequency Rule";
    }
}