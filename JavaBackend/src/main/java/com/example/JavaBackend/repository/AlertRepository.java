package com.example.fintechcompliance.repository;

import com.example.fintechcompliance.model.FinancialAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<FinancialAlert, Long> {


    List<FinancialAlert> findByTransactionId(Long transactionId);
    List<FinancialAlert> findByRuleViolated(String ruleViolated);

}
