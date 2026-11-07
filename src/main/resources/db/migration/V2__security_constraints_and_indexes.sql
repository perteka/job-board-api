-- Enforce the one-company-per-employer rule and case-insensitive company names.
ALTER TABLE companies
    ADD CONSTRAINT uk_companies_owner UNIQUE (owner_user_id);

CREATE UNIQUE INDEX uk_companies_name_lower ON companies (lower(name));

-- Support the most common ownership and user-list queries.
CREATE INDEX idx_applications_job_seeker ON applications (job_seeker_id);
CREATE INDEX idx_job_postings_created_by ON job_postings (created_by);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens (expires_at) WHERE revoked = false;

-- Existing refresh tokens are migrated to SHA-256 hashes before dropping plaintext.
CREATE EXTENSION IF NOT EXISTS pgcrypto;
ALTER TABLE refresh_tokens ADD COLUMN token_hash VARCHAR(64);
UPDATE refresh_tokens
SET token_hash = encode(digest(token, 'sha256'), 'hex');
ALTER TABLE refresh_tokens ALTER COLUMN token_hash SET NOT NULL;
ALTER TABLE refresh_tokens ADD CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash);
ALTER TABLE refresh_tokens DROP CONSTRAINT uk_refresh_tokens_token;
ALTER TABLE refresh_tokens DROP COLUMN token;
