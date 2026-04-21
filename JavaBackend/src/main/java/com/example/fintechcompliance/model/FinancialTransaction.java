package com.example.fintechcompliance.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "transactions")
@JsonIgnoreProperties(ignoreUnknown = true)
public class FinancialTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "senderId is required")
    private String senderId;       

    @NotBlank(message = "receiverId is required")
    private String receiverId;     

    @Positive(message = "amount must be positive")
    private double amount;         

    private String category;       
    private String gender;         
    private Integer age;               
    private Integer cityPop;           
    private Double lat;
    private Double lon;
    private Double merchLat;
    private Double merchLong;

    private LocalDateTime timestamp;

    @PrePersist
    public void prePersist() {
        if (this.timestamp == null) this.timestamp = LocalDateTime.now();
    }
}