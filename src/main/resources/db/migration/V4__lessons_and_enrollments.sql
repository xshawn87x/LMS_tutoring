-- ============================================================
-- V4: 레슨(lesson) + 수강신청(enrollment) — 같은 RLS 격리 패턴 적용
-- ============================================================

-- 레슨: 과정에 속한 학습 콘텐츠
CREATE TABLE lesson (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL,
    course_id   UUID NOT NULL REFERENCES course (id) ON DELETE CASCADE,
    title       VARCHAR(255) NOT NULL,
    content     TEXT,
    order_no    INT NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_lesson_tenant_id ON lesson (tenant_id);
CREATE INDEX idx_lesson_course_id ON lesson (course_id);

-- 수강신청: 학생(JWT subject)이 과정을 수강. 진도(progress)와 상태(status) 추적.
CREATE TABLE enrollment (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL,
    course_id   UUID NOT NULL REFERENCES course (id) ON DELETE CASCADE,
    student_id  VARCHAR(255) NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    progress    INT NOT NULL DEFAULT 0 CHECK (progress BETWEEN 0 AND 100),
    enrolled_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- 같은 테넌트 안에서 한 학생이 한 과정을 중복 수강하지 못하게
    CONSTRAINT uq_enrollment UNIQUE (tenant_id, course_id, student_id)
);
CREATE INDEX idx_enrollment_tenant_id ON enrollment (tenant_id);
CREATE INDEX idx_enrollment_student ON enrollment (tenant_id, student_id);

-- ------------------------------------------------------------
-- RLS: course와 동일하게 app.current_tenant 기준 행 격리 (fail-closed)
-- ------------------------------------------------------------
ALTER TABLE lesson ENABLE ROW LEVEL SECURITY;
ALTER TABLE lesson FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON lesson
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

ALTER TABLE enrollment ENABLE ROW LEVEL SECURITY;
ALTER TABLE enrollment FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON enrollment
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

-- 앱 롤(lms_app) 권한 부여
GRANT SELECT, INSERT, UPDATE, DELETE ON lesson TO lms_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON enrollment TO lms_app;
