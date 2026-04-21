package com.example.fintechcompliance.ruleengine;

import com.example.fintechcompliance.model.FinancialTransaction;
import com.example.fintechcompliance.repository.TransactionRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component  
public class StructuringRule implements Rule {

    private final TransactionRepository transactionRepository;

    
    public StructuringRule(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public boolean apply(FinancialTransaction t) {

        LocalDateTime oneHourAgo = t.getTimestamp().minusHours(1);

        List<FinancialTransaction> recentTxns =
                transactionRepository.findRecentTransactions(t.getSenderId(), oneHourAgo);

        
        double total = recentTxns.stream()
                .filter(txn -> !txn.getId().equals(t.getId()))
                .mapToDouble(FinancialTransaction::getAmount)
                .sum();

        return total > 1000000;
    }

    @Override
    public String getName() {
        return "Structuring Rule";
    }
}