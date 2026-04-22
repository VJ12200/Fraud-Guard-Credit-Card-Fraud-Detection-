package com.example.fintechcompliance.ruleengine;

import com.example.fintechcompliance.model.FinancialTransaction;

public interface Rule {
    boolean apply(FinancialTransaction t);
    String getName();
}