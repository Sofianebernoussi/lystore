ALTER TABLE lystore.project
  ADD COLUMN preference bigint;

ALTER TABLE lystore.campaign
  ADD COLUMN priority_enabled boolean DEFAULT true ;