-- EcoTrack PostgreSQL schema (reference; Spring JPA ddl-auto=update also applies migrations)

CREATE TABLE IF NOT EXISTS carbon_logs (
    id BIGSERIAL PRIMARY KEY,
    user_email VARCHAR(255) NOT NULL,
    activity_category VARCHAR(100) NOT NULL,
    co2_impact DOUBLE PRECISION NOT NULL,
    description VARCHAR(500),
    logged_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_carbon_logs_user_email_logged_at
    ON carbon_logs (user_email, logged_at DESC);

CREATE TABLE IF NOT EXISTS goals (
    id BIGSERIAL PRIMARY KEY,
    user_email VARCHAR(255) NOT NULL,
    title VARCHAR(150) NOT NULL,
    target_carbon_reduction DOUBLE PRECISION NOT NULL,
    current_progress DOUBLE PRECISION NOT NULL DEFAULT 0,
    deadline DATE NOT NULL,
    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_goals_user_email_deadline
    ON goals (user_email, deadline);
