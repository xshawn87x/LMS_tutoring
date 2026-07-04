-- ============================================================
-- V9: 수료(과정 완료) 기록 = 수료증
-- ============================================================
-- 완료 조건(진도 100% + 모든 퀴즈 통과) 충족 시 1건 발급. 학생당 과정당 1건(유니크).

CREATE TABLE course_completion (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id      UUID NOT NULL,
    course_id      UUID NOT NULL REFERENCES course (id) ON DELETE CASCADE,
    student_id     VARCHAR(255) NOT NULL,
    certificate_no VARCHAR(40) NOT NULL,
    issued_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_course_completion UNIQUE (tenant_id, course_id, student_id)
);
CREATE INDEX idx_course_completion_tenant ON course_completion (tenant_id);
CREATE INDEX idx_course_completion_student ON course_completion (tenant_id, student_id);

ALTER TABLE course_completion ENABLE ROW LEVEL SECURITY;
ALTER TABLE course_completion FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON course_completion
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

GRANT SELECT, INSERT, UPDATE, DELETE ON course_completion TO lms_app;
