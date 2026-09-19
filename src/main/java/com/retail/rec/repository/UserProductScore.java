package com.retail.rec.repository;

import java.util.UUID;

/** Projection for the aggregated `SUM(event_weight) GROUP BY user_id, product_id` query. */
public interface UserProductScore {
    UUID getUserId();

    UUID getProductId();

    Double getScore();
}
