-- Insert the Default Income Category
INSERT INTO categories (id, user_id, category_name, transaction_type, active, created_at)
VALUES (
           gen_random_uuid(),
           NULL,
           'Default Income',
           'INCOME',
           true,
           CURRENT_TIMESTAMP
       );

-- Insert the Default Expense Category
INSERT INTO categories (id, user_id, category_name, transaction_type, active, created_at)
VALUES (
           gen_random_uuid(),
           NULL,
           'Default Expense',
           'EXPENSE',
           true,
           CURRENT_TIMESTAMP
       );