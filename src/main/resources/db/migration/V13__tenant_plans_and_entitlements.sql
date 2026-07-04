-- ============================================================
-- V13: SaaS 요금제(Plan) + 기능 자격(Entitlement) — 기능 플래그를 2계층으로 분리
-- ============================================================
-- 지금까지 기능 on/off는 기관 ADMIN이 tenant_feature로 자유롭게 바꿨다(1계층).
-- 이제 두 결정을 분리한다:
--   ① 자격(Entitlement): "이 기관이 이 모듈을 쓸 수 있나?" — 플랫폼(슈퍼관리자)이 요금제로 부여.
--   ② 활성화(Activation): "쓸 수 있는 것 중 실제로 켤까?" — 기관 ADMIN이 tenant_feature로 결정.
-- 유효 활성 = 자격 있음 AND 기관이 켬.
--
-- tenant/entitlement는 플랫폼이 테넌트 경계를 "넘어" 관리하므로 RLS 없는 전역 테이블이다
-- (tenant 레지스트리와 동일한 성격). 앱은 tenant_id로 명시적으로 스코프한다.

-- 기관의 현재 요금제. 기존 두 기관은 현행 동작을 그대로 보존하기 위해 전체 자격(PRO)로 시작.
ALTER TABLE tenant ADD COLUMN plan VARCHAR(20) NOT NULL DEFAULT 'FREE';
UPDATE tenant SET plan = 'PRO';

-- 기능 자격: (tenant, feature) 행이 존재하면 그 기능을 "쓸 수 있다".
-- source = PLAN(요금제로 부여) | ADDON(요금제 밖 개별 부여). 요금제 변경 시 PLAN 행만 교체하고
-- ADDON 행은 보존한다 → 하이브리드(플랜 + 애드온)를 재설계 없이 지원.
CREATE TABLE tenant_entitlement (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL,
    feature     VARCHAR(40) NOT NULL,
    source      VARCHAR(10) NOT NULL DEFAULT 'PLAN',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_tenant_entitlement UNIQUE (tenant_id, feature)
);
CREATE INDEX idx_tenant_entitlement_tenant ON tenant_entitlement (tenant_id);

-- 기존 두 테넌트에 PRO = 전체 기능 자격 부여 (현행 동작 보존)
INSERT INTO tenant_entitlement (tenant_id, feature, source)
SELECT t.id, f.feature, 'PLAN'
FROM tenant t
CROSS JOIN (VALUES
    ('LESSONS'), ('ENROLLMENTS'), ('QUIZZES'), ('DIAGNOSIS'),
    ('RECOMMENDATIONS'), ('AI_CURATION'), ('CERTIFICATES')
) AS f(feature);

-- 앱 런타임 롤(lms_app) 권한: entitlement 전체 CRUD, tenant는 요금제(plan) 변경을 위해 UPDATE 추가.
-- (두 테이블 모두 RLS 없음 — 플랫폼이 테넌트 경계를 넘어 관리)
GRANT SELECT, INSERT, UPDATE, DELETE ON tenant_entitlement TO lms_app;
GRANT UPDATE ON tenant TO lms_app;
