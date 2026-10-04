CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,

                       username VARCHAR(50) NOT NULL,
                       email VARCHAR(255) NOT NULL,

                       first_name VARCHAR(100) NOT NULL,
                       last_name VARCHAR(100),

                       created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                       CONSTRAINT uk_users_username UNIQUE (username),
                       CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE INDEX idx_users_created_at
    ON users (created_at);