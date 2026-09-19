package com.retail.rec.scheduler;

import com.retail.rec.service.RecommendationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RecBatchJob {

    private static final Logger log = LoggerFactory.getLogger(RecBatchJob.class);

    private final RecommendationService recommendationService;

    public RecBatchJob(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    /** 2h sáng mỗi ngày — IMPLEMENTATION_PLAN.md section 5.5. */
    @Scheduled(cron = "0 0 2 * * *")
    public void recomputeAllRecommendations() {
        log.info("Starting nightly recommendation recompute");
        recommendationService.recomputeAll();
        log.info("Finished nightly recommendation recompute");
    }
}
