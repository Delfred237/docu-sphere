-- V13__fix_timestamp_precision.sql
-- Fix timestamp precision for Hibernate 6.x compatibility
-- Hibernate 6 expects TIMESTAMP(6) WITH TIME ZONE, not TIMESTAMP WITH TIME ZONE

-- ============================================================================
-- Table: users
-- ============================================================================
ALTER TABLE users
ALTER COLUMN created_at TYPE TIMESTAMP(6) WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP(6) WITH TIME ZONE,
    ALTER COLUMN deleted_at TYPE TIMESTAMP(6) WITH TIME ZONE;

-- ============================================================================
-- Table: roles
-- ============================================================================
ALTER TABLE roles
ALTER COLUMN created_at TYPE TIMESTAMP(6) WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP(6) WITH TIME ZONE,
    ALTER COLUMN deleted_at TYPE TIMESTAMP(6) WITH TIME ZONE;

-- ============================================================================
-- Table: permissions
-- ============================================================================
ALTER TABLE permissions
ALTER COLUMN created_at TYPE TIMESTAMP(6) WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP(6) WITH TIME ZONE,
    ALTER COLUMN deleted_at TYPE TIMESTAMP(6) WITH TIME ZONE;

-- ============================================================================
-- Table: email_verification_tokens
-- ============================================================================
ALTER TABLE email_verification_tokens
ALTER COLUMN created_at TYPE TIMESTAMP(6) WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP(6) WITH TIME ZONE,
    ALTER COLUMN deleted_at TYPE TIMESTAMP(6) WITH TIME ZONE,
    ALTER COLUMN expiry_date TYPE TIMESTAMP(6) WITH TIME ZONE;

-- ============================================================================
-- Table: password_reset_tokens
-- ============================================================================
ALTER TABLE password_reset_tokens
ALTER COLUMN created_at TYPE TIMESTAMP(6) WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP(6) WITH TIME ZONE,
    ALTER COLUMN deleted_at TYPE TIMESTAMP(6) WITH TIME ZONE,
    ALTER COLUMN expiry_date TYPE TIMESTAMP(6) WITH TIME ZONE;

-- ============================================================================
-- Table: refresh_tokens
-- ============================================================================
ALTER TABLE refresh_tokens
ALTER COLUMN expiry_date TYPE TIMESTAMP(6) WITH TIME ZONE;

-- ============================================================================
-- Table: folders
-- ============================================================================
ALTER TABLE folders
ALTER COLUMN created_at TYPE TIMESTAMP(6) WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP(6) WITH TIME ZONE,
    ALTER COLUMN deleted_at TYPE TIMESTAMP(6) WITH TIME ZONE;

-- ============================================================================
-- Table: documents
-- ============================================================================
ALTER TABLE documents
ALTER COLUMN created_at TYPE TIMESTAMP(6) WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP(6) WITH TIME ZONE,
    ALTER COLUMN deleted_at TYPE TIMESTAMP(6) WITH TIME ZONE;

-- ============================================================================
-- Table: audit_logs
-- ============================================================================
ALTER TABLE audit_logs
ALTER COLUMN created_at TYPE TIMESTAMP(6) WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP(6) WITH TIME ZONE,
    ALTER COLUMN deleted_at TYPE TIMESTAMP(6) WITH TIME ZONE;

-- ============================================================================
-- Table: notifications
-- ============================================================================
ALTER TABLE notifications
ALTER COLUMN created_at TYPE TIMESTAMP(6) WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP(6) WITH TIME ZONE,
    ALTER COLUMN deleted_at TYPE TIMESTAMP(6) WITH TIME ZONE;

-- ============================================================================
-- Table: share_links
-- ============================================================================
ALTER TABLE share_links
ALTER COLUMN created_at TYPE TIMESTAMP(6) WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP(6) WITH TIME ZONE,
    ALTER COLUMN deleted_at TYPE TIMESTAMP(6) WITH TIME ZONE,
    ALTER COLUMN expires_at TYPE TIMESTAMP(6) WITH TIME ZONE;