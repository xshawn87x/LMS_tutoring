-- ============================================================
-- V15: 감사 로그(audit_log) + 가격 편집 권한
-- ============================================================
-- 감사 로그: 플랫폼(슈퍼관리자)의 요금제/자격/가격/인보이스 변경을 누가·언제·무엇을 했는지 기록.
-- 과금 분쟁·추적에 필수. RLS 없는 전역 테이블(플랫폼 전역 행위 기록).

CREATE TABLE audit_log (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor       VARCHAR(320) NOT NULL,        -- 행위자(슈퍼관리자 이메일 등), 없으면 'system'
    action      VARCHAR(60) NOT NULL,         -- 예: PLAN_CHANGE, ENTITLEMENT_GRANT, INVOICE_PAY, PRICE_UPDATE
    target_type VARCHAR(40),                  -- 예: TENANT, INVOICE, PLAN_PRICE
    target_id   VARCHAR(120),                 -- 대상 식별자(테넌트 id, 기능명 등)
    detail      TEXT,                         -- 사람이 읽는 변경 설명
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_log_created ON audit_log (created_at DESC);
CREATE INDEX idx_audit_log_target ON audit_log (target_type, target_id);

GRANT SELECT, INSERT ON audit_log TO lms_app;

-- 가격 편집: 슈퍼관리자가 콘솔에서 요금제/애드온 가격을 수정할 수 있게 UPDATE 권한 부여.
GRANT UPDATE ON plan_price TO lms_app;
GRANT UPDATE ON feature_addon_price TO lms_app;
