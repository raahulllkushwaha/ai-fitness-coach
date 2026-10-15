CREATE TABLE telegram_account_links (
                                        id BIGSERIAL PRIMARY KEY,

                                        user_id BIGINT NOT NULL,
                                        telegram_user_id BIGINT NOT NULL,
                                        telegram_username VARCHAR(100),

                                        linked_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                        CONSTRAINT fk_telegram_account_links_user
                                            FOREIGN KEY (user_id)
                                                REFERENCES users(id)
                                                ON DELETE CASCADE,

                                        CONSTRAINT uq_telegram_account_links_user
                                            UNIQUE (user_id),

                                        CONSTRAINT uq_telegram_account_links_telegram_user
                                            UNIQUE (telegram_user_id)
);