CREATE TABLE intervals_icu_connections (
                                           id BIGSERIAL PRIMARY KEY,

                                           user_id BIGINT NOT NULL,

                                           access_token_encrypted TEXT NOT NULL,

                                           scopes VARCHAR(500),

                                           athlete_id VARCHAR(100),

                                           connected_at TIMESTAMPTZ NOT NULL
                                               DEFAULT CURRENT_TIMESTAMP,

                                           last_sync_at TIMESTAMPTZ,

                                           created_at TIMESTAMPTZ NOT NULL
                                               DEFAULT CURRENT_TIMESTAMP,

                                           updated_at TIMESTAMPTZ NOT NULL
                                               DEFAULT CURRENT_TIMESTAMP,

                                           CONSTRAINT fk_intervals_icu_connections_user
                                               FOREIGN KEY (user_id)
                                                   REFERENCES users(id)
                                                   ON DELETE CASCADE,

                                           CONSTRAINT uq_intervals_icu_connections_user
                                               UNIQUE (user_id)
);