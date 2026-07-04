-- ============================================================
-- V18: 강의 Q&A (학습질문 게시판)
-- ============================================================
-- 수강생이 강의별로 질문하고, 강사/관리자가 답변한다. RLS로 테넌트 격리.
-- course_question(질문) 1 : N course_answer(답변). 질문 resolved 플래그.

CREATE TABLE course_question (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id  UUID NOT NULL,
    course_id  UUID NOT NULL,
    author     VARCHAR(320) NOT NULL,
    title      VARCHAR(255) NOT NULL,
    body       TEXT,
    resolved   BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_question_course FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE
);
CREATE INDEX idx_question_tenant ON course_question (tenant_id);
CREATE INDEX idx_question_course ON course_question (course_id);

CREATE TABLE course_answer (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL,
    question_id UUID NOT NULL,
    author      VARCHAR(320) NOT NULL,
    body        TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_answer_question FOREIGN KEY (question_id) REFERENCES course_question(id) ON DELETE CASCADE
);
CREATE INDEX idx_answer_tenant ON course_answer (tenant_id);
CREATE INDEX idx_answer_question ON course_answer (question_id);

ALTER TABLE course_question ENABLE ROW LEVEL SECURITY;
ALTER TABLE course_question FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON course_question
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

ALTER TABLE course_answer ENABLE ROW LEVEL SECURITY;
ALTER TABLE course_answer FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON course_answer
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

GRANT SELECT, INSERT, UPDATE, DELETE ON course_question TO lms_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON course_answer TO lms_app;
