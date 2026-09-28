package com.retail.rec.algorithm;

import com.retail.rec.service.SimilarityService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** User-based Collaborative Filtering — IMPLEMENTATION_PLAN.md section 5.4 step 3. */
@Component
public class CollaborativeFilter {

    private static final int TOP_K_SIMILAR_USERS = 20;

    private final SimilarityService similarityService;

    public CollaborativeFilter(SimilarityService similarityService) {
        this.similarityService = similarityService;
    }

    public record ScoredProduct(UUID productId, double score, String reason) {
    }

    /**
     * @param userId         user to recommend for
     * @param allUserVectors user_id -> {product_id: weighted_score} for every user with behavior history
     * @param limit          max recommendations to return
     * @return top-N products by weighted similarity score, or an empty list on cold start
     *         (userId has no behavior_events yet — the caller falls back to top-selling)
     */
    public List<ScoredProduct> recommend(UUID userId, Map<UUID, Map<UUID, Double>> allUserVectors, int limit) {
        Map<UUID, Double> targetVector = allUserVectors.get(userId);
        if (targetVector == null || targetVector.isEmpty()) {
            return List.of();
        }

        List<SimilarityService.SimilarUser> topSimilarUsers =
                similarityService.findTopSimilarUsers(userId, allUserVectors, TOP_K_SIMILAR_USERS);

        // Steps 3-4: accumulate weighted scores for products the target user hasn't interacted with.
        Map<UUID, Double> candidateScores = new HashMap<>();
        for (SimilarityService.SimilarUser similarUser : topSimilarUsers) {
            Map<UUID, Double> similarUserVector = allUserVectors.get(similarUser.userId());

            for (Map.Entry<UUID, Double> product : similarUserVector.entrySet()) {
                if (targetVector.containsKey(product.getKey())) {
                    continue; // already seen/bought by the target user
                }
                candidateScores.merge(
                        product.getKey(), similarUser.similarity() * product.getValue(), (a, b) -> a + b);
            }
        }

        // Step 5: top-N candidates by accumulated score.
        return candidateScores.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> new ScoredProduct(entry.getKey(), entry.getValue(), "similar_users"))
                .collect(Collectors.toList());
    }
}
