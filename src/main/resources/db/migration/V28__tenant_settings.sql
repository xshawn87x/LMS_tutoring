-- ============================================================
-- V28: 학원 환경설정(브랜딩) — 로고·색상·연락처·약관
-- ============================================================
-- 테넌트당 1행. 관리자가 수정, 테넌트 내 누구나 조회(브랜딩 표시). RLS로 격리.

CREATE TABLE tenant_settings (
    tenant_id     UUID PRIMARY KEY,
    display_name  VARCHAR(255),
    logo_url      VARCHAR(500),
    primary_color VARCHAR(20),
    contact       VARCHAR(255),
    terms         TEXT,
    updated_at    TIMESTAMPTZ
);

ALTER TABLE tenant_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenant_settings FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON tenant_settings
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

GRANT SELECT, INSERT, UPDATE, DELETE ON tenant_settings TO lms_app;
