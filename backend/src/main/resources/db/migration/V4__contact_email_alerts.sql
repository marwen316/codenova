ALTER TABLE contact_message ADD COLUMN email_status varchar(16) NOT NULL DEFAULT 'NONE';
ALTER TABLE contact_message ADD COLUMN email_attempts integer NOT NULL DEFAULT 0;
ALTER TABLE contact_message ADD COLUMN email_next_attempt timestamp with time zone;
CREATE INDEX contact_message_pending_email ON contact_message (email_next_attempt) WHERE email_status = 'PENDING';
