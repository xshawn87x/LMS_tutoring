package com.lms.security;

/**
 * 역할(Role) 상수. JWT의 "roles" 클레임 값이자 Spring Security 권한(ROLE_ 접두사 후)의 이름.
 *
 * 참고: @PreAuthorize 의 SpEL 문자열에서는 상수를 직접 참조하기 어려워 리터럴을 쓴다.
 * 이 클래스는 토큰 발급/변환 코드의 오타 방지용 단일 출처다.
 */
public final class Roles {

    /** 플랫폼 슈퍼관리자 — 테넌트 경계를 넘어 요금제·자격을 관리(특정 테넌트 소속 아님) */
    public static final String PLATFORM_ADMIN = "PLATFORM_ADMIN";
    /** 관리자 — 한 기관(테넌트) 안의 전체 권한 */
    public static final String ADMIN = "ADMIN";
    /** 강사 — 과정·레슨 생성/관리 */
    public static final String INSTRUCTOR = "INSTRUCTOR";
    /** 학생 — 수강신청·진도 관리 */
    public static final String STUDENT = "STUDENT";
    /** 학부모 — 연결된 자녀의 학습 현황 조회(읽기 전용) */
    public static final String PARENT = "PARENT";

    /** JWT 클레임 이름 */
    public static final String CLAIM = "roles";

    private Roles() {
    }
}
