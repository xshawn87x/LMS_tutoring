-- ============================================================
-- V29: 시험(자체 시험/모의고사)과 성적 — 입시/보습학원 특화
-- ============================================================
-- exam(시험 정의: 과목·시행일·만점, 선택적으로 반에 연결) 1:N exam_score(학생별 점수).
-- 성적 추이 그래프·학부모 리포트의 원천 데이터. 모두 RLS로 테넌트 격리.
-- 시험/성적 입력=INSTRUCTOR/ADMIN(컨트롤러), 본인 성적 조회=학생, 자녀 성적=연결된 학부모.

CREATE TABLE exam (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id  UUID NOT NULL,
    title      VARCHAR(255) NOT NULL,
    subject    VARCHAR(100),                  -- 과목명 (예: 수학, 영어). 추이 그래프에서 과목별 계열로 쓴다.
    exam_date  DATE NOT NULL,
    max_score  INT NOT NULL DEFAULT 100,
    group_id   UUID,                          -- 선택: 특정 반 대상 시험
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_exam_group FOREIGN KEY (group_id) REFERENCES student_group(id) ON DELETE SET NULL,
    CONSTRAINT ck_exam_max CHECK (max_score > 0)
);
CREATE INDEX idx_exam_tenant ON exam (tenant_id);
CREATE INDEX idx_exam_date ON exam (exam_date);

CREATE TABLE exam_score (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    exam_id         UUID NOT NULL,
    student_subject VARCHAR(320) NOT NULL,
    score           INT NOT NULL,
    comment         VARCHAR(500),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_score_exam FOREIGN KEY (exam_id) REFERENCES exam(id) ON DELETE CASCADE,
    CONSTRAINT uq_exam_score UNIQUE (exam_id, student_subject),
    CONSTRAINT ck_score_nonneg CHECK (score >= 0)
);
CREATE INDEX idx_score_tenant ON exam_score (tenant_id);
CREATE INDEX idx_score_exam ON exam_score (exam_id);
CREATE INDEX idx_score_student ON exam_score (student_subject);

ALTER TABLE exam ENABLE ROW LEVEL SECURITY;
ALTER TABLE exam FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON exam
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

ALTER TABLE exam_score ENABLE ROW LEVEL SECURITY;
ALTER TABLE exam_score FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON exam_score
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

GRANT SELECT, INSERT, UPDATE, DELETE ON exam TO lms_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON exam_score TO lms_app;
