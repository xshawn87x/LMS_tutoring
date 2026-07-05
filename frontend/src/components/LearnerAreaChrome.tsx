"use client";

import { ReactNode } from "react";
import { useSession } from "./SessionProvider";
import { NavBar } from "./NavBar";
import { PortalGuard } from "./PortalGuard";
import { LearnerLayout } from "./LearnerLayout";

// 학습자 그룹((learner)) 페이지의 크롬을 "경로"가 아니라 "역할"로 정한다.
//  - 강사·관리자: 운영 포털과 동일한 다크 내비(NavBar) → 과정·공지 등 공용 페이지도 테마 통일
//  - 학생·학부모: 좌측 사이드바 + 라이트 테마(LearnerLayout)
// 세션 복원 전(hydrated=false)엔 아무 크롬도 그리지 않아 "잘못된 테마 깜빡임"을 막는다.
export function LearnerAreaChrome({ children }: { children: ReactNode }) {
  const { session, hydrated } = useSession();
  if (!hydrated) return null;

  const isStaff = !!session && (session.roles.includes("INSTRUCTOR") || session.roles.includes("ADMIN"));

  if (isStaff) {
    return (
      <>
        <NavBar />
        <main className="container">
          <PortalGuard>{children}</PortalGuard>
        </main>
      </>
    );
  }

  return (
    <div className="learner">
      <LearnerLayout>{children}</LearnerLayout>
    </div>
  );
}
