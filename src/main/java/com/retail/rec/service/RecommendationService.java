package com.retail.rec.service;

import com.retail.rec.algorithm.CollaborativeFilter;
import com.retail.rec.model.Recommendation;
import com.retail.rec.repository.RecRepo;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecommendationService {

    private static final int RECOMMENDATIONS_PER_USER = 10;

    private final RecRepo recRepo;
    private final BehaviorAnalysisService behaviorAnalysisService;
    private final CollaborativeFilter collaborativeFilter;

    // Guards recomputeAll() against overlapping runs — it's O(n^2) in user
    // count and does a delete-then-insert per user, so two concurrent runs
    // (e.g. an unauthenticated caller hammering POST /api/rec/recompute
    // while the 2am cron is running) would race on the same rows and waste
    // CPU. See docs/PROJECT-SECURITY-AUDIT.md section 1.
    private final AtomicBoolean recomputeInProgress = new AtomicBoolean(false);

    public RecommendationService(
            RecRepo recRepo,
            BehaviorAnalysisService behaviorAnalysisService,
            CollaborativeFilter collaborativeFilter) {
        this.recRepo = recRepo;
        this.behaviorAnalysisService = behaviorAnalysisService;
        this.collaborativeFilter = collaborativeFilter;
    }

    /**
     * GET /api/rec/{userId} — reads pre-computed rows only, per
     * IMPLEMENTATION_PLAN.md section 5.6. No on-request calculation; that
     * only happens in {@link #recomputeAll()}, run nightly by RecBatchJob.
     */
    public List<Recommendation> getRecommendations(UUID userId, int limit) {
        return recRepo.findByUserIdOrderByScoreDesc(userId, PageRequest.of(0, limit));
    }

    /**
     * Recomputes and upserts recommendations for every user with behavior history.
     *
     * @return {@code true} if this call actually ran the recompute, {@code false}
     *     if it was skipped because another recompute was already in progress.
     */
    @Transactional
    public boolean recomputeAll() {
        if (!recomputeInProgress.compareAndSet(false, true)) {
            return false;
        }
        try {
            Map<UUID, Map<UUID, Double>> allUserVectors = behaviorAnalysisService.buildUserVectors();
            Instant now = Instant.now();

            for (UUID userId : allUserVectors.keySet()) {
                List<CollaborativeFilter.ScoredProduct> scored =
                        collaborativeFilter.recommend(userId, allUserVectors, RECOMMENDATIONS_PER_USER);
                upsertForUser(userId, scored, now);
            }
            return true;
        } finally {
            recomputeInProgress.set(false);
        }
    }

    private void upsertForUser(UUID userId, List<CollaborativeFilter.ScoredProduct> scored, Instant computedAt) {
        // Replace this user's existing recommendations with the freshly computed set,
        // per IMPLEMENTATION_PLAN.md section 5.5 ("xoá cũ, ghi mới").
        recRepo.deleteByUserId(userId);
        for (CollaborativeFilter.ScoredProduct product : scored) {
            recRepo.save(new Recommendation(
                    userId, product.productId(), product.score(), product.reason(), computedAt));
        }
    }
}
