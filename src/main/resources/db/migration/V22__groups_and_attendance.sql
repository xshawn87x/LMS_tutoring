-- ============================================================
-- V22: 반/과정(기수) + 출석
-- ============================================================
-- student_group(반/기수) 1:N group_member(반 소속 학생). attendance(출석)는 반+학생+날짜 단위.
-- 모두 RLS로 테넌트 격리. 관리는 INSTRUCTOR/ADMIN(컨트롤러), 조회는 소속/학부모 등.

CREATE TABLE student_group (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id  UUID NOT NULL,
    name       VARCHAR(255) NOT NULL,
    term       VARCHAR(100),
    start_date DATE,
    end_date   DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_group_tenant ON student_group (tenant_id);

CREATE TABLE group_member (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    group_id        UUID NOT NULL,
    student_subject VARCHAR(320) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_member_group FOREIGN KEY (group_id) REFERENCES student_group(id) ON DELETE CASCADE,
    CONSTRAINT uq_group_member UNIQUE (group_id, student_subject)
);
CREATE INDEX idx_member_tenant ON group_member (tenant_id);
CREATE INDEX idx_member_group ON group_member (group_id);

CREATE TABLE attendance (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    group_id        UUID NOT NULL,
    student_subject VARCHAR(320) NOT NULL,
    att_date        DATE NOT NULL,
    status          VARCHAR(10) NOT NULL,   -- PRESENT | ABSENT | LATE | EXCUSED
    note            VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_att_group FOREIGN KEY (group_id) REFERENCES student_group(id) ON DELETE CASCADE,
    CONSTRAINT uq_attendance UNIQUE (group_id, student_subject, att_date)
);
CREATE INDEX idx_att_tenant ON attendance (tenant_id);
CREATE INDEX idx_att_group_date ON attendance (group_id, att_date);
CREATE INDEX idx_att_student ON attendance (student_subject);

ALTER TABLE student_group ENABLE ROW LEVEL SECURITY;
ALTER TABLE student_group FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON student_group
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

ALTER TABLE group_member ENABLE ROW LEVEL SECURITY;
ALTER TABLE group_member FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON group_member
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

ALTER TABLE attendance ENABLE ROW LEVEL SECURITY;
ALTER TABLE attendance FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON attendance
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

GRANT SELECT, INSERT, UPDATE, DELETE ON student_group TO lms_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON group_member TO lms_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON attendance TO lms_app;
