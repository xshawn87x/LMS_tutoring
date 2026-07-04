-- ============================================================
-- V19: 과제 (제출 + 채점)
-- ============================================================
-- assignment(강의별 과제) 1 : N assignment_submission(학생 제출).
-- 학생당 과제당 1건 제출(재제출은 갱신). 강사/관리자가 채점(score/feedback).

CREATE TABLE assignment (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL,
    course_id   UUID NOT NULL,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    due_at      TIMESTAMPTZ,
    max_score   INT NOT NULL DEFAULT 100,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_assignment_course FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE
);
CREATE INDEX idx_assignment_tenant ON assignment (tenant_id);
CREATE INDEX idx_assignment_course ON assignment (course_id);

CREATE TABLE assignment_submission (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL,
    assignment_id UUID NOT NULL,
    student       VARCHAR(320) NOT NULL,
    text_answer   TEXT,
    file_url      VARCHAR(500),
    submitted_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    score         INT,
    feedback      TEXT,
    graded_at     TIMESTAMPTZ,
    CONSTRAINT fk_submission_assignment FOREIGN KEY (assignment_id) REFERENCES assignment(id) ON DELETE CASCADE,
    CONSTRAINT uq_submission UNIQUE (assignment_id, student)
);
CREATE INDEX idx_submission_tenant ON assignment_submission (tenant_id);
CREATE INDEX idx_submission_assignment ON assignment_submission (assignment_id);

ALTER TABLE assignment ENABLE ROW LEVEL SECURITY;
ALTER TABLE assignment FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON assignment
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

ALTER TABLE assignment_submission ENABLE ROW LEVEL SECURITY;
ALTER TABLE assignment_submission FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON assignment_submission
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

GRANT SELECT, INSERT, UPDATE, DELETE ON assignment TO lms_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON assignment_submission TO lms_app;
