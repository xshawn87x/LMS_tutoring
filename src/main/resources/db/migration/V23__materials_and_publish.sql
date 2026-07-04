-- ============================================================
-- V23: 자료실(강의별 자료) + 강의 노출(공개/비공개)
-- ============================================================

-- 자료실: 강의별 교재/자료 (파일은 업로드 API로 올리고 URL만 저장). RLS 격리.
CREATE TABLE course_material (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL,
    course_id   UUID NOT NULL,
    title       VARCHAR(255) NOT NULL,
    file_url    VARCHAR(500) NOT NULL,
    uploaded_by VARCHAR(320),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_material_course FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE
);
CREATE INDEX idx_material_tenant ON course_material (tenant_id);
CREATE INDEX idx_material_course ON course_material (course_id);

ALTER TABLE course_material ENABLE ROW LEVEL SECURITY;
ALTER TABLE course_material FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON course_material
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);
GRANT SELECT, INSERT, UPDATE, DELETE ON course_material TO lms_app;

-- 강의 노출: 기존 강의는 모두 공개(true)로.
ALTER TABLE course ADD COLUMN published BOOLEAN NOT NULL DEFAULT true;
