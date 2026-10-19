-- =============================================================
-- V2__create_alumni_profiles_table.sql
-- Alumni profiles table with deduplication on linkedin_profile_url
-- =============================================================

CREATE TABLE alumni_profiles (
    id                    BIGSERIAL PRIMARY KEY,
    name                  VARCHAR(255),
    current_designation   VARCHAR(255),
    university            VARCHAR(255) NOT NULL,
    location              VARCHAR(255),
    linkedin_headline     TEXT,
    linkedin_profile_url  VARCHAR(500) UNIQUE,
    passout_year          INTEGER,
    search_job_id         BIGINT REFERENCES search_jobs(id) ON DELETE SET NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_alumni_passout_year
        CHECK (passout_year IS NULL OR (passout_year >= 1900 AND passout_year <= 2100))
);

-- Index for common filter queries
CREATE INDEX idx_alumni_university            ON alumni_profiles (LOWER(university));
CREATE INDEX idx_alumni_current_designation   ON alumni_profiles (LOWER(current_designation));
CREATE INDEX idx_alumni_passout_year  ON alumni_profiles (passout_year);
CREATE INDEX idx_alumni_search_job    ON alumni_profiles (search_job_id);
CREATE INDEX idx_alumni_created       ON alumni_profiles (created_at DESC);

-- Function + trigger to auto-update updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_alumni_profiles_updated_at
    BEFORE UPDATE ON alumni_profiles
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
