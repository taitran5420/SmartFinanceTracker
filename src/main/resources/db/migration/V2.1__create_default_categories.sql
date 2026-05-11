-- Insert Default Expense Categories
INSERT INTO categories (id, category_name, transaction_type, active, code, created_at) VALUES
(gen_random_uuid(), 'Food & Dining', 'EXPENSE', true, NULL, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Rent & Housing', 'EXPENSE', true, NULL, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Transport', 'EXPENSE', true, NULL, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Healthcare', 'EXPENSE', true, NULL, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Entertainment', 'EXPENSE', true, NULL, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Utilities', 'EXPENSE', true, NULL, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Shopping', 'EXPENSE', true, NULL, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Education', 'EXPENSE', true, NULL, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Other Expense', 'EXPENSE', true, 'SYS_OTHER_EXPENSE',CURRENT_TIMESTAMP);

-- Insert Default Income Categories
INSERT INTO categories (id, category_name, transaction_type, active, code, created_at) VALUES
(gen_random_uuid(), 'Salary', 'INCOME', true, NULL, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Freelance', 'INCOME', true, NULL, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Business Revenue', 'INCOME', true, NULL, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Investment Return', 'INCOME', true, NULL, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Gift', 'INCOME', true, NULL, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Bonus', 'INCOME', true, NULL, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Rental Income', 'INCOME', true, NULL, CURRENT_TIMESTAMP),
(gen_random_uuid(), 'Other Income', 'INCOME', true, 'SYS_OTHER_INCOME', CURRENT_TIMESTAMP);