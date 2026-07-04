// 역할 기반 "포털(구역)" 규칙 — 학습자 포털 / 운영 포털 / 플랫폼 콘솔을 경로로 분리한다.
//
//  - 학습자 포털: 루트("/") 및 과정·학습·수료증·자녀현황 등 (STUDENT·PARENT 중심, 운영자도 열람 가능)
//  - 운영 포털: "/manage/**" (INSTRUCTOR·ADMIN). 강사=교육 도구만, 관리자=교육+학원운영 전체
//  - 플랫폼 콘솔: "/platform" (PLATFORM_ADMIN, 별도 토큰) — 여기선 public 취급하고 자체 가드에 맡김

// 운영 포털 내에서 "관리자 전용" 경로 (강사는 접근 불가)
const ADMIN_ONLY_PREFIXES = ["/manage/members", "/manage/settings", "/manage/features", "/manage/market"];

export function isOps(roles: string[]): boolean {
  return roles.some((r) => r === "INSTRUCTOR" || r === "ADMIN");
}

export function isAdmin(roles: string[]): boolean {
  return roles.includes("ADMIN");
}

// 로그인/랜딩 시 역할에 맞는 홈 경로
export function homePathForRoles(roles: string[]): string {
  if (isOps(roles)) return "/manage";
  if (roles.includes("PARENT") && !roles.includes("STUDENT")) return "/children";
  return "/";
}

// 접근 거부 시 보낼 경로를 반환한다. null이면 접근 허용.
export function accessRedirect(pathname: string, roles: string[]): string | null {
  if (pathname === "/manage" || pathname.startsWith("/manage/")) {
    if (!isOps(roles)) return "/"; // 학생·학부모 → 운영 포털 차단
    if (ADMIN_ONLY_PREFIXES.some((p) => pathname === p || pathname.startsWith(p + "/")) && !isAdmin(roles)) {
      return "/manage"; // 강사 → 관리자 전용 화면 차단
    }
  }
  return null;
}
