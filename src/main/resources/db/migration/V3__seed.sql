-- ============================================================
-- V3: 검증용 시드 데이터 — 두 테넌트, 각자의 과정
-- ============================================================
-- FORCE RLS가 켜져 있어 소유자(lms_owner)인 이 마이그레이션도 RLS 대상이다.
-- 따라서 각 테넌트의 행을 넣기 전에 SET LOCAL로 세션 테넌트를 지정한다.
-- (SET LOCAL은 Flyway가 감싸는 이 마이그레이션 트랜잭션 범위에서만 유효 — RLS 동작도 함께 시연됨)

-- 테넌트 A
SET LOCAL app.current_tenant = '11111111-1111-1111-1111-111111111111';
INSERT INTO course (tenant_id, title, description) VALUES
    ('11111111-1111-1111-1111-111111111111', 'Spring 입문', 'Spring Boot 3 기초 과정'),
    ('11111111-1111-1111-1111-111111111111', 'JPA 기초', 'Spring Data JPA / Hibernate 입문');

-- 테넌트 B
SET LOCAL app.current_tenant = '22222222-2222-2222-2222-222222222222';
INSERT INTO course (tenant_id, title, description) VALUES
    ('22222222-2222-2222-2222-222222222222', 'Python 입문', 'Python 프로그래밍 기초');
