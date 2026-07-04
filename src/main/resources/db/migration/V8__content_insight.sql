-- ============================================================
-- V8: AI 콘텐츠 분석 결과(큐레이션) — 과정별 인사이트
-- ============================================================
-- 과정/레슨 내용을 분석해 태그·난이도·요약·예상시간을 저장한다.
-- generated_by: HEURISTIC(기본, 무료) | CLAUDE(키 설정 시). AI_CURATION 기능 플래그로 게이팅.

CREATE TABLE content_insight (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID NOT NULL,
    course_id    UUID NOT NULL REFERENCES course (id) ON DELETE CASCADE,
    tags         TEXT NOT NULL,            -- JSON 배열 ["spring","jpa"]
    difficulty   INT,                      -- 0 입문 ~ 3 고급 (분석 추정)
    summary      TEXT,
    est_minutes  INT,
    generated_by VARCHAR(20) NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_content_insight UNIQUE (tenant_id, course_id)
);
CREATE INDEX idx_content_insight_tenant ON content_insight (tenant_id);

ALTER TABLE content_insight ENABLE ROW LEVEL SECURITY;
ALTER TABLE content_insight FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON content_insight
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

GRANT SELECT, INSERT, UPDATE, DELETE ON content_insight TO lms_app;
