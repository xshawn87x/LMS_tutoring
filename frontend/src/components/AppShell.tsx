"use client";

import { ReactNode } from "react";
import { usePathname } from "next/navigation";
import { NavBar } from "./NavBar";
import { PortalGuard } from "./PortalGuard";
import { LearnerLayout } from "./LearnerLayout";

// 구역에 따라 크롬(chrome)을 나눈다:
//  - 학습자(학생·학부모): 좌측 사이드바 레이아웃 + Coursera 라이트 테마
//  - 로그인: 사이드바 없는 미니멀 라이트 화면
//  - 운영(/manage)·플랫폼(/platform): 상단 내비 + 집중형 다크
export function AppShell({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const inOps = pathname === "/manage" || pathname.startsWith("/manage/");
  const inPlatform = pathname === "/platform" || pathname.startsWith("/platform/");
  const isLogin = pathname === "/login";
  const learner = !inOps && !inPlatform;

  // 정적 프리렌더 시 usePathname()이 비어 학습자 크롬으로 굳는다(학습자=주 대상). 운영/플랫폼은 클라이언트에서 교정.
  if (learner && !isLogin) {
    return (
      <div className="learner" suppressHydrationWarning>
        <LearnerLayout>{children}</LearnerLayout>
      </div>
    );
  }
  if (learner && isLogin) {
    return (
      <div className="learner" suppressHydrationWarning>
        <main className="container" style={{ maxWidth: 460 }}>
          <PortalGuard>{children}</PortalGuard>
        </main>
      </div>
    );
  }
  return (
    <div suppressHydrationWarning>
      <NavBar />
      <main className="container">
        <PortalGuard>{children}</PortalGuard>
      </main>
    </div>
  );
}
