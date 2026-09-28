# Zipmart Backend Spring — Project Context & Coding Conventions

## Project Context

This repo is 1 of 5 sibling repos in the Zipmart system (`zipmart-frontend-web`,
`zipmart-admin-web`, `zipmart-mobile`, `zipmart-backend-nest`,
`zipmart-backend-spring`). This is the **Recommendation Engine** — an
internal-only service, never exposed to the internet, called exclusively by
`zipmart-backend-nest`'s `RecProxyService`. No client (web, admin, mobile)
ever talks to this service directly.

Port: **8080**. Data ownership: this repo owns and migrates (Flyway) the
`recommendations` table exclusively. It only **reads** `behavior_events`,
which `zipmart-backend-nest` owns and writes (TypeORM). Both services share
the same PostgreSQL 17 database.

## Spring Boot Coding Conventions

- **JDK 21, Spring Boot 4.1.1** (generated via start.spring.io — this is a
  newer major than the "3.x" era most tutorials assume; starter artifact
  names changed: `spring-boot-starter-webmvc` (not `-web`),
  `spring-boot-starter-flyway` is now its own starter (not just adding the
  flywaydb dependency directly).
- Package structure: `controller / service / repository / model / algorithm
  / scheduler / config`, per `docs/PROJECT-IMPLEMENTATION-PLAN.md` (monorepo
  root) section 5.2 — every new class goes in the layer that matches its
  role, don't collapse layers.
- **No Lombok** — every entity/DTO has explicit getters/constructors, per
  `docs/PROJECT-IMPLEMENTATION-PLAN.md` section 5.3 ("giai đoạn đầu").
- `spring.jpa.hibernate.ddl-auto=none` — Hibernate must **never** create or
  alter schema. `BehaviorEvent` maps onto a table this service doesn't own;
  letting Hibernate anywhere near DDL for it would be a real hazard, not
  just unnecessary.
- Internal-only API: no Spring Security, no auth layer. Do not add one
  casually — if this service ever needs to be reachable from anywhere but
  `zipmart-backend-nest`'s internal Docker network, that's an architecture
  change to discuss first, not a config tweak.

## Three real gotchas hit while getting this running (read before touching datasource/Flyway config)

1. **pgjdbc sends the JVM's default timezone as a Postgres connection
   startup parameter.** On this Windows dev machine, the JVM resolved the
   host timezone to the legacy alias `"Asia/Saigon"`, which Postgres 17
   doesn't recognize — every single connection attempt failed immediately
   with `FATAL: invalid value for parameter "TimeZone"`, before any SQL
   (including Flyway's own bootstrap queries) could run. Fixed by forcing
   `TimeZone.setDefault(TimeZone.getTimeZone("UTC"))` as the very first line
   of `main()` in `ZipmartBackendSpringApplication`, so this is
   host-independent — don't remove it.
2. **Flyway refuses to run on a non-empty schema with no history table.**
   This database already has `zipmart-backend-nest`'s tables
   (`users`, `products`, `orders`, ...) from TypeORM, but no
   `flyway_schema_history` table existed yet — Flyway's safety check blocks
   migration until you explicitly opt in. Fixed with
   `spring.flyway.baseline-on-migrate=true`.
3. **Flyway's default `baseline-version` is `1`, which collides with this
   repo's own `V1__create_recommendations_table.sql`.** Baselining AT
   version 1 makes Flyway believe V1 is already applied and silently skips
   it — the app starts up "successfully" but the `recommendations` table
   is never created, and every query against it 500s with
   `relation "recommendations" does not exist`. Fixed with
   `spring.flyway.baseline-version=0` explicitly, so V1 actually runs after
   baselining. If a future migration renumbers or a fresh migration set
   starts elsewhere, revisit this value.

All three are set in `application.properties`, each with a comment
explaining why — don't "clean up" those comments away, the reasoning isn't
obvious from the code alone.

## Recommendation Algorithm Skill

User-based Collaborative Filtering, split across three files matching
`docs/PROJECT-IMPLEMENTATION-PLAN.md` (monorepo root) section 5.4 exactly —
this 3-way split is deliberate, not incidental duplication:

- **`algorithm/UserSimilarity.java`** — pure math only: `cosineSimilarity(a, b)`
  between two `Map<UUID, Double>` behavior vectors. No Spring wiring beyond
  being a `@Component`, no orchestration.
- **`service/SimilarityService.java`** — orchestrates `UserSimilarity` into
  "find the top-K (20) most similar users to a target user" from the full
  set of user vectors.
- **`algorithm/CollaborativeFilter.java`** — takes `SimilarityService`'s
  top-K result, accumulates weighted candidate scores for products the
  target user hasn't interacted with, returns the top-N as
  `ScoredProduct(productId, score, reason)` records with `reason: "similar_users"`.

Data flow: `BehaviorAnalysisService.buildUserVectors()` (native aggregate
query, `SUM(event_weight) GROUP BY user_id, product_id`) →
`RecommendationService.recomputeAll()` runs `CollaborativeFilter` per user
and **upserts by delete-then-insert** into `recommendations` → `RecBatchJob`
runs this nightly at 2am (`@Scheduled(cron = "0 0 2 * * *")`).

- **Cold start**: a user with no `behavior_events` yet has no vector, so
  `CollaborativeFilter.recommend()` returns `List.of()` immediately.
  `zipmart-backend-nest`'s `RecProxyService` is the one that falls back to
  top-selling products when it gets an empty array back — this repo does
  not implement any fallback itself, by design.
- **Response shape matters**: `GET /api/rec/{userId}` returns a bare JSON
  array (`List<RecommendationResponse>`), **not** wrapped in an object —
  `RecProxyService.fetchFromSpring()` in `zipmart-backend-nest` expects
  exactly that shape. Don't wrap it in `{ "items": [...] }` even though
  that's what the *enriched* response backend-nest eventually returns to
  its own clients.
- **`POST /api/rec/recompute`** — not in the original plan, added as a
  pragmatic way to trigger `recomputeAll()` on demand instead of waiting
  for the 2am cron, useful for both ops and testing. Still internal-only,
  no auth.

## Current State

Full pipeline implemented and verified end-to-end: Flyway migration creates
`recommendations` alongside `zipmart-backend-nest`'s tables on the shared
database; `GET /api/rec/{userId}` and `POST /api/rec/recompute` both tested
directly; confirmed `zipmart-backend-nest`'s `RecProxyService` successfully
reaches this service live (checked its logs for the *absence* of the
"backend-spring unreachable" warning it logs on connection failure) and
receives a genuine (not connection-error) empty result, which correctly
continues to backend-nest's own top-selling fallback. Recommendations are
still empty in practice because the shared dev database only has 2 users and
sparse overlapping behavior data — not enough for collaborative filtering to
surface anything yet; that's a data-volume limitation, not a code issue.
