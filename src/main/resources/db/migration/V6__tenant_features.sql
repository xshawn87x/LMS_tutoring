-- ============================================================
-- V6: 테넌트별 기능 플래그 (모듈러 모놀리식 — 기관별 모듈 선택 활성화)
-- ============================================================
-- 행이 없으면 Feature enum의 기본값을 따른다. 행이 있으면 그 값으로 override.
-- 테넌트 관리자가 자기 기관의 기능만 켜고/끌 수 있다 (RLS로 격리).

CREATE TABLE tenant_feature (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL,
    feature     VARCHAR(40) NOT NULL,
    enabled     BOOLEAN NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_tenant_feature UNIQUE (tenant_id, feature)
);
CREATE INDEX idx_tenant_feature_tenant_id ON tenant_feature (tenant_id);

ALTER TABLE tenant_feature ENABLE ROW LEVEL SECURITY;
ALTER TABLE tenant_feature FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON tenant_feature
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

GRANT SELECT, INSERT, UPDATE, DELETE ON tenant_feature TO lms_app;
