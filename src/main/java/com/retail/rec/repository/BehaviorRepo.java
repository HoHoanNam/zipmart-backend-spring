package com.retail.rec.repository;

import com.retail.rec.model.BehaviorEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BehaviorRepo extends JpaRepository<BehaviorEvent, UUID> {

    /**
     * user_vector[user_id] = { product_id: sum(event_weight) }, per
     * IMPLEMENTATION_PLAN.md section 5.4 step 1.
     */
    @Query(
            value = "SELECT user_id AS userId, product_id AS productId, SUM(event_weight) AS score "
                    + "FROM behavior_events GROUP BY user_id, product_id",
            nativeQuery = true)
    List<UserProductScore> findAggregatedScores();
}
