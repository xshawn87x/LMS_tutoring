"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useSession } from "./SessionProvider";
import { useFeatures } from "./FeaturesProvider";
import { getSettings, unreadCount } from "@/lib/api";

export function NavBar() {
  const { session, logout } = useSession();
  const { isEnabled } = useFeatures();
  const pathname = usePathname();
  const [unread, setUnread] = useState(0);
  const [brand, setBrand] = useState<{ name: string | null; logo: string | null; color: string | null }>({ name: null, logo: null, color: null });

  const isAdmin = !!session && session.roles.includes("ADMIN");
  const isLearner = !!session && session.roles.includes("STUDENT");
  const isTeacher = !!session && (session.roles.includes("INSTRUCTOR") || session.roles.includes("ADMIN"));
  const isParent = !!session && session.roles.includes("PARENT");

  // 알림 안읽음 수 (세션/경로 변화 시 갱신)
  const refreshUnread = useCallback(async () => {
    if (!session) { setUnread(0); return; }
    try { setUnread((await unreadCount(session.token)).count); } catch { /* 무시 */ }
  }, [session]);
  useEffect(() => { refreshUnread(); }, [refreshUnread, pathname]);

  // 학원 브랜딩 로드 + 대표색상 적용
  useEffect(() => {
    if (!session) { setBrand({ name: null, logo: null, color: null }); return; }
    getSettings(session.token).then((s) => {
      setBrand({ name: s.displayName ?? null, logo: s.logoUrl ?? null, color: s.primaryColor ?? null });
      if (s.primaryColor && typeof document !== "undefined") {
        document.documentElement.style.setProperty("--accent", s.primaryColor);
      }
    }).catch(() => { /* 무시 */ });
  }, [session]);

  return (
    <nav className="navbar">
      <Link href={session ? "/" : "/login"} className="brand row" style={{ color: "var(--text)", gap: 8 }}>
        {brand.logo
          // eslint-disable-next-line @next/next/no-img-element
          ? <img src={brand.logo} alt="" style={{ height: 24, maxWidth: 100, objectFit: "contain" }} />
          : null}
        {brand.name || "LMS"}
      </Link>
      {session && (
        <span className="links">
          <Link href="/">홈</Link>
          <Link href="/courses">과정</Link>
          {isEnabled("ENROLLMENTS") && <Link href="/my-learning">내 학습</Link>}
          <Link href="/notices">공지</Link>
          <Link href="/me/notifications" title="알림">🔔{unread > 0 && <span className="pf-pill issued" style={{ marginLeft: 2, padding: "0 6px", fontSize: 11 }}>{unread}</span>}</Link>
          {isLearner && isEnabled("CERTIFICATES") && <Link href="/certificates">수료증</Link>}
          {isParent && <Link href="/children">자녀 현황</Link>}
          {isTeacher && <Link href="/instructor">강사 대시보드</Link>}
          {isTeacher && <Link href="/admin/groups">반·출석</Link>}
          {isAdmin && <Link href="/admin/members">회원</Link>}
          {isAdmin && <Link href="/admin/market">마켓</Link>}
          {isAdmin && <Link href="/admin/settings">환경설정</Link>}
          {isAdmin && <Link href="/admin/features">기능</Link>}
        </span>
      )}
      <span className="spacer" />
      {session ? (
        <span className="row">
          <Link href="/account" className="muted">{session.displayName || session.subject}</Link>
          <span className="badge tenant">테넌트 {session.tenantId.slice(0, 4)}</span>
          {session.roles.map((r) => (
            <span key={r} className="badge role">{r}</span>
          ))}
          <button className="ghost" onClick={logout}>로그아웃</button>
        </span>
      ) : (
        <span className="row">
          <Link href="/platform" className="muted" title="SaaS 제공자(플랫폼) 슈퍼관리자">플랫폼 콘솔</Link>
          <Link href="/login" className="muted">로그인</Link>
        </span>
      )}
    </nav>
  );
}
