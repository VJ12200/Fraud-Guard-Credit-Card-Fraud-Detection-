package com.example.fintechcompliance.model;


import jakarta.persistence.*;
import lombok.Data;



@Data
@Entity
@Table(name = "alerts")
public class FinancialAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long transactionId;
    private String ruleViolated;
    private double riskScore;
    private String explanation;
}