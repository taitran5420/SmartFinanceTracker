-- Create Enum for notification types
CREATE TYPE notification_type_enum AS ENUM ('OVERDRAFT_ALERT', 'BUDGET_WARNING', 'SYSTEM_UPDATE', 'RECURRING_INFO', 'TRANSACTION_SUCCESS');

-- Create notifications table for In-App storage
CREATE TABLE notifications
(
   id UUID PRIMARY KEY,
   user_id UUID NOT NULL,
   title VARCHAR(255) NOT NULL,
   message TEXT NOT NULL,
   notification_type notification_type_enum NOT NULL,
   is_read BOOLEAN NOT NULL DEFAULT false,
   created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP WITH TIME ZONE,
   CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- Index to fetch unread notifications for a user
CREATE INDEX idx_notifications_user_unread ON notifications (user_id, is_read);