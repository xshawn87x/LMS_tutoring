-- ============================================================
-- V20: 비밀번호 재설정 토큰
-- ============================================================
-- 로그인 전(비밀번호 분실) 단계이지만 org_code로 테넌트가 확정되므로 RLS 격리한다(app_user와 동일).
-- 로컬 환경엔 이메일 발송이 없어 요청 시 토큰을 응답으로 돌려준다(운영에선 이메일/문자로 전달).

CREATE TABLE password_reset_token (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id  UUID NOT NULL,
    email      VARCHAR(320) NOT NULL,
    token      VARCHAR(80) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used       BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_reset_token_tenant ON password_reset_token (tenant_id);
CREATE INDEX idx_reset_token_lookup ON password_reset_token (email, token);

ALTER TABLE password_reset_token ENABLE ROW LEVEL SECURITY;
ALTER TABLE password_reset_token FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON password_reset_token
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

GRANT SELECT, INSERT, UPDATE, DELETE ON password_reset_token TO lms_app;
