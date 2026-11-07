ALTER TABLE users
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE users SET email_verified = TRUE;

CREATE TABLE email_verification_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_email_verification_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_email_verification_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_email_verification_tokens_user ON email_verification_tokens (user_id);
