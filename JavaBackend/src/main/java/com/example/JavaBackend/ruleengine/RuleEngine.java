package com.example.fintechcompliance.ruleengine;

import com.example.fintechcompliance.model.FinancialTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component  
public class RuleEngine {

    private final List<Rule> rules;

    @Autowired
    public RuleEngine(List<Rule> rules) {
        this.rules = rules;
    }

    public List<String> evaluate(FinancialTransaction t) {

        List<String> violations = new ArrayList<>();

        for (Rule rule : rules) {
            if (rule.apply(t)) {
                violations.add(rule.getName());
            }
        }

        return violations;
    }
}