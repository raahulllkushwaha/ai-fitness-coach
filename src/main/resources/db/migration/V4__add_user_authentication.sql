ALTER TABLE users
    ADD COLUMN password_hash VARCHAR(255);

ALTER TABLE users
    ADD COLUMN role VARCHAR(30) NOT NULL DEFAULT 'USER';

ALTER TABLE users
    ADD CONSTRAINT chk_users_role
        CHECK (role IN ('USER', 'ADMIN'));