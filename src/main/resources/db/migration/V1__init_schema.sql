CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(150) NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_categories_name UNIQUE (name),
    CONSTRAINT uk_categories_slug UNIQUE (slug)
);

CREATE TABLE skills (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_skills_name UNIQUE (name),
    CONSTRAINT uk_skills_slug UNIQUE (slug)
);

CREATE TABLE companies (
    id BIGSERIAL PRIMARY KEY,
    owner_user_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    website VARCHAR(255),
    location VARCHAR(255),
    logo_url VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_companies_name UNIQUE (name),
    CONSTRAINT fk_companies_owner FOREIGN KEY (owner_user_id) REFERENCES users (id)
);

CREATE TABLE job_seeker_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    headline VARCHAR(150),
    summary TEXT,
    experience_years INTEGER,
    cv_url VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_job_seeker_profiles_user UNIQUE (user_id),
    CONSTRAINT fk_job_seeker_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE job_postings (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    created_by BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    requirements TEXT,
    location VARCHAR(150) NOT NULL,
    employment_type VARCHAR(30) NOT NULL,
    work_mode VARCHAR(20) NOT NULL,
    min_salary NUMERIC(12, 2),
    max_salary NUMERIC(12, 2),
    status VARCHAR(20) NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_job_postings_company FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT fk_job_postings_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT fk_job_postings_creator FOREIGN KEY (created_by) REFERENCES users (id)
);

CREATE INDEX idx_job_postings_status ON job_postings (status);
CREATE INDEX idx_job_postings_location ON job_postings (location);
CREATE INDEX idx_job_postings_company ON job_postings (company_id);

CREATE TABLE job_posting_skills (
    job_posting_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    PRIMARY KEY (job_posting_id, skill_id),
    CONSTRAINT fk_job_posting_skills_job FOREIGN KEY (job_posting_id) REFERENCES job_postings (id) ON DELETE CASCADE,
    CONSTRAINT fk_job_posting_skills_skill FOREIGN KEY (skill_id) REFERENCES skills (id) ON DELETE CASCADE
);

CREATE TABLE applications (
    id BIGSERIAL PRIMARY KEY,
    job_posting_id BIGINT NOT NULL,
    job_seeker_id BIGINT NOT NULL,
    cover_letter TEXT,
    cv_url VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_applications_job_seeker UNIQUE (job_posting_id, job_seeker_id),
    CONSTRAINT fk_applications_job FOREIGN KEY (job_posting_id) REFERENCES job_postings (id),
    CONSTRAINT fk_applications_seeker FOREIGN KEY (job_seeker_id) REFERENCES users (id)
);

CREATE TABLE saved_jobs (
    user_id BIGINT NOT NULL,
    job_posting_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (user_id, job_posting_id),
    CONSTRAINT fk_saved_jobs_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_saved_jobs_job FOREIGN KEY (job_posting_id) REFERENCES job_postings (id) ON DELETE CASCADE
);

CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(500) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_refresh_tokens_token UNIQUE (token),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
