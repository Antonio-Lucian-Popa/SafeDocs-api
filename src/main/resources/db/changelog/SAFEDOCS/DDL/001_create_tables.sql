-- liquibase formatted sql
-- changeset safedocs:001 createExtensionPgcrypto
-- preconditions:
--   onFail: MARK_RAN
--   onError: HALT
--   sqlCheck: SELECT 1
CREATE EXTENSION IF NOT EXISTS pgcrypto;
-- rollback DROP EXTENSION IF EXISTS pgcrypto;

-- =====================================================================
-- USERS
-- =====================================================================
-- changeset safedocs:010 createTableUsers
CREATE TABLE users (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email            VARCHAR(255) NOT NULL,
    password_hash    VARCHAR(255),                -- null dacă vine din Google only
    display_name     VARCHAR(255),
    provider         VARCHAR(32) NOT NULL DEFAULT 'LOCAL',  -- LOCAL | GOOGLE (text simplu)
    google_sub       VARCHAR(64),                 -- subject-ul Google (pentru login federat)
    is_active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_users_email UNIQUE (email)
);
-- rollback DROP TABLE IF EXISTS users;

-- changeset safedocs:011 indexUsersGoogleSub
CREATE INDEX IF NOT EXISTS idx_users_google_sub ON users(google_sub);
-- rollback DROP INDEX IF EXISTS idx_users_google_sub;

-- =====================================================================
-- FOLDERS
-- =====================================================================
-- changeset safedocs:020 createTableFolders
CREATE TABLE folders (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    parent_folder_id   UUID REFERENCES folders(id) ON DELETE CASCADE,
    name               VARCHAR(255) NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    -- Unicitate: același nume nu poate apărea de două ori în același părinte pentru același user
    CONSTRAINT uq_folder_name_per_parent UNIQUE (user_id, parent_folder_id, name)
);
-- rollback DROP TABLE IF EXISTS folders;

-- changeset safedocs:021 indexFoldersByUserParent
CREATE INDEX IF NOT EXISTS idx_folders_user_parent ON folders(user_id, parent_folder_id);
-- rollback DROP INDEX IF EXISTS idx_folders_user_parent;

-- =====================================================================
-- DOCUMENTS
-- =====================================================================
-- changeset safedocs:030 createTableDocuments
CREATE TABLE documents (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    folder_id      UUID REFERENCES folders(id) ON DELETE SET NULL,
    title          VARCHAR(255) NOT NULL,
    file_path      TEXT NOT NULL,               -- path local pe disc (relativ față de root-ul app-ului)
    mime_type      VARCHAR(255),
    file_size      BIGINT,                      -- bytes
    checksum_sha256 CHAR(64),                   -- opțional, pentru integritate/duplicat
    tags           JSONB,                       -- ex: {"type":"RCA","number":"...","issuer":"..."}
    expires_at     TIMESTAMPTZ,                 -- dată expirare (dacă e setată)
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    -- Unicitate: titlu în cadrul aceluiași folder (ca în drive)
    CONSTRAINT uq_doc_title_per_folder UNIQUE (user_id, folder_id, title)
);
-- rollback DROP TABLE IF EXISTS documents;

-- changeset safedocs:031 indexDocumentsSearch
CREATE INDEX IF NOT EXISTS idx_documents_user_folder ON documents(user_id, folder_id);
-- rollback DROP INDEX IF EXISTS idx_documents_user_folder;

-- changeset safedocs:032 indexDocumentsExpires
CREATE INDEX IF NOT EXISTS idx_documents_expires_at ON documents(expires_at);
-- rollback DROP INDEX IF EXISTS idx_documents_expires_at;

-- changeset safedocs:033 ginIndexDocumentsTags
CREATE INDEX IF NOT EXISTS idx_documents_tags_gin ON documents USING GIN (tags);
-- rollback DROP INDEX IF EXISTS idx_documents_tags_gin;

