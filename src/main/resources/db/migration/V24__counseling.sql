-- ============================================================
-- V24: 상담 관리 (상담기록 + 상담예약)
-- ============================================================
-- counseling_record: 강사/관리자가 학생 상담 내용을 기록.
-- counseling_appointment: 학생/학부모가 상담을 신청 → 관리자가 확정/취소.
-- 모두 RLS로 테넌트 격리.

CREATE TABLE counseling_record (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    student_subject VARCHAR(320) NOT NULL,
    counselor       VARCHAR(320) NOT NULL,
    content         TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_counsel_tenant ON counseling_record (tenant_id);
CREATE INDEX idx_counsel_student ON counseling_record (student_subject);

CREATE TABLE counseling_appointment (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    student_subject VARCHAR(320) NOT NULL,
    requested_by    VARCHAR(320) NOT NULL,
    preferred_at    TIMESTAMPTZ,
    status          VARCHAR(12) NOT NULL DEFAULT 'REQUESTED',  -- REQUESTED | CONFIRMED | CANCELLED
    memo            VARCHAR(500),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_appt_tenant ON counseling_appointment (tenant_id);
CREATE INDEX idx_appt_requester ON counseling_appointment (requested_by);

ALTER TABLE counseling_record ENABLE ROW LEVEL SECURITY;
ALTER TABLE counseling_record FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON counseling_record
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

ALTER TABLE counseling_appointment ENABLE ROW LEVEL SECURITY;
ALTER TABLE counseling_appointment FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON counseling_appointment
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

GRANT SELECT, INSERT, UPDATE, DELETE ON counseling_record TO lms_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON counseling_appointment TO lms_app;
