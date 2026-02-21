-- ============================================
-- LedgerFlow - Initial Schema
-- ============================================

-- Enable UUID generation (safe if already enabled)
CREATE EXTENSION IF NOT EXISTS pgcrypto;


-- ============================================
-- PAYMENTS
-- ============================================

CREATE TABLE payments (
                          id UUID PRIMARY KEY,
                          merchant_id UUID NOT NULL,
                          amount NUMERIC(19,4) NOT NULL,
                          currency VARCHAR(10) NOT NULL,
                          status VARCHAR(50) NOT NULL,
                          created_at TIMESTAMP NOT NULL,
                          updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_payments_status
    ON payments(status);



-- ============================================
-- LEDGER ACCOUNTS
-- ============================================

CREATE TABLE ledger_accounts (
                                 id UUID PRIMARY KEY,
                                 name VARCHAR(100) NOT NULL UNIQUE,
                                 type VARCHAR(50) NOT NULL,
                                 created_at TIMESTAMP NOT NULL
);



-- ============================================
-- LEDGER ENTRIES (Append-only Double Entry)
-- ============================================

CREATE TABLE ledger_entries (
                                id UUID PRIMARY KEY,
                                payment_id UUID NOT NULL,
                                account_id UUID NOT NULL,
                                type VARCHAR(10) NOT NULL,
                                amount NUMERIC(19,4) NOT NULL,
                                created_at TIMESTAMP NOT NULL,

                                CONSTRAINT fk_ledger_account
                                    FOREIGN KEY (account_id)
                                        REFERENCES ledger_accounts(id),

                                CONSTRAINT fk_ledger_payment
                                    FOREIGN KEY (payment_id)
                                        REFERENCES payments(id),

                                CONSTRAINT chk_ledger_entry_type
                                    CHECK (type IN ('DEBIT', 'CREDIT'))
);

CREATE INDEX idx_ledger_entries_account
    ON ledger_entries(account_id);

CREATE INDEX idx_ledger_entries_payment
    ON ledger_entries(payment_id);



-- ============================================
-- IDEMPOTENCY KEYS
-- ============================================

CREATE TABLE idempotency_keys (
                                  id UUID PRIMARY KEY,
                                  idempotency_key VARCHAR(255) NOT NULL,
                                  request_hash TEXT NOT NULL,
                                  response_body TEXT NOT NULL,
                                  created_at TIMESTAMP NOT NULL,

                                  CONSTRAINT uq_idempotency_key UNIQUE (idempotency_key)
);

CREATE INDEX idx_idempotency_key
    ON idempotency_keys(idempotency_key);



-- ============================================
-- INITIAL SYSTEM ACCOUNTS
-- ============================================

INSERT INTO ledger_accounts (id, name, type, created_at)
VALUES
    (gen_random_uuid(), 'PLATFORM_CASH', 'ASSET', now()),
    (gen_random_uuid(), 'MERCHANT_BALANCE', 'LIABILITY', now());
