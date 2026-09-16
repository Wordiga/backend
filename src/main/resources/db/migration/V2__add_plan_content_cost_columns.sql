ALTER TABLE plan_contents
    ADD COLUMN IF NOT EXISTS unit_amount BIGINT;
ALTER TABLE plan_contents
    ADD COLUMN IF NOT EXISTS cost_unit VARCHAR(20);
ALTER TABLE plan_contents
    ADD COLUMN IF NOT EXISTS cost_quantity INT;
ALTER TABLE plan_contents
    ADD COLUMN IF NOT EXISTS per_person_share BIGINT;
ALTER TABLE plan_contents
    ADD COLUMN IF NOT EXISTS cost_source VARCHAR(20);
