package com.retail.rec.service;

import com.retail.rec.repository.BehaviorRepo;
import com.retail.rec.repository.UserProductScore;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Builds per-user behavior vectors — IMPLEMENTATION_PLAN.md section 5.4 step 1. */
@Service
public class BehaviorAnalysisService {

    private final BehaviorRepo behaviorRepo;

    public BehaviorAnalysisService(BehaviorRepo behaviorRepo) {
        this.behaviorRepo = behaviorRepo;
    }

    /** user_id -> {product_id: weighted_score}, aggregated from every behavior_events row. */
    public Map<UUID, Map<UUID, Double>> buildUserVectors() {
        Map<UUID, Map<UUID, Double>> vectors = new HashMap<>();
        for (UserProductScore row : behaviorRepo.findAggregatedScores()) {
            vectors.computeIfAbsent(row.getUserId(), key -> new HashMap<>())
                    .put(row.getProductId(), row.getScore());
        }
        return vectors;
    }
}
