package com.retail.rec.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

/**
 * Owned exclusively by zipmart-backend-spring — migrated here via Flyway
 * (V1__create_recommendations_table.sql), not by zipmart-backend-nest.
 */
@Entity
@Table(
        name = "recommendations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "product_id"}))
public class Recommendation {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private Double score;

    private String reason;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;

    public Recommendation() {
    }

    public Recommendation(UUID userId, UUID productId, Double score, String reason, Instant computedAt) {
        this.userId = userId;
        this.productId = productId;
        this.score = score;
        this.reason = reason;
        this.computedAt = computedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getProductId() {
        return productId;
    }

    public Double getScore() {
        return score;
    }

    public String getReason() {
        return reason;
    }

    public Instant getComputedAt() {
        return computedAt;
    }
}
