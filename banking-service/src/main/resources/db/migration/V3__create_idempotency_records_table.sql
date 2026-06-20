CREATE TABLE idempotency_records (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    idempotency_key     VARCHAR(100)    NOT NULL,
    user_id             BIGINT          NOT NULL,            -- no FK; auth-service owns users
    request_path        VARCHAR(255)    NOT NULL,
    request_hash        VARCHAR(64)     NOT NULL,            -- detects same key reused with a different body
    response_status     INT             NOT NULL,
    response_body       TEXT            NOT NULL,
    created_at          DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_idempotency_key_user (idempotency_key, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
