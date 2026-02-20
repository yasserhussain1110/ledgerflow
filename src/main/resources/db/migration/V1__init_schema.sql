-- payments table
CREATE TABLE payments (
                          id UUID PRIMARY KEY,
                          merchant_id UUID NOT NULL,
                          amount BIGINT NOT NULL,
                          currency VARCHAR(10) NOT NULL,
                          status VARCHAR(50) NOT NULL,
                          created_at TIMESTAMP NOT NULL,
                          updated_at TIMESTAMP NOT NULL
);

-- ledger_accounts table
CREATE TABLE ledger_accounts (
                                 id UUID PRIMARY KEY,
                                 name VARCHAR(100) NOT NULL,
                                 type VARCHAR(50) NOT NULL,
                                 created_at TIMESTAMP NOT NULL
);

-- ledger_entries table
CREATE TABLE ledger_entries (
                                id UUID PRIMARY KEY,
                                transaction_id UUID NOT NULL,
                                account_id UUID NOT NULL,
                                debit_amount BIGINT DEFAULT 0,
                                credit_amount BIGINT DEFAULT 0,
                                created_at TIMESTAMP NOT NULL,
                                CONSTRAINT fk_account FOREIGN KEY(account_id)
                                    REFERENCES ledger_accounts(id)
);

-- idempotency_keys table
CREATE TABLE idempotency_keys (
                                  id UUID PRIMARY KEY,
                                  idempotency_key VARCHAR(255) NOT NULL,
                                  request_hash TEXT NOT NULL,
                                  response_body TEXT NOT NULL,
                                  created_at TIMESTAMP NOT NULL,
                                  UNIQUE (idempotency_key)
);

-- Indexes
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_ledger_entries_account ON ledger_entries(account_id);
CREATE INDEX idx_idempotency_key ON idempotency_keys(idempotency_key);
