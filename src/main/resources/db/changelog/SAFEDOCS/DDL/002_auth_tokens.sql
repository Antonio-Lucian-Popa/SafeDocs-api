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



-- changeset safedocs:100 alterChecksumDocs
ALTER TABLE documents
  ALTER COLUMN checksum_sha256 TYPE VARCHAR(64);

-- rollback
-- ALTER TABLE documents ALTER COLUMN checksum_sha256 TYPE CHAR(64);


-- changeset safedocs:101 alterChecksumDocVersions
ALTER TABLE document_versions
  ALTER COLUMN checksum_sha256 TYPE VARCHAR(64);

-- rollback
-- ALTER TABLE document_versions ALTER COLUMN checksum_sha256 TYPE CHAR(64);


-- =====================================================================
-- FOLDER SHARES (share direct către utilizatori existenți)
-- =====================================================================

-- changeset safedocs:110 createTableFolderShares
CREATE TABLE folder_shares (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    folder_id           UUID NOT NULL REFERENCES folders(id) ON DELETE CASCADE,
    shared_with_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    permission          VARCHAR(16) NOT NULL DEFAULT 'READ', -- READ | WRITE
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by_user_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_folder_share UNIQUE (folder_id, shared_with_user_id),
    CONSTRAINT ck_folder_share_perm CHECK (permission IN ('READ','WRITE'))
);
-- rollback DROP TABLE IF EXISTS folder_shares;

-- changeset safedocs:111 indexesFolderShares
CREATE INDEX IF NOT EXISTS idx_folder_shares_folder ON folder_shares(folder_id);
CREATE INDEX IF NOT EXISTS idx_folder_shares_shared_with ON folder_shares(shared_with_user_id);
-- rollback DROP INDEX IF EXISTS idx_folder_shares_folder;
-- rollback DROP INDEX IF EXISTS idx_folder_shares_shared_with;