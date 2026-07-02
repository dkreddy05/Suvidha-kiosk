CREATE TABLE IF NOT EXISTS notification.notifications (
    notification_id UUID PRIMARY KEY,
    citizen_id VARCHAR(36) NOT NULL,
    phone_number TEXT NOT NULL,
    message_type VARCHAR(20) NOT NULL,
    message_content TEXT,
    status VARCHAR(20) NOT NULL,
    sent_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_notifications_citizen_id ON notification.notifications(citizen_id);
CREATE INDEX IF NOT EXISTS idx_notifications_status ON notification.notifications(status);
CREATE INDEX IF NOT EXISTS idx_notifications_sent_at ON notification.notifications(sent_at);
