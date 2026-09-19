-- zipmart-backend-spring owns this table exclusively (IMPLEMENTATION_PLAN.md
-- section 0.4 / 5.7). zipmart-backend-nest's TypeORM migration already runs
-- this same CREATE EXTENSION on the shared database, but it's repeated here
-- (idempotent) so this migration is self-contained if ever run first.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE recommendations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    product_id UUID NOT NULL,
    score FLOAT NOT NULL,
    reason TEXT,
    computed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_recommendations_user_product UNIQUE (user_id, product_id)
);
