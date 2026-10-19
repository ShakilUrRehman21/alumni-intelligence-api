-- =============================================================
-- V1__create_search_jobs_table.sql
-- Initial schema: search_jobs table
-- =============================================================

CREATE TABLE search_jobs (
    id               BIGSERIAL PRIMARY KEY,
    university       VARCHAR(255) NOT NULL,
    designation      VARCHAR(255) NOT NULL,
    passout_year     INTEGER,
    status           VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    phantombuster_run_id  VARCHAR(255),
    result_count     INTEGER      DEFAULT 0,
    error_message    TEXT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    completed_at     TIMESTAMPTZ,

    CONSTRAINT chk_search_jobs_status
        CHECK (status IN ('PENDING','RUNNING','COMPLETED','FAILED')),
    CONSTRAINT chk_passout_year
        CHECK (passout_year IS NULL OR (passout_year >= 1900 AND passout_year <= 2100))
);

CREATE INDEX idx_search_jobs_status    ON search_jobs (status);
CREATE INDEX idx_search_jobs_created   ON search_jobs (created_at DESC);
CREATE INDEX idx_search_jobs_university ON search_jobs (LOWER(university));
