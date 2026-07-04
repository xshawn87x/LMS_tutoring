-- ============================================================
-- V2: Row Level Security (RLS) — 멀티테넌시의 핵심
-- ============================================================
-- 모든 행 접근을 세션 변수 app.current_tenant 기준으로 강제 격리한다.
-- 애플리케이션 코드는 WHERE tenant_id = ? 를 직접 쓰지 않는다 — DB가 강제한다.

ALTER TABLE course ENABLE ROW LEVEL SECURITY;
-- FORCE: 테이블 소유자(lms_owner)에게도 RLS를 적용해 우회를 방지 (defense-in-depth)
ALTER TABLE course FORCE ROW LEVEL SECURITY;

-- 정책: 현재 세션의 테넌트와 일치하는 행만 읽고/쓸 수 있다.
--   current_setting(..., true) = missing_ok → 세션 변수가 없으면 NULL 반환(에러 아님)
--   NULLIF(..., '') → 빈 문자열도 NULL 처리
--   변수가 없거나 비면 tenant_id = NULL → 어떤 행도 매칭 안 됨(fail-closed)
CREATE POLICY tenant_isolation ON course
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);
