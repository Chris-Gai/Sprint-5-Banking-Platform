CREATE TABLE bank_transactions (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    transaction_ref     VARCHAR(36)     NOT NULL,            -- opaque public id (UUID)
    type                VARCHAR(20)     NOT NULL,            -- DEPOSIT, WITHDRAWAL, TRANSFER
    from_account_id     BIGINT          NULL,                -- null for deposits
    to_account_id       BIGINT          NULL,                -- null for withdrawals
    amount              DECIMAL(14,2)   NOT NULL,
    description         VARCHAR(255)    NULL,
    created_at          DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_transactions_ref (transaction_ref),
    INDEX idx_transactions_from (from_account_id),
    INDEX idx_transactions_to (to_account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
