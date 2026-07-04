-- ============================================================
-- V1: 애플리케이션 DB 롤 + course 테이블
-- ============================================================
-- 이 마이그레이션은 테이블 소유자(lms_owner)로 실행된다.
-- 애플리케이션은 RLS 적용을 받기 위해 소유자가 아닌 별도 롤(lms_app)로 접속한다.

-- 앱 전용 롤 (소유자가 아님 → RLS의 대상이 됨)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'lms_app') THEN
        CREATE ROLE lms_app LOGIN PASSWORD 'lms_app_pw';
    END IF;
END
$$;

-- course 테이블 (소유자 = lms_owner)
CREATE TABLE course (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 테넌트 단위 조회 성능을 위한 인덱스
CREATE INDEX idx_course_tenant_id ON course (tenant_id);

-- 앱 롤에 스키마/테이블 접근 권한 부여 (RLS 정책이 행 단위로 추가 제한)
GRANT USAGE ON SCHEMA public TO lms_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON course TO lms_app;
