package com.example.fintechcompliance.ruleengine;

import com.example.fintechcompliance.model.FinancialTransaction;
import org.springframework.stereotype.Component;

@Component
public class HighAmountRule implements Rule {

    @Override
    public boolean apply(FinancialTransaction t) {
        return t.getAmount() > 1000000;
    }

    @Override
    public String getName() {
        return "High Amount Rule";
    }
}