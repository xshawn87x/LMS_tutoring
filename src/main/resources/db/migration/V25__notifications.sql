-- ============================================================
-- V25: 인앱 알림 + 발송 이력(외부 채널 스텁)
-- ============================================================
-- notification: 인앱 알림(수신자별, 읽음 처리).
-- delivery_log: 채널별(인앱/SMS/카카오) 발송 시도 이력. SMS/카카오는 외부 유료라 스텁(SIMULATED).

CREATE TABLE notification (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id  UUID NOT NULL,
    recipient  VARCHAR(320) NOT NULL,
    title      VARCHAR(255) NOT NULL,
    body       TEXT,
    category   VARCHAR(40),
    is_read    BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_noti_tenant ON notification (tenant_id);
CREATE INDEX idx_noti_recipient ON notification (recipient, is_read);

CREATE TABLE delivery_log (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id  UUID NOT NULL,
    channel    VARCHAR(10) NOT NULL,   -- IN_APP | SMS | KAKAO
    recipient  VARCHAR(320) NOT NULL,
    title      VARCHAR(255),
    status     VARCHAR(12) NOT NULL,   -- SENT | SIMULATED | FAILED
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_delivery_tenant ON delivery_log (tenant_id);

ALTER TABLE notification ENABLE ROW LEVEL SECURITY;
ALTER TABLE notification FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON notification
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

ALTER TABLE delivery_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE delivery_log FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON delivery_log
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

GRANT SELECT, INSERT, UPDATE, DELETE ON notification TO lms_app;
GRANT SELECT, INSERT ON delivery_log TO lms_app;
