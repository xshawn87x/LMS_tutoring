-- ============================================================
-- V5: 평가/퀴즈 — quiz · question · quiz_submission (동일 RLS 패턴)
-- ============================================================

-- 퀴즈: 과정에 속한 평가
CREATE TABLE quiz (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL,
    course_id   UUID NOT NULL REFERENCES course (id) ON DELETE CASCADE,
    title       VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_quiz_tenant_id ON quiz (tenant_id);
CREATE INDEX idx_quiz_course_id ON quiz (course_id);

-- 문항: 객관식. choices는 JSON 문자열 배열, correct_index는 정답 보기(0-based).
CREATE TABLE question (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL,
    quiz_id       UUID NOT NULL REFERENCES quiz (id) ON DELETE CASCADE,
    body          TEXT NOT NULL,
    choices       TEXT NOT NULL,           -- JSON 배열: ["A","B","C"]
    correct_index INT NOT NULL,            -- 정답 보기 인덱스 (응답에는 노출 안 함)
    order_no      INT NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_question_tenant_id ON question (tenant_id);
CREATE INDEX idx_question_quiz_id ON question (quiz_id);

-- 제출/채점 결과: 학생당 점수 기록
CREATE TABLE quiz_submission (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID NOT NULL,
    quiz_id       UUID NOT NULL REFERENCES quiz (id) ON DELETE CASCADE,
    student_id    VARCHAR(255) NOT NULL,
    score         INT NOT NULL,
    total         INT NOT NULL,
    submitted_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_submission_tenant_id ON quiz_submission (tenant_id);
CREATE INDEX idx_submission_student ON quiz_submission (tenant_id, student_id);

-- ------------------------------------------------------------
-- RLS: 세 테이블 모두 app.current_tenant 기준 격리 (fail-closed)
-- ------------------------------------------------------------
DO $$
DECLARE t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['quiz', 'question', 'quiz_submission'] LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', t);
        EXECUTE format(
            'CREATE POLICY tenant_isolation ON %I '
            || 'USING (tenant_id = NULLIF(current_setting(''app.current_tenant'', true), '''')::uuid) '
            || 'WITH CHECK (tenant_id = NULLIF(current_setting(''app.current_tenant'', true), '''')::uuid)', t);
        EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON %I TO lms_app', t);
    END LOOP;
END
$$;
