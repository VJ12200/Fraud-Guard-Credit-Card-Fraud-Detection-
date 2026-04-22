package com.example.fintechcompliance.service;

import java.util.*;

public class CycleDetection {

    public boolean hasCycleFromNode(String start,
                                    Map<String, List<String>> graph) {

        return dfs(start, start, graph, new HashSet<>());
    }

    private boolean dfs(String current,
                        String target,
                        Map<String, List<String>> graph,
                        Set<String> path) {

        path.add(current);

        for (String neighbor : graph.getOrDefault(current, new ArrayList<>())) {

            
            if (neighbor.equals(target) && path.size() > 2) {
                return true;
            }

            
            if (!path.contains(neighbor)) {
                if (dfs(neighbor, target, graph, path)) {
                    return true;
                }
            }
        }

        
        path.remove(current);

        return false;
    }
}