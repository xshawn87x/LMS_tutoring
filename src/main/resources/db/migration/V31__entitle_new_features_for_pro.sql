-- ============================================================
-- V31: 신규 기능 플래그를 기존 PRO 테넌트에 자격 부여
-- ============================================================
-- Feature enum에 운영 모듈(EXAMS·PLACEMENT·ATTENDANCE·ASSIGNMENTS·QNA·MATERIALS·
-- COUNSELING·NOTIFICATIONS·TUITION·MARKET)을 추가했다. PRO 요금제는 전 기능을 포함하지만,
-- 기존 테넌트의 tenant_entitlement 행은 시드 시점 기능만 있으므로 신규 기능 자격을 보강한다.
-- (FREE/STANDARD 테넌트는 요금제 정책에 따라 제외 — 필요 시 플랫폼이 애드온 부여.)

INSERT INTO tenant_entitlement (tenant_id, feature, source)
SELECT t.id, f.feature, 'PLAN'
FROM tenant t
CROSS JOIN (VALUES
    ('EXAMS'), ('PLACEMENT'), ('ATTENDANCE'), ('ASSIGNMENTS'), ('QNA'),
    ('MATERIALS'), ('COUNSELING'), ('NOTIFICATIONS'), ('TUITION'), ('MARKET')
) AS f(feature)
WHERE t.plan = 'PRO'
  AND NOT EXISTS (
      SELECT 1 FROM tenant_entitlement e WHERE e.tenant_id = t.id AND e.feature = f.feature
  );
