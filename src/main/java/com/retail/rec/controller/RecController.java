package com.retail.rec.controller;

import com.retail.rec.service.RecommendationService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal-only API — never exposed to the internet, called exclusively by
 * zipmart-backend-nest's RecProxyService. No auth layer here by design; see
 * IMPLEMENTATION_PLAN.md section 5.1/5.3.
 */
@RestController
@RequestMapping("/api/rec")
public class RecController {

    private final RecommendationService recommendationService;

    public RecController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/{userId}")
    public List<RecommendationResponse> getRecommendations(
            @PathVariable UUID userId, @RequestParam(defaultValue = "10") int limit) {
        return recommendationService.getRecommendations(userId, limit).stream()
                .map(rec -> new RecommendationResponse(rec.getProductId(), rec.getScore(), rec.getReason()))
                .toList();
    }

    /**
     * Manual trigger for the nightly batch (IMPLEMENTATION_PLAN.md section
     * 5.5 normally runs this at 2am via RecBatchJob) — useful for ops and
     * for verifying the pipeline without waiting for the cron.
     *
     * <p>Returns 409 instead of running again if a recompute is already in
     * progress (see {@link RecommendationService#recomputeAll()}) — this
     * endpoint has no auth/rate-limit layer by design (internal-only), so
     * the in-progress guard is what stops repeated calls from piling up
     * concurrent O(n^2) passes over the same data.
     */
    @PostMapping("/recompute")
    public ResponseEntity<Void> recompute() {
        boolean started = recommendationService.recomputeAll();
        return started ? ResponseEntity.accepted().build() : ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
}
