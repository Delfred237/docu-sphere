-- V4__refactor_verification_for_otp.sql

-- Suppression de l'ancienne contrainte d'unicité sur le token UUID
ALTER TABLE email_verification_tokens DROP CONSTRAINT IF EXISTS email_verification_tokens_token_key;

-- Renommage de la colonne token en code
ALTER TABLE email_verification_tokens RENAME COLUMN token TO code;

-- Ajustement du type pour un code à 6 chiffres
ALTER TABLE email_verification_tokens ALTER COLUMN code TYPE VARCHAR(6);

-- Ajout du compteur de tentatives (sécurité anti-brute-force)
ALTER TABLE email_verification_tokens ADD COLUMN attempts INTEGER NOT NULL DEFAULT 0;

-- Un utilisateur ne doit avoir qu'un seul code actif à la fois
ALTER TABLE email_verification_tokens ADD CONSTRAINT uk_email_verification_tokens_user_id UNIQUE (user_id);

-- Suppression de l'ancien index sur token
DROP INDEX IF EXISTS idx_email_verification_tokens_token;

-- Index sur le code pour accélérer la vérification (si besoin)
CREATE INDEX idx_email_verification_tokens_code ON email_verification_tokens(code);