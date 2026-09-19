package com.retail.rec.algorithm;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Cosine Similarity between two users' behavior vectors — IMPLEMENTATION_PLAN.md section 5.4 step 2. */
@Component
public class UserSimilarity {

    public double cosineSimilarity(Map<UUID, Double> a, Map<UUID, Double> b) {
        Set<UUID> common = new HashSet<>(a.keySet());
        common.retainAll(b.keySet());

        double dot = common.stream().mapToDouble(p -> a.get(p) * b.get(p)).sum();
        double normA = Math.sqrt(a.values().stream().mapToDouble(v -> v * v).sum());
        double normB = Math.sqrt(b.values().stream().mapToDouble(v -> v * v).sum());

        if (normA == 0 || normB == 0) {
            return 0.0;
        }
        return dot / (normA * normB);
    }
}
