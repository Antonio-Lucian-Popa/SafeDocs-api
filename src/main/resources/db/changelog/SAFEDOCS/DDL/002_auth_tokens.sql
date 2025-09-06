-- liquibase formatted sql

-- =====================================================================
-- REFRESH TOKENS
-- =====================================================================

-- changeset safedocs:090 createTableRefreshTokens
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(200) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
-- rollback DROP TABLE IF EXISTS refresh_tokens;

-- changeset safedocs:091 indexesRefreshTokens
CREATE INDEX IF NOT EXISTS idx_rt_user   ON refresh_tokens(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_rt_token ON refresh_tokens(token);
-- rollback DROP INDEX IF EXISTS idx_rt_user;
-- rollback DROP INDEX IF EXISTS idx_rt_token;

-- =====================================================================
-- (OPȚIONAL) EXTENSIE UNACCENT pentru căutare fără diacritice
-- Dacă ai deja changeset pentru unaccent, poți ignora acesta.
-- =====================================================================
-- changeset safedocs:080 extUnaccent
CREATE EXTENSION IF NOT EXISTS unaccent;
-- rollback DROP EXTENSION IF EXISTS unaccent;
