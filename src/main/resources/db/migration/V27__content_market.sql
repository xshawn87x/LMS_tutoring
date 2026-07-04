-- ============================================================
-- V27: 콘텐츠 마켓 (본사 제공 콘텐츠 → 학원 구매 → 정산)
-- ============================================================
-- market_content: 플랫폼(본사)이 등록하는 판매용 콘텐츠 카탈로그. 전역(RLS 없음).
-- content_purchase: 학원(테넌트)이 구매한 내역. 플랫폼이 정산 위해 테넌트 경계를 넘어 집계 →
--   RLS 없는 전역 테이블이며 tenant_id를 명시적으로 스코프한다.

CREATE TABLE market_content (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    category    VARCHAR(40),
    price       INT NOT NULL DEFAULT 0,
    provider    VARCHAR(255),
    published   BOOLEAN NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE content_purchase (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID NOT NULL,
    content_id   UUID NOT NULL,
    purchased_by VARCHAR(320),
    amount       INT NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_purchase_content FOREIGN KEY (content_id) REFERENCES market_content(id) ON DELETE CASCADE,
    CONSTRAINT uq_purchase UNIQUE (tenant_id, content_id)
);
CREATE INDEX idx_purchase_tenant ON content_purchase (tenant_id);
CREATE INDEX idx_purchase_content ON content_purchase (content_id);

-- 둘 다 전역(RLS 없음). content_purchase는 앱에서 tenant_id로 명시 스코프.
GRANT SELECT, INSERT, UPDATE, DELETE ON market_content TO lms_app;
GRANT SELECT, INSERT, DELETE ON content_purchase TO lms_app;
