-- =====================================================
-- ENABLE EXTENSIONS
-- =====================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";



-- =====================================================
-- MERCHANTS
-- =====================================================

CREATE TABLE merchants (
                           id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                           name VARCHAR(150) NOT NULL,
                           email VARCHAR(150) UNIQUE NOT NULL,
                           created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);



-- =====================================================
-- PAYMENTS
-- =====================================================

CREATE TABLE payments (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                          merchant_id UUID NOT NULL,

                          amount BIGINT NOT NULL, -- stored in minor units (cents/paise)

                          currency VARCHAR(10) NOT NULL,

                          status VARCHAR(50) NOT NULL, -- matches PaymentStatus enum

                          version BIGINT NOT NULL DEFAULT 0,

                          created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
                          updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

ALTER TABLE payments
    ADD CONSTRAINT fk_payments_merchant
        FOREIGN KEY (merchant_id)
            REFERENCES merchants(id);

CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_merchant ON payments(merchant_id);



-- =====================================================
-- LEDGER ACCOUNTS
-- =====================================================

CREATE TABLE ledger_accounts (
                                 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                 merchant_id UUID NOT NULL,

                                 name VARCHAR(100) NOT NULL,

                                 type VARCHAR(50) NOT NULL, -- ASSET, LIABILITY, REVENUE

                                 created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

ALTER TABLE ledger_accounts
    ADD CONSTRAINT fk_ledger_accounts_merchant
        FOREIGN KEY (merchant_id)
            REFERENCES merchants(id);

CREATE INDEX idx_ledger_accounts_merchant
    ON ledger_accounts(merchant_id);



-- =====================================================
-- LEDGER ENTRIES
-- =====================================================

CREATE TABLE ledger_entries (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                transaction_id UUID NOT NULL, -- payment id

                                account_id UUID NOT NULL,

                                debit_amount BIGINT NOT NULL DEFAULT 0,
                                credit_amount BIGINT NOT NULL DEFAULT 0,

                                created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

ALTER TABLE ledger_entries
    ADD CONSTRAINT fk_ledger_entries_account
        FOREIGN KEY (account_id)
            REFERENCES ledger_accounts(id);

CREATE INDEX idx_ledger_entries_account
    ON ledger_entries(account_id);

CREATE INDEX idx_ledger_entries_transaction
    ON ledger_entries(transaction_id);



-- =====================================================
-- IDEMPOTENCY KEYS
-- =====================================================

CREATE TABLE idempotency_keys (
                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                  merchant_id UUID NOT NULL,

                                  idempotency_key VARCHAR(100) NOT NULL,

                                  request_hash VARCHAR(255) NOT NULL,

                                  response_payload TEXT,

                                  status VARCHAR(50) NOT NULL, -- IN_PROGRESS, COMPLETED, FAILED

                                  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

                                  expires_at TIMESTAMP WITH TIME ZONE
);

ALTER TABLE idempotency_keys
    ADD CONSTRAINT fk_idempotency_merchant
        FOREIGN KEY (merchant_id)
            REFERENCES merchants(id);

-- A merchant cannot reuse same idempotency key
CREATE UNIQUE INDEX ux_idempotency_merchant_key
    ON idempotency_keys (merchant_id, idempotency_key);

CREATE INDEX idx_idempotency_expires
    ON idempotency_keys (expires_at);



-- =====================================================
-- SYSTEM SEED DATA
-- =====================================================

-- Create a default merchant

INSERT INTO merchants (id, name, email)
VALUES (
           '11111111-1111-1111-1111-111111111111',
           'Default Merchant',
           'merchant@ledgerflow.com'
       );


-- Create system ledger accounts for default merchant

INSERT INTO ledger_accounts (merchant_id, name, type)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'Cash Account', 'ASSET'),
    ('11111111-1111-1111-1111-111111111111', 'Revenue Account', 'REVENUE'),
    ('11111111-1111-1111-1111-111111111111', 'Clearing Account', 'LIABILITY');

ALTER TABLE payments
    ADD CONSTRAINT chk_payment_status
        CHECK (status IN ('INITIATED','AUTHORIZED','CAPTURED','FAILED','CANCELED'));
