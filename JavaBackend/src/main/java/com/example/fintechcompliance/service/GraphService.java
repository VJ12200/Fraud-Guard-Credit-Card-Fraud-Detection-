package com.example.fintechcompliance.service;

import com.example.fintechcompliance.model.FinancialTransaction;
import com.example.fintechcompliance.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class GraphService {

    @Autowired
    private TransactionRepository transactionRepository;

    private Map<String, List<String>> cachedGraph = null;
    private LocalDateTime cacheTime = null;

    private static final int CACHE_TTL_MINUTES  = 5;
    private static final int GRAPH_WINDOW_HOURS = 24; 

    public Map<String, List<String>> buildGraph() {
        if (cachedGraph == null ||
                Duration.between(cacheTime, LocalDateTime.now()).toMinutes() > CACHE_TTL_MINUTES) {
            cachedGraph = buildFresh();
            cacheTime = LocalDateTime.now();
        }
        return cachedGraph;
    }

    private Map<String, List<String>> buildFresh() {
        
        LocalDateTime since = LocalDateTime.now().minusHours(GRAPH_WINDOW_HOURS);
        List<FinancialTransaction> transactions = transactionRepository.findAllSince(since);

        Map<String, List<String>> graph = new HashMap<>();
        for (FinancialTransaction t : transactions) {
            graph.computeIfAbsent(t.getSenderId(), k -> new ArrayList<>())
                    .add(t.getReceiverId());
        }
        return graph;
    }


    public void invaIlidateCache() {
        this.cachedGraph = null;
        this.cacheTime   = null;
    }
}