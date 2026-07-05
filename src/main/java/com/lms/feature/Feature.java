package com.lms.feature;

/**
 * 기관(테넌트)별로 선택 활성화할 수 있는 기능 모듈.
 *
 * 과정(Course)은 코어라 플래그가 없다. 나머지 모듈은 기관마다 켜고/끌 수 있다.
 * defaultEnabled: tenant_feature override가 없을 때의 기본값.
 * implemented: 실제 백엔드 구현이 있는지(아직 없는 "예정" 기능을 화면에 보여주기 위함).
 */
public enum Feature {

    LESSONS("레슨", true, true),
    ENROLLMENTS("수강신청", true, true),
    QUIZZES("퀴즈/평가", true, true),
    DIAGNOSIS("역량 진단·관심분야", true, true),
    RECOMMENDATIONS("맞춤 추천", true, true),
    AI_CURATION("AI 콘텐츠 분석·큐레이션", false, true),
    CERTIFICATES("수료증 발급", false, true),
    // 입시/보습학원 운영 모듈 — 기관마다 켜고/끌 수 있다(기본 켜짐). 화면 노출(내비)을 제어.
    EXAMS("시험·성적", true, true),
    PLACEMENT("성적 기반 반편성", true, true),
    ATTENDANCE("반·출석", true, true),
    ASSIGNMENTS("과제", true, true),
    QNA("수강 Q&A", true, true),
    MATERIALS("자료실", true, true),
    COUNSELING("상담", true, true),
    NOTIFICATIONS("알림 발송", true, true),
    TUITION("수강료 결제", true, true),
    MARKET("콘텐츠 마켓", true, true);

    private final String displayName;
    private final boolean defaultEnabled;
    private final boolean implemented;

    Feature(String displayName, boolean defaultEnabled, boolean implemented) {
        this.displayName = displayName;
        this.defaultEnabled = defaultEnabled;
        this.implemented = implemented;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isDefaultEnabled() {
        return defaultEnabled;
    }

    public boolean isImplemented() {
        return implemented;
    }
}
