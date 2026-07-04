-- ============================================================
-- V11: 실제 로그인/회원가입 — 기관(tenant) 레지스트리 + 사용자 계정
-- ============================================================
-- dev 토큰을 대체하는 진짜 인증. 비밀번호는 bcrypt 해시로만 저장한다.
-- tenant 테이블은 로그인 전(테넌트 미상) 단계에서 org_code로 조회해야 하므로
-- RLS를 걸지 않는다(전역 레지스트리). 대신 app_user는 RLS로 테넌트 격리한다.

-- 기관 레지스트리: 로그인/회원가입 시 org_code로 테넌트를 찾는다.
CREATE TABLE tenant (
    id        UUID PRIMARY KEY,
    org_code  VARCHAR(64) NOT NULL UNIQUE,
    name      VARCHAR(255) NOT NULL
);

-- 기존 두 테넌트를 등록 (시드 과정과 동일한 UUID)
INSERT INTO tenant (id, org_code, name) VALUES
    ('11111111-1111-1111-1111-111111111111', 'acme', 'Acme 러닝'),
    ('22222222-2222-2222-2222-222222222222', 'globex', 'Globex 아카데미');

-- 사용자 계정 (테넌트 소속). 이메일은 테넌트 내에서 유일.
CREATE TABLE app_user (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL,
    email         VARCHAR(320) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name  VARCHAR(255),
    roles         VARCHAR(255) NOT NULL DEFAULT 'STUDENT',  -- 콤마 구분
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_app_user_email UNIQUE (tenant_id, email)
);
CREATE INDEX idx_app_user_tenant ON app_user (tenant_id);

ALTER TABLE app_user ENABLE ROW LEVEL SECURITY;
ALTER TABLE app_user FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON app_user
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

-- tenant는 로그인 전 조회용 → 읽기만 허용. app_user는 전체 CRUD.
GRANT SELECT ON tenant TO lms_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON app_user TO lms_app;
