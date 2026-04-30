-- Create categories table
CREATE TABLE categories
(
    id               UUID PRIMARY KEY,
    category_name    VARCHAR(255) NOT NULL,
    transaction_type VARCHAR(50)  NOT NULL,
    user_id          UUID,
    active           BOOLEAN      NOT NULL,
    CONSTRAINT uk_user_category_name UNIQUE NULLS NOT DISTINCT (user_id, category_name),
    CONSTRAINT fk_categories_user_id FOREIGN KEY (user_id) REFERENCES users (id)
);

-- Create transactions table
CREATE TABLE transactions
(
    id               UUID PRIMARY KEY,
    user_id          UUID                     NOT NULL,
    category_id      UUID                     NOT NULL,
    amount           NUMERIC(19, 4)           NOT NULL,
    transaction_type VARCHAR(50)              NOT NULL,
    note             TEXT,
    idempotency_key  UUID UNIQUE,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    over_budget      BOOLEAN                  NOT NULL,
    CONSTRAINT fk_transactions_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_transactions_category_id FOREIGN KEY (category_id) REFERENCES categories (id)
);

-- Composite index for user and date queries.
CREATE INDEX idx_transaction_user_created ON transactions (user_id, created_at);

-- Create a function to check if critical fields are being modified
CREATE OR REPLACE FUNCTION prevent_transaction_updates()
    RETURNS TRIGGER AS '
    BEGIN
        -- Check if user_id is being updated
        IF NEW.user_id IS DISTINCT FROM OLD.user_id THEN
            RAISE EXCEPTION ''Updating user_id is strictly prohibited'';
        END IF;

        -- Check if amount is being updated
        IF NEW.amount IS DISTINCT FROM OLD.amount THEN
            RAISE EXCEPTION ''Updating amount is strictly prohibited'';
        END IF;

        -- Check if transaction_type is being updated
        IF NEW.transaction_type IS DISTINCT FROM OLD.transaction_type THEN
            RAISE EXCEPTION ''Updating transaction_type is strictly prohibited'';
        END IF;

        -- Check if created_at is being updated
        IF NEW.created_at IS DISTINCT FROM OLD.created_at THEN
            RAISE EXCEPTION ''Updating created_at is strictly prohibited'';
        END IF;

        -- Check if idempotency_key is being updated
        IF NEW.idempotency_key IS DISTINCT FROM OLD.idempotency_key THEN
            RAISE EXCEPTION ''Updating idempotency_key is strictly prohibited'';
        END IF;

        RETURN NEW;
    END;
' LANGUAGE plpgsql;

CREATE TRIGGER trg_prevent_transaction_updates
    BEFORE UPDATE ON transactions
    FOR EACH ROW
EXECUTE FUNCTION prevent_transaction_updates();