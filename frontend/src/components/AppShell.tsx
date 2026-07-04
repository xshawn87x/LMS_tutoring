"use client";

import { ReactNode } from "react";
import { usePathname } from "next/navigation";
import { NavBar } from "./NavBar";
import { PortalGuard } from "./PortalGuard";

// 구역에 따라 테마를 나눈다: 학습자(학생·학부모)·로그인은 Coursera 스타일 라이트,
// 운영(/manage)·플랫폼(/platform)은 집중형 다크.
export function AppShell({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const inOps = pathname === "/manage" || pathname.startsWith("/manage/");
  const inPlatform = pathname === "/platform" || pathname.startsWith("/platform/");
  const learner = !inOps && !inPlatform;

  // 정적 프리렌더 시점엔 usePathname()이 비어 학습자 테마로 굳는다(학습자=주 대상이라 무해).
  // 운영/플랫폼은 클라이언트에서 다크로 교정되므로 하이드레이션 경고만 억제한다.
  return (
    <div className={learner ? "learner" : undefined} suppressHydrationWarning>
      <NavBar />
      <main className="container">
        <PortalGuard>{children}</PortalGuard>
      </main>
    </div>
  );
}
