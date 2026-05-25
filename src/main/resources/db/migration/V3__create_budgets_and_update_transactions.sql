-- Create budgets table
CREATE TABLE budgets
(
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    category_id UUID NOT NULL,
    amount_limit NUMERIC(19, 4) NOT NULL,
    budget_month INT NOT NULL,
    budget_year INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_budgets_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_budgets_category_id FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT uk_user_category_month_year UNIQUE (user_id, category_id, budget_month, budget_year)
);

-- Add is_over_budget flag to transaction table
ALTER TABLE transactions
ADD COLUMN is_over_budget BOOLEAN NOT NULL DEFAULT false;