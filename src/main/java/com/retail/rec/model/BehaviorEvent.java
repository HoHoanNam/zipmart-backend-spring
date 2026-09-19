package com.retail.rec.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Read-only mapping onto the `behavior_events` table. That table is owned
 * and migrated by zipmart-backend-nest (TypeORM) — this entity never writes
 * to it, only reads, per IMPLEMENTATION_PLAN.md section 0.4.
 */
@Entity
@Table(name = "behavior_events")
public class BehaviorEvent {

    @Id
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "event_weight")
    private Double eventWeight;

    @Column(name = "occurred_at")
    private Instant occurredAt;

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getEventType() {
        return eventType;
    }

    public Double getEventWeight() {
        return eventWeight;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
