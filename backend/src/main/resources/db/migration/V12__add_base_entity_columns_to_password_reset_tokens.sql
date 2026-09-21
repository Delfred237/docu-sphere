-- V12__add_base_entity_columns_to_password_reset_tokens.sql

ALTER TABLE password_reset_tokens
    ADD COLUMN public_id VARCHAR(32) NOT NULL UNIQUE,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Ajouter l'index sur public_id (comme pour les autres tables)
CREATE INDEX idx_password_reset_tokens_public_id ON password_reset_tokens(public_id);