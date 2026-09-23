-- V13__fix_timestamp_precision.sql
-- Fix timestamp precision for Hibernate 6.x compatibility
-- Hibernate 6 expects TIMESTAMP(6) WITH TIME ZONE, not TIMESTAMP WITH TIME ZONE

-- ============================================================================
-- Tables that extend BaseEntity (have created_at, updated_at, deleted_at)
-- ============================================================================

-- Table: users
ALTER TABLE users
ALTER COLUMN created_at TYPE TIMESTAMP(6) WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP(6) WITH TIME ZONE,
    ALTER COLUMN deleted_at TYPE TIMESTAMP(6) WITH TIME ZONE;

-- Table: roles
ALTER TABLE roles
ALTER COLUMN created_at TYPE TIMESTAMP(6) WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP(6) WITH TIME ZONE,
    ALTER COLUMN deleted_at TYPE TIMESTAMP(6) WITH TIME ZONE;

-- Table: permissions
ALTER TABLE permissions
ALTER COLUMN created_at TYPE TIMESTAMP(6) WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP(6) WITH TIME ZONE,
    ALTER COLUMN deleted_at TYPE TIMESTAMP(6) WITH TIME ZONE;

-- Table: password_reset_tokens (extends BaseEntity since V12)
ALTER TABLE password_reset_tokens
ALTER COLUMN created_at TYPE TIMESTAMP(6) WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP(6) WITH TIME ZONE,
    ALTER COLUMN deleted_at TYPE TIMESTAMP(6) WITH TIME ZONE,
    ALTER COLUMN expiry_date TYPE TIMESTAMP(6) WITH TIME ZONE;

-- Table: folders
ALTER TABLE folders
ALTER COLUMN created_at TYPE TIMESTAMP(6) WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP(6) WITH TIME ZONE,
    ALTER COLUMN deleted_at TYPE TIMESTAMP(6) WITH TIME ZONE;

-- Table: documents
ALTER TABLE documents
ALTER COLUMN created_at TYPE TIMESTAMP(6) WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP(6) WITH TIME ZONE,
    ALTER COLUMN deleted_at TYPE TIMESTAMP(6) WITH TIME ZONE;

-- Table: audit_logs
ALTER TABLE audit_logs
ALTER COLUMN created_at TYPE TIMESTAMP(6) WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP(6) WITH TIME ZONE,
    ALTER COLUMN deleted_at TYPE TIMESTAMP(6) WITH TIME ZONE;

-- Table: notifications
ALTER TABLE notifications
ALTER COLUMN created_at TYPE TIMESTAMP(6) WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP(6) WITH TIME ZONE,
    ALTER COLUMN deleted_at TYPE TIMESTAMP(6) WITH TIME ZONE;

-- Table: share_links
ALTER TABLE share_links
ALTER COLUMN created_at TYPE TIMESTAMP(6) WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP(6) WITH TIME ZONE,
    ALTER COLUMN deleted_at TYPE TIMESTAMP(6) WITH TIME ZONE,
    ALTER COLUMN expires_at TYPE TIMESTAMP(6) WITH TIME ZONE;

-- ============================================================================
-- Tables that DO NOT extend BaseEntity (only specific timestamp columns)
-- ============================================================================

-- Table: email_verification_tokens (only expiry_date, no created_at/updated_at/deleted_at)
ALTER TABLE email_verification_tokens
ALTER COLUMN expiry_date TYPE TIMESTAMP(6) WITH TIME ZONE;

-- Table: refresh_tokens (only expiry_date, no created_at/updated_at/deleted_at)
ALTER TABLE refresh_tokens
ALTER COLUMN expiry_date TYPE TIMESTAMP(6) WITH TIME ZONE;