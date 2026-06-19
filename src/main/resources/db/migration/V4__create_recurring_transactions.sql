-- Create Enum for recurrence frequency
CREATE TYPE frequency_enum AS ENUM ('ONCE', 'DAILY', 'WEEKLY', 'MONTHLY');

-- Create recurring transactions table
CREATE TABLE recurring_transactions
(
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    category_id UUID NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    transaction_type transaction_type_enum NOT NULL,
    note TEXT,
    frequency frequency_enum NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    next_occurrence_date DATE NOT NULL,
    execution_time TIME NOT NULL DEFAULT '00:00:00',
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_recurring_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_recurring_category FOREIGN KEY (category_id) REFERENCES categories (id)
);

-- Composite index for fast polling
CREATE INDEX idx_recurring_next_datetime ON recurring_transactions (next_occurrence_date, execution_time, active);
-- Composite index for dashboard
CREATE INDEX idx_recurring_user_next_date ON recurring_transactions (user_id, next_occurrence_date, active);