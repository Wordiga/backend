ALTER TABLE members DROP COLUMN IF EXISTS password;
ALTER TABLE members ADD COLUMN IF NOT EXISTS profile_image VARCHAR(500);

ALTER TABLE plans ADD COLUMN IF NOT EXISTS estimated_budget_currency VARCHAR(3);
UPDATE plans
SET estimated_budget_currency = 'KRW'
WHERE estimated_budget_currency IS NULL
  AND (estimated_total_amount IS NOT NULL OR estimated_per_person_amount IS NOT NULL);

CREATE TABLE IF NOT EXISTS plan_budget_items
(
    id       BIGSERIAL   NOT NULL,
    plan_id  BIGINT      NOT NULL,
    category VARCHAR(50) NOT NULL,
    amount   BIGINT      NOT NULL,
    CONSTRAINT pk_plan_budget_items PRIMARY KEY (id),
    CONSTRAINT uk_plan_budget_items_plan_category UNIQUE (plan_id, category),
    CONSTRAINT fk_plan_budget_items_plan FOREIGN KEY (plan_id) REFERENCES plans (id) ON DELETE CASCADE
);

INSERT INTO plan_budget_items (plan_id, category, amount)
SELECT plan.id, item.key, (item.value #>> '{}')::BIGINT
FROM plans plan
CROSS JOIN LATERAL jsonb_each(
        CASE WHEN jsonb_typeof(plan.estimated_budget_breakdown::jsonb) = 'object'
             THEN plan.estimated_budget_breakdown::jsonb ELSE '{}'::jsonb END
     ) item
WHERE plan.estimated_budget_breakdown IS NOT NULL
  AND (item.value #>> '{}') ~ '^[0-9]+$'
ON CONFLICT (plan_id, category) DO NOTHING;

CREATE INDEX IF NOT EXISTS idx_plan_budget_items_plan ON plan_budget_items (plan_id);

ALTER TABLE plans DROP COLUMN IF EXISTS ai_response_json;
ALTER TABLE plans DROP COLUMN IF EXISTS estimated_budget_breakdown;
