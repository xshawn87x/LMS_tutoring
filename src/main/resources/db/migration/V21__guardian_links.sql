-- ============================================================
-- V21: 학부모-자녀 연결 (guardian_link)
-- ============================================================
-- 학부모(parent_subject=이메일)와 자녀(student_subject=이메일)를 잇는다. 학원 관리자가 연결한다.
-- 학부모는 연결된 자녀의 학습 현황을 읽기 전용으로 본다. RLS로 테넌트 격리.

CREATE TABLE guardian_link (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    parent_subject  VARCHAR(320) NOT NULL,
    student_subject VARCHAR(320) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_guardian UNIQUE (tenant_id, parent_subject, student_subject)
);
CREATE INDEX idx_guardian_tenant ON guardian_link (tenant_id);
CREATE INDEX idx_guardian_parent ON guardian_link (parent_subject);

ALTER TABLE guardian_link ENABLE ROW LEVEL SECURITY;
ALTER TABLE guardian_link FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON guardian_link
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

GRANT SELECT, INSERT, UPDATE, DELETE ON guardian_link TO lms_app;
