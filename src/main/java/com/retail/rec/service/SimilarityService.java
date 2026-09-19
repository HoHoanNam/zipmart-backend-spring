package com.retail.rec.service;

import com.retail.rec.algorithm.UserSimilarity;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Orchestrates {@link UserSimilarity} (the pure cosine-similarity math) into
 * "find the top-K most similar users" — IMPLEMENTATION_PLAN.md section 5.4
 * steps 1-2.
 */
@Service
public class SimilarityService {

    private final UserSimilarity userSimilarity;

    public SimilarityService(UserSimilarity userSimilarity) {
        this.userSimilarity = userSimilarity;
    }

    public record SimilarUser(UUID userId, double similarity) {
    }

    public List<SimilarUser> findTopSimilarUsers(
            UUID targetUserId, Map<UUID, Map<UUID, Double>> allUserVectors, int topK) {
        Map<UUID, Double> targetVector = allUserVectors.get(targetUserId);
        if (targetVector == null || targetVector.isEmpty()) {
            return List.of();
        }

        return allUserVectors.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(targetUserId))
                .map(entry -> new SimilarUser(
                        entry.getKey(), userSimilarity.cosineSimilarity(targetVector, entry.getValue())))
                .filter(similarUser -> similarUser.similarity() > 0)
                .sorted(Comparator.comparingDouble(SimilarUser::similarity).reversed())
                .limit(topK)
                .collect(Collectors.toList());
    }
}
