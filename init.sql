-- ========================
-- members
-- ========================

CREATE TABLE members
(
    id          BIGSERIAL    NOT NULL,
    email       VARCHAR(100) NOT NULL,
    nickname    VARCHAR(50)  NULL,
    password    VARCHAR(255) NULL,
    provider    VARCHAR(20)  NOT NULL,
    provider_id VARCHAR(100) NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE members
    ADD CONSTRAINT pk_members PRIMARY KEY (id);

CREATE UNIQUE INDEX idx_members_email ON members (email);
CREATE INDEX idx_members_provider ON members (provider, provider_id);

-- ========================
-- cart_contents
-- ========================

CREATE TABLE cart_contents
(
    id              BIGSERIAL    NOT NULL,
    content_id      VARCHAR(50)  NOT NULL,
    member_id       BIGINT       NOT NULL,
    content_type_id VARCHAR(20)  NULL,
    thumbnail       VARCHAR(500) NULL,
    title           VARCHAR(255) NOT NULL,
    zipcode         VARCHAR(10)  NULL,
    inserted_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP    NULL
);

ALTER TABLE cart_contents
    ADD CONSTRAINT pk_cart_contents PRIMARY KEY (id);

ALTER TABLE cart_contents
    ADD CONSTRAINT fk_cart_contents_member
        FOREIGN KEY (member_id) REFERENCES members (id) ON DELETE CASCADE;

-- ALTER TABLE cart_contents
--     ADD CONSTRAINT fk_cart_contents_content_type
--         FOREIGN KEY (content_type_id) REFERENCES content_types(content_type_id) ON DELETE SET NULL;

CREATE INDEX idx_cart_member ON cart_contents (member_id);
CREATE INDEX idx_cart_member_deleted ON cart_contents (member_id, deleted_at);
-- CREATE INDEX idx_cart_content_type ON cart_contents(content_type_id);

-- ========================
-- plans
-- ========================

CREATE TABLE plans
(
    id                BIGSERIAL    NOT NULL,
    member_id         BIGINT       NOT NULL,
    title             VARCHAR(255) NOT NULL,
    start_date        DATE         NULL,
    end_date          DATE         NULL,
    participant_count INT          NOT NULL DEFAULT 1,
    plan_type         VARCHAR(50)  NULL,
    schedule_id       VARCHAR(100) NULL,
    ai_response_json  TEXT         NULL,
    estimated_total_amount BIGINT  NULL,
    estimated_per_person_amount BIGINT NULL,
    ai_warnings       VARCHAR(2000) NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE plans
    ADD CONSTRAINT pk_plans PRIMARY KEY (id);

ALTER TABLE plans
    ADD CONSTRAINT fk_plans_member
        FOREIGN KEY (member_id) REFERENCES members (id) ON DELETE CASCADE;

CREATE INDEX idx_plans_member ON plans (member_id);
CREATE INDEX idx_plans_date ON plans (start_date, end_date);

-- ========================
-- plan_contents
-- ========================

CREATE TABLE plan_contents
(
    id              BIGSERIAL    NOT NULL,
    plan_id         BIGINT       NOT NULL,
    sequence        INT          NOT NULL,
    day_number      INT          NOT NULL,
    plan_date       DATE         NOT NULL,
    content_id      VARCHAR(50)  NOT NULL,
    content_title   VARCHAR(255) NOT NULL,
    content_type_id VARCHAR(20)  NULL,
    scheduled_time  TIMESTAMP    NULL,
    duration        INT          NULL,
    addr1           VARCHAR(255) NULL,
    thumbnail_url   VARCHAR(500) NULL,
    mapx            NUMERIC(15, 10) NULL,
    mapy            NUMERIC(15, 10) NULL,
    start_time      TIME         NULL,
    end_time        TIME         NULL,
    travel_time_minutes INT      NULL,
    travel_distance_meters INT   NULL,
    estimated_cost  BIGINT       NULL,
    memo            VARCHAR(255) NULL
);

ALTER TABLE plan_contents
    ADD CONSTRAINT pk_plan_contents PRIMARY KEY (id);

ALTER TABLE plan_contents
    ADD CONSTRAINT fk_plan_contents_plan
        FOREIGN KEY (plan_id) REFERENCES plans (id) ON DELETE CASCADE;

-- ALTER TABLE plan_contents
--     ADD CONSTRAINT fk_plan_contents_content_type
--         FOREIGN KEY (content_type_id) REFERENCES content_types(content_type_id) ON DELETE SET NULL;

CREATE INDEX idx_plan_contents_plan ON plan_contents (plan_id);
CREATE INDEX idx_plan_contents_sequence ON plan_contents (plan_id, sequence);
CREATE INDEX idx_plan_contents_day_sequence ON plan_contents (plan_id, plan_date, sequence);

-- ========================
-- proposals
-- ========================

CREATE TABLE proposals
(
    id          BIGSERIAL    NOT NULL,
    plan_id     BIGINT       NOT NULL,
    s3_key      VARCHAR(500) NOT NULL,
    file_name   VARCHAR(255) NOT NULL,
    file_size   BIGINT       NOT NULL,
    expires_at  TIMESTAMP    NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_proposals PRIMARY KEY (id),
    CONSTRAINT uk_proposals_s3_key UNIQUE (s3_key),
    CONSTRAINT fk_proposals_plan FOREIGN KEY (plan_id) REFERENCES plans (id) ON DELETE CASCADE
);

CREATE INDEX idx_proposals_plan_created ON proposals (plan_id, created_at DESC, id DESC);


-- ========================
-- content_types
-- ========================

CREATE TABLE content_types
(
    content_type_id   VARCHAR(20)  NOT NULL,
    lcls_system1_code VARCHAR(20)  NULL,
    lcls_system1_name VARCHAR(100) NULL,
    lcls_system2_code VARCHAR(20)  NULL,
    lcls_system2_name VARCHAR(100) NULL,
    lcls_system3_code VARCHAR(20)  NULL,
    lcls_system3_name VARCHAR(100) NULL
);

ALTER TABLE content_types
    ADD CONSTRAINT pk_content_types PRIMARY KEY (content_type_id);

-- ========================
-- wishes (위시리스트)
-- ========================

CREATE TABLE wishes
(
    id              BIGSERIAL    NOT NULL,
    member_id       BIGINT       NOT NULL,
    content_id      VARCHAR(50)  NOT NULL,
    content_type_id VARCHAR(20)  NULL,
    title           VARCHAR(255) NOT NULL,
    firstimage      VARCHAR(500) NULL,
    addr1           VARCHAR(255) NULL,
    mapx            NUMERIC(15, 10) NULL,
    mapy            NUMERIC(15, 10) NULL,
    sigungu_code    VARCHAR(10)  NULL,
    sigungu_name    VARCHAR(50)  NULL,
    folder_name     VARCHAR(50)  NOT NULL DEFAULT '기본 위시리스트',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE wishes
    ADD CONSTRAINT pk_wishes PRIMARY KEY (id);

ALTER TABLE wishes
    ADD CONSTRAINT fk_wishes_member
        FOREIGN KEY (member_id) REFERENCES members (id) ON DELETE CASCADE;

-- 동일 회원이 동일 콘텐츠 중복 등록 방지
CREATE UNIQUE INDEX idx_wishes_member_content ON wishes (member_id, content_id);

CREATE INDEX idx_wishes_member ON wishes (member_id);
CREATE INDEX idx_wishes_folder ON wishes (member_id, folder_name);
