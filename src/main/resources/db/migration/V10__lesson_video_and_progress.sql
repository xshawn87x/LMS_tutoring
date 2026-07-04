-- ============================================================
-- V10: 레슨 동영상 + 학습자별 레슨 진도(이어듣기·완료) — 학습창 기반
-- ============================================================

-- 레슨에 동영상 URL (없으면 프론트가 샘플 영상으로 대체)
ALTER TABLE lesson ADD COLUMN video_url VARCHAR(1000);

-- 학습자별 레슨 진도: 마지막 재생 위치(이어듣기) + 완료 여부
CREATE TABLE lesson_progress (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id             UUID NOT NULL,
    student_id            VARCHAR(255) NOT NULL,
    lesson_id             UUID NOT NULL REFERENCES lesson (id) ON DELETE CASCADE,
    course_id             UUID NOT NULL REFERENCES course (id) ON DELETE CASCADE,
    last_position_seconds INT NOT NULL DEFAULT 0,
    completed             BOOLEAN NOT NULL DEFAULT false,
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_lesson_progress UNIQUE (tenant_id, student_id, lesson_id)
);
CREATE INDEX idx_lesson_progress_tenant ON lesson_progress (tenant_id);
CREATE INDEX idx_lesson_progress_student_course ON lesson_progress (tenant_id, student_id, course_id);

ALTER TABLE lesson_progress ENABLE ROW LEVEL SECURITY;
ALTER TABLE lesson_progress FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON lesson_progress
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

GRANT SELECT, INSERT, UPDATE, DELETE ON lesson_progress TO lms_app;
