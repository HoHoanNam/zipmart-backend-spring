package com.retail.rec.controller;

import java.util.UUID;

/**
 * Shape zipmart-backend-nest's RecProxyService expects from
 * `GET /api/rec/{userId}` — a plain array of these, not wrapped in an
 * object. See RecommendationItem in rec-proxy.service.ts.
 */
public record RecommendationResponse(UUID productId, double score, String reason) {
}
