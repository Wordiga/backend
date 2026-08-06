-- ========================
-- members (회원)
-- ========================
CREATE TABLE members
(
    id            BIGSERIAL    NOT NULL,
    email         VARCHAR(100) NOT NULL,
    nickname      VARCHAR(50)  NULL,
    provider      VARCHAR(20)  NOT NULL,
    provider_id   VARCHAR(100) NULL,
    profile_image VARCHAR(500) NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE members
    ADD CONSTRAINT pk_members PRIMARY KEY (id);

CREATE INDEX idx_members_provider ON members (provider, provider_id);

-- ========================
-- tourism_content_snapshots (한국관광공사 콘텐츠 원본 스냅샷 캐시)
-- ========================
CREATE TABLE tourism_content_snapshots
(
    content_id        VARCHAR(50)     NOT NULL,
    content_type_id   VARCHAR(20)     NULL,
    title             VARCHAR(255)    NOT NULL,
    firstimage        VARCHAR(500)    NULL,
    addr1             VARCHAR(255)    NULL,
    mapx              NUMERIC(15, 10) NULL,
    mapy              NUMERIC(15, 10) NULL,
    sigungu_code      VARCHAR(10)     NULL,
    sigungu_name      VARCHAR(50)     NULL,
    lcls_system1_code VARCHAR(20)     NULL,
    lcls_system2_code VARCHAR(20)     NULL,
    lcls_system3_code VARCHAR(20)     NULL,
    updated_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_tourism_content_snapshots PRIMARY KEY (content_id)
);

-- ========================
-- wishes (위시리스트)
-- ========================
CREATE TABLE wishes
(
    id          BIGSERIAL   NOT NULL,
    member_id   BIGINT      NOT NULL,
    content_id  VARCHAR(50) NOT NULL,
    folder_name VARCHAR(50) NOT NULL DEFAULT '기본 위시리스트',
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE wishes
    ADD CONSTRAINT pk_wishes PRIMARY KEY (id);

ALTER TABLE wishes
    ADD CONSTRAINT fk_wishes_member
        FOREIGN KEY (member_id) REFERENCES members (id) ON DELETE CASCADE;

ALTER TABLE wishes
    ADD CONSTRAINT fk_wishes_content
        FOREIGN KEY (content_id) REFERENCES tourism_content_snapshots (content_id) ON DELETE CASCADE;

CREATE UNIQUE INDEX idx_wishes_member_content ON wishes (member_id, content_id);
CREATE INDEX idx_wishes_member ON wishes (member_id);
CREATE INDEX idx_wishes_folder ON wishes (member_id, folder_name);

-- ========================
-- plans (여행 플랜)
-- ========================
CREATE TABLE plans
(
    id                          BIGSERIAL    NOT NULL,
    member_id                   BIGINT       NOT NULL,
    title                       VARCHAR(255) NOT NULL,
    start_date                  DATE         NOT NULL,
    end_date                    DATE         NOT NULL,
    participant_count           INT          NOT NULL DEFAULT 1,
    plan_type                   VARCHAR(50)  NULL,
    schedule_id                 VARCHAR(100) NULL,
    estimated_total_amount      BIGINT       NULL,
    estimated_per_person_amount BIGINT       NULL,
    created_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE plans
    ADD CONSTRAINT pk_plans PRIMARY KEY (id);

ALTER TABLE plans
    ADD CONSTRAINT fk_plans_member
        FOREIGN KEY (member_id) REFERENCES members (id) ON DELETE CASCADE;

CREATE INDEX idx_plans_member ON plans (member_id);

-- ========================
-- plan_contents (플랜 상세 일정 콘텐츠)
-- ========================
CREATE TABLE plan_contents
(
    id                     BIGSERIAL   NOT NULL,
    plan_id                BIGINT      NOT NULL,
    sequence               INT         NOT NULL,
    day_number             INT         NOT NULL,
    content_id             VARCHAR(50) NOT NULL,
    scheduled_time         TIMESTAMP   NULL,
    duration               INT         NULL,
    start_time             TIME        NULL,
    end_time               TIME        NULL,
    travel_time_minutes    INT         NULL,
    travel_distance_meters INT         NULL,
    estimated_cost         BIGINT      NULL
);

ALTER TABLE plan_contents
    ADD CONSTRAINT pk_plan_contents PRIMARY KEY (id);

ALTER TABLE plan_contents
    ADD CONSTRAINT fk_plan_contents_plan
        FOREIGN KEY (plan_id) REFERENCES plans (id) ON DELETE CASCADE;

ALTER TABLE plan_contents
    ADD CONSTRAINT fk_plan_contents_content
        FOREIGN KEY (content_id) REFERENCES tourism_content_snapshots (content_id);

CREATE INDEX idx_plan_contents_plan ON plan_contents (plan_id);
CREATE INDEX idx_plan_contents_sequence ON plan_contents (plan_id, sequence);

-- ========================
-- plan_budget_breakdowns (AI 예산 카테고리별 상세 내역)
-- ========================
CREATE TABLE plan_budget_breakdowns
(
    id       BIGSERIAL   NOT NULL,
    plan_id  BIGINT      NOT NULL,
    category VARCHAR(50) NOT NULL,
    amount   BIGINT      NOT NULL,
    CONSTRAINT pk_plan_budget_breakdowns PRIMARY KEY (id),
    CONSTRAINT uk_plan_budget_breakdowns_plan_category UNIQUE (plan_id, category),
    CONSTRAINT fk_plan_budget_breakdowns_plan FOREIGN KEY (plan_id) REFERENCES plans (id) ON DELETE CASCADE
);

CREATE INDEX idx_plan_budget_breakdowns_plan ON plan_budget_breakdowns (plan_id);

-- ========================
-- proposals (AI 제안서 메타데이터)
-- ========================
CREATE TABLE proposals
(
    id         BIGSERIAL    NOT NULL,
    plan_id    BIGINT       NOT NULL,
    s3_key     VARCHAR(500) NOT NULL,
    file_name  VARCHAR(255) NOT NULL,
    file_size  BIGINT       NOT NULL,
    expires_at TIMESTAMP    NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_proposals PRIMARY KEY (id),
    CONSTRAINT uk_proposals_s3_key UNIQUE (s3_key),
    CONSTRAINT fk_proposals_plan FOREIGN KEY (plan_id) REFERENCES plans (id) ON DELETE CASCADE
);

CREATE INDEX idx_proposals_plan_created ON proposals (plan_id, created_at DESC, id DESC);