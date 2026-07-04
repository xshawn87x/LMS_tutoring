-- ============================================================
-- V26: 수강료 결제 (학원 ↔ 수강생) — 플랫폼 SaaS 과금과 별개
-- ============================================================
-- course.tuition_fee: 강의 수강료(0=무료).
-- student_payment: 수강생의 수강료 결제 내역(PG 연동은 스텁, 실PG는 provider 교체).

ALTER TABLE course ADD COLUMN tuition_fee INT NOT NULL DEFAULT 0;

CREATE TABLE student_payment (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    student_subject VARCHAR(320) NOT NULL,
    course_id       UUID NOT NULL,
    amount          INT NOT NULL,
    status          VARCHAR(12) NOT NULL DEFAULT 'PAID',   -- PENDING | PAID | REFUNDED
    method          VARCHAR(20),
    payment_ref     VARCHAR(100),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    refunded_at     TIMESTAMPTZ,
    CONSTRAINT fk_payment_course FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE
);
CREATE INDEX idx_spay_tenant ON student_payment (tenant_id);
CREATE INDEX idx_spay_student ON student_payment (student_subject);

ALTER TABLE student_payment ENABLE ROW LEVEL SECURITY;
ALTER TABLE student_payment FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON student_payment
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

GRANT SELECT, INSERT, UPDATE, DELETE ON student_payment TO lms_app;