-- =====================================================================
-- DOCUMENT VERSIONS
-- =====================================================================
-- changeset safedocs:040 createTableDocumentVersions
CREATE TABLE document_versions (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id    UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    version_no     INTEGER NOT NULL DEFAULT 1,
    file_path      TEXT NOT NULL,       -- fiecare versiune are propriul fișier
    mime_type      VARCHAR(255),
    file_size      BIGINT,
    checksum_sha256 CHAR(64),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_doc_version UNIQUE (document_id, version_no)
);
-- rollback DROP TABLE IF EXISTS document_versions;

-- changeset safedocs:041 indexDocVersionsDocId
CREATE INDEX IF NOT EXISTS idx_document_versions_document_id ON document_versions(document_id);
-- rollback DROP INDEX IF EXISTS idx_document_versions_document_id;

-- =====================================================================
-- REMINDERS
-- =====================================================================
-- changeset safedocs:050 createTableReminders
CREATE TABLE reminders (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    document_id   UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    -- remind_offset_days: de ex. 30, 7, 1 (cu cât timp înainte de expires_at trimitem)
    remind_offset_days INTEGER NOT NULL CHECK (remind_offset_days IN (1,7,30)),
    scheduled_for TIMESTAMPTZ NOT NULL,   -- când ar trebui să fie trimis (calculat din expires_at - offset)
    sent_at       TIMESTAMPTZ,            -- null dacă încă nu e trimis
    channel       VARCHAR(16) NOT NULL DEFAULT 'EMAIL', -- EMAIL | PUSH (simplu)
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_reminder_unique_per_doc_offset UNIQUE (document_id, remind_offset_days, channel)
);
-- rollback DROP TABLE IF EXISTS reminders;

-- changeset safedocs:051 indexRemindersDue
CREATE INDEX IF NOT EXISTS idx_reminders_due ON reminders(scheduled_for) WHERE sent_at IS NULL;
-- rollback DROP INDEX IF EXISTS idx_reminders_due;

-- =====================================================================
-- MISC / TRIGGER UPDATE 'updated_at'
-- =====================================================================
-- changeset safedocs:060 updatedAtFunction
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END
$$ LANGUAGE plpgsql;
-- rollback DROP FUNCTION IF EXISTS set_updated_at();

-- changeset safedocs:061 updatedAtTriggers
-- users
DROP TRIGGER IF EXISTS trg_users_updated_at ON users;
CREATE TRIGGER trg_users_updated_at
BEFORE UPDATE ON users
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- folders
DROP TRIGGER IF EXISTS trg_folders_updated_at ON folders;
CREATE TRIGGER trg_folders_updated_at
BEFORE UPDATE ON folders
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- documents
DROP TRIGGER IF EXISTS trg_documents_updated_at ON documents;
CREATE TRIGGER trg_documents_updated_at
BEFORE UPDATE ON documents
FOR EACH ROW EXECUTE FUNCTION set_updated_at();
-- rollback
-- DROP TRIGGER IF EXISTS trg_users_updated_at ON users;
-- DROP TRIGGER IF EXISTS trg_folders_updated_at ON folders;
-- DROP TRIGGER IF EXISTS trg_documents_updated_at ON documents;

-- =====================================================================
-- OPTIONAL: ROOT FOLDER per user (poți popula la creare user din aplicație)
-- =====================================================================
-- changeset safedocs:070 helperViewExpiringSoon
CREATE OR REPLACE VIEW v_documents_expiring_soon AS
SELECT
  d.id        AS document_id,
  d.user_id,
  d.title,
  d.expires_at,
  GREATEST(0, (d.expires_at::date - CURRENT_DATE))::int AS days_left
FROM documents d
WHERE d.expires_at IS NOT NULL
  AND d.expires_at::date >= CURRENT_DATE
  AND (d.expires_at::date - CURRENT_DATE) <= 30;
-- rollback DROP VIEW IF EXISTS v_documents_expiring_soon;

