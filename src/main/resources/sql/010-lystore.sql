ALTER TABLE lystore.campaign
ADD COLUMN purse_enabled boolean;

UPDATE lystore.campaign SET purse_enabled = true;

ALTER TABLE lystore.campaign
ALTER COLUMN purse_enabled SET DEFAULT FALSE;