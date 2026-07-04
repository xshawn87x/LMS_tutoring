-- ============================================================
-- V14: 가격(요금제·애드온) + 사용량 계량 + 청구(인보이스)
-- ============================================================
-- 2계층 자격(V13) 위에 "돈"을 얹는다:
--   - plan_price          : 요금제 월정액
--   - feature_addon_price : 애드온으로 개별 판매하는 기능의 가격(정액 또는 사용량 기반)
--   - usage_counter       : 사용량 과금 기능(AI 분석 등)의 월별 사용량 집계
--   - invoice             : 특정 월의 청구 스냅샷(요금제+애드온+사용량) + 결제 상태
-- 모두 플랫폼이 테넌트 경계를 넘어 다루므로 RLS 없는 전역 테이블(tenant_id 명시 스코프).

-- 요금제 월정액 (KRW)
CREATE TABLE plan_price (
    plan          VARCHAR(20) PRIMARY KEY,
    monthly_price INT NOT NULL,
    currency      VARCHAR(3) NOT NULL DEFAULT 'KRW'
);
INSERT INTO plan_price (plan, monthly_price) VALUES
    ('FREE', 0), ('STANDARD', 49000), ('PRO', 99000);

-- 애드온(개별 기능) 가격. pricing_type = FLAT(월정액) | USAGE(단위당 + 월 무료 포함량).
-- 여기 등록된 기능만 "판매 가능한 애드온". 코어 학습기능은 미등록(플랜 티어로만 차등).
CREATE TABLE feature_addon_price (
    feature        VARCHAR(40) PRIMARY KEY,
    pricing_type   VARCHAR(10) NOT NULL,
    monthly_price  INT NOT NULL DEFAULT 0,
    unit_price     INT NOT NULL DEFAULT 0,
    included_units INT NOT NULL DEFAULT 0,
    unit_label     VARCHAR(40),
    currency       VARCHAR(3) NOT NULL DEFAULT 'KRW'
);
INSERT INTO feature_addon_price (feature, pricing_type, monthly_price, unit_price, included_units, unit_label) VALUES
    ('CERTIFICATES', 'FLAT',  20000, 0,   0,  NULL),
    ('AI_CURATION',  'USAGE', 0,     500, 50, '분석');

-- 사용량 계량 (월별). 사용량 과금 기능(AI_CURATION 등)이 호출될 때 증가.
CREATE TABLE usage_counter (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id  UUID NOT NULL,
    feature    VARCHAR(40) NOT NULL,
    period     VARCHAR(7) NOT NULL,          -- 'YYYY-MM'
    count      INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_usage_counter UNIQUE (tenant_id, feature, period)
);
CREATE INDEX idx_usage_counter_tenant ON usage_counter (tenant_id);

-- 청구 스냅샷. lines = 라인아이템 JSON(요금제/애드온/사용량), 가격이 나중에 바뀌어도 발행 시점 값을 보존.
CREATE TABLE invoice (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL,
    period      VARCHAR(7) NOT NULL,
    currency    VARCHAR(3) NOT NULL DEFAULT 'KRW',
    total       INT NOT NULL,
    status      VARCHAR(10) NOT NULL DEFAULT 'ISSUED',   -- ISSUED | PAID
    lines       TEXT NOT NULL,                           -- JSON 스냅샷
    payment_ref VARCHAR(100),
    issued_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    paid_at     TIMESTAMPTZ
);
CREATE INDEX idx_invoice_tenant ON invoice (tenant_id);

-- 앱 롤 권한: 가격은 읽기, 사용량은 증가(upsert), 인보이스는 발행/결제(CRUD).
GRANT SELECT ON plan_price TO lms_app;
GRANT SELECT ON feature_addon_price TO lms_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON usage_counter TO lms_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON invoice TO lms_app;
