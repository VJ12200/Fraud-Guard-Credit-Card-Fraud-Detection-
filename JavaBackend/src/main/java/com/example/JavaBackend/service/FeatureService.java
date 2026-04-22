package com.example.fintechcompliance.service;

import com.example.fintechcompliance.model.FinancialTransaction;
import com.example.fintechcompliance.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FeatureService {

    @Autowired
    private TransactionRepository transactionRepository;

    
    public Map<String, Object> computeFeatures(FinancialTransaction txn) {





        LocalDateTime txnTime = txn.getTimestamp() != null
                ? txn.getTimestamp()
                : LocalDateTime.now();

        LocalDateTime windowStart = txnTime.minusHours(1);

        List<FinancialTransaction> recentTxns =
                transactionRepository.findRecentTransactions(
                        txn.getSenderId(),
                        windowStart
                );

        
        double avgAmount = computeAverage(recentTxns);
        int    txnCount  = recentTxns.size();
        double timeGap   = computeTimeGap(recentTxns, txnTime);

        Map<String, Object> features = new HashMap<>();

        
        features.put("avg_amount", avgAmount);
        features.put("txn_count",  txnCount);
        features.put("time_gap",   timeGap);

        
        features.put("hour",      txnTime.getHour());
        features.put("age",       txn.getAge()      != null ? txn.getAge()      : 0);
        features.put("category",  txn.getCategory() != null ? txn.getCategory() : "misc_net");
        features.put("gender",    txn.getGender()   != null ? txn.getGender()   : "M");
        features.put("city_pop",  txn.getCityPop()  != null ? txn.getCityPop()  : 0);
        features.put("lat",       txn.getLat()      != null ? txn.getLat()      : 0.0);
        features.put("lon",       txn.getLon()      != null ? txn.getLon()      : 0.0);
        features.put("merch_lat", txn.getMerchLat() != null ? txn.getMerchLat() : 0.0);
        features.put("merch_long",txn.getMerchLong()!= null ? txn.getMerchLong(): 0.0);

        
        double timeSinceLastTxn = recentTxns.isEmpty() ? 999_999.0
                : Duration.between(recentTxns.get(0).getTimestamp(), txnTime).toSeconds();
        features.put("time_since_last_txn", Math.max(timeSinceLastTxn, 0));


        features.put("txn_count_per_card_day", txnCount);


        double avg = (double) features.get("avg_amount");
        features.put("amt_vs_card_mean", txn.getAmount() - avg);


        double std = recentTxns.stream()
                .mapToDouble(t -> Math.pow(t.getAmount() - avg, 2))
                .average().orElse(1.0);
        std = Math.sqrt(std) + 1e-6;
        features.put("amt_vs_card_std", txn.getAmount() / std);


        double lat      = txn.getLat()      != null ? txn.getLat()      : 0.0;
        double lon      = txn.getLon()      != null ? txn.getLon()      : 0.0;
        double merchLat = txn.getMerchLat() != null ? txn.getMerchLat() : 0.0;
        double merchLon = txn.getMerchLong()!= null ? txn.getMerchLong(): 0.0;
        features.put("geo_distance", Math.sqrt(Math.pow(lat - merchLat, 2) + Math.pow(lon - merchLon, 2)));

        return features;
    }

    
    private double computeAverage(List<FinancialTransaction> txns) {
        if (txns.isEmpty()) return 1.0;
        double avg = txns.stream()
                .mapToDouble(FinancialTransaction::getAmount)
                .average()
                .orElse(1.0);
        return avg > 0 ? avg : 1.0;
    }

    private double computeTimeGap(List<FinancialTransaction> txns,
                                  LocalDateTime currentTime) {
        if (txns.isEmpty()) return 60.0;
        long gap = Duration.between(
                txns.get(0).getTimestamp(),
                currentTime
        ).toSeconds();
        return Math.max(gap, 1.0);
    }
}