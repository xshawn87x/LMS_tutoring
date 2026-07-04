-- ============================================================
-- V17: 공지사항 (학원 공지 + 강의 공지)
-- ============================================================
-- scope=ACADEMY: 학원(테넌트) 전체 공지 (course_id NULL)
-- scope=COURSE : 특정 강의 공지 (course_id 지정, 과정 삭제 시 CASCADE)
-- 작성: INSTRUCTOR/ADMIN, 조회: 테넌트 내 인증 사용자 누구나. RLS로 테넌트 격리.

CREATE TABLE notice (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id  UUID NOT NULL,
    scope      VARCHAR(10) NOT NULL,
    course_id  UUID,
    title      VARCHAR(255) NOT NULL,
    body       TEXT,
    author     VARCHAR(320),
    pinned     BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ,
    CONSTRAINT fk_notice_course FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE
);
CREATE INDEX idx_notice_tenant ON notice (tenant_id);
CREATE INDEX idx_notice_course ON notice (course_id);

ALTER TABLE notice ENABLE ROW LEVEL SECURITY;
ALTER TABLE notice FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON notice
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

GRANT SELECT, INSERT, UPDATE, DELETE ON notice TO lms_app;
