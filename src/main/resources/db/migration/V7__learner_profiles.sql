-- ============================================================
-- V7: 학습자 프로필(역량·관심분야) + 과정 분야/난이도 (규칙 기반 추천 기반)
-- ============================================================

-- 관심분야 카탈로그: 전역 공유 참조 테이블 (테넌트 무관, RLS 없음)
CREATE TABLE interest_category (
    code       VARCHAR(40) PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0
);
INSERT INTO interest_category (code, name, sort_order) VALUES
    ('BACKEND',  '백엔드',        1),
    ('FRONTEND', '프론트엔드',     2),
    ('DATA',     '데이터/AI',     3),
    ('MOBILE',   '모바일',        4),
    ('DEVOPS',   'DevOps/인프라', 5),
    ('DESIGN',   '디자인',        6);
GRANT SELECT ON interest_category TO lms_app;

-- 과정에 분야·난이도 (강사가 지정; Phase 2에서 AI가 자동 채움). level: 0 입문 ~ 3 고급
ALTER TABLE course ADD COLUMN category_code VARCHAR(40);
ALTER TABLE course ADD COLUMN level INT;

-- 기존 시드 과정 분류 (추천이 바로 동작하도록)
UPDATE course SET category_code = 'BACKEND', level = 0 WHERE title = 'Spring 입문';
UPDATE course SET category_code = 'BACKEND', level = 1 WHERE title = 'JPA 기초';
UPDATE course SET category_code = 'DATA',    level = 0 WHERE title = 'Python 입문';

-- 학습자 프로필 / 관심분야 / 역량 (테넌트 + 학생 단위, RLS 격리)
CREATE TABLE learner_profile (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id  UUID NOT NULL,
    student_id VARCHAR(255) NOT NULL,
    onboarded  BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_learner_profile UNIQUE (tenant_id, student_id)
);

CREATE TABLE learner_interest (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL,
    student_id    VARCHAR(255) NOT NULL,
    category_code VARCHAR(40) NOT NULL,
    CONSTRAINT uq_learner_interest UNIQUE (tenant_id, student_id, category_code)
);

CREATE TABLE learner_skill (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL,
    student_id    VARCHAR(255) NOT NULL,
    category_code VARCHAR(40) NOT NULL,
    level         INT NOT NULL,
    CONSTRAINT uq_learner_skill UNIQUE (tenant_id, student_id, category_code)
);

CREATE INDEX idx_learner_profile_student ON learner_profile (tenant_id, student_id);
CREATE INDEX idx_learner_interest_student ON learner_interest (tenant_id, student_id);
CREATE INDEX idx_learner_skill_student ON learner_skill (tenant_id, student_id);

-- RLS: 세 테이블 모두 app.current_tenant 기준 격리 (fail-closed)
DO $$
DECLARE t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['learner_profile', 'learner_interest', 'learner_skill'] LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', t);
        EXECUTE format(
            'CREATE POLICY tenant_isolation ON %I '
            || 'USING (tenant_id = NULLIF(current_setting(''app.current_tenant'', true), '''')::uuid) '
            || 'WITH CHECK (tenant_id = NULLIF(current_setting(''app.current_tenant'', true), '''')::uuid)', t);
        EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON %I TO lms_app', t);
    END LOOP;
END
$$;
