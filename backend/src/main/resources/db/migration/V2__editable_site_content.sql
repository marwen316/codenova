ALTER TABLE site_content ADD COLUMN settings JSONB NOT NULL DEFAULT '{}'::jsonb;
