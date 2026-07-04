"use client";

import { ReactNode, useEffect } from "react";
import { usePathname, useRouter } from "next/navigation";
import { useSession } from "./SessionProvider";
import { accessRedirect } from "@/lib/portal";

// 로그인 없이 접근 가능한 구역 (플랫폼 콘솔은 별도 토큰으로 자체 가드)
const PUBLIC_PREFIXES = ["/login", "/platform"];

function isPublicPath(pathname: string): boolean {
  return PUBLIC_PREFIXES.some((p) => pathname === p || pathname.startsWith(p + "/"));
}

// 역할별 포털 접근을 강제한다. localStorage 토큰 기반이라 서버 미들웨어 대신 클라이언트에서 가드.
export function PortalGuard({ children }: { children: ReactNode }) {
  const { session, hydrated } = useSession();
  const pathname = usePathname();
  const router = useRouter();
  const isPublic = isPublicPath(pathname);

  useEffect(() => {
    if (!hydrated || isPublic) return; // 세션 복원 전엔 판단 보류
    if (!session) {
      router.replace("/login");
      return;
    }
    const to = accessRedirect(pathname, session.roles);
    if (to) router.replace(to);
  }, [hydrated, isPublic, session, pathname, router]);

  // 리다이렉트가 확정되기 전엔 콘텐츠를 감춰 화면 깜빡임을 막는다.
  if (!hydrated) return null;
  if (!isPublic) {
    if (!session) return null;
    if (accessRedirect(pathname, session.roles)) return null;
  }
  return <>{children}</>;
}
