package com.example.fintechcompliance.repository;

import com.example.fintechcompliance.model.FinancialTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<FinancialTransaction, Long> {

    List<FinancialTransaction> findBySenderId(String senderId);
    List<FinancialTransaction> findBySenderIdAndTimestampAfter(String senderId, LocalDateTime time);
    List<FinancialTransaction> findByAmountGreaterThan(double amount);

    @Query("""
        SELECT t FROM FinancialTransaction t
        WHERE t.senderId = :senderId
        AND t.timestamp >= :startTime
        ORDER BY t.timestamp DESC
        """)
    List<FinancialTransaction> findRecentTransactions(
            @Param("senderId") String senderId,
            @Param("startTime") LocalDateTime startTime
    );

    
    @Query("""
        SELECT t FROM FinancialTransaction t
        WHERE t.timestamp >= :since
        """)
    List<FinancialTransaction> findAllSince(
            @Param("since") LocalDateTime since
    );
}