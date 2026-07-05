"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useSession } from "./SessionProvider";
import { useFeatures } from "./FeaturesProvider";
import { getSettings } from "@/lib/api";

// 운영(강사·관리자) 상단 내비. 학습자 공용 페이지(과정·공지 등)에서도 강사·관리자에겐
// 이 다크 내비가 렌더되어 운영 포털과 테마·내비가 통일된다. (학생·학부모는 LearnerLayout)
export function NavBar() {
  const { session, logout } = useSession();
  const { isEnabled } = useFeatures();
  const [brand, setBrand] = useState<{ name: string | null; logo: string | null }>({ name: null, logo: null });

  const isAdmin = !!session && session.roles.includes("ADMIN");

  // 학원 브랜딩 로드 + 대표색상 적용
  useEffect(() => {
    if (!session) { setBrand({ name: null, logo: null }); return; }
    getSettings(session.token).then((s) => {
      setBrand({ name: s.displayName ?? null, logo: s.logoUrl ?? null });
      if (s.primaryColor && typeof document !== "undefined") {
        document.documentElement.style.setProperty("--accent", s.primaryColor);
      }
    }).catch(() => { /* 무시 */ });
  }, [session]);

  return (
    <nav className="navbar">
      <Link href={session ? "/manage" : "/login"} className="brand row" style={{ color: "var(--text)", gap: 8 }}>
        {brand.logo
          // eslint-disable-next-line @next/next/no-img-element
          ? <img src={brand.logo} alt="" style={{ height: 24, maxWidth: 100, objectFit: "contain" }} />
          : null}
        {brand.name || "LMS"}
        {session && <span className="badge role" style={{ marginLeft: 4 }}>운영</span>}
      </Link>
      {session && (
        <span className="links">
          <Link href="/manage">대시보드</Link>
          {isEnabled("EXAMS") && <Link href="/manage/exams">시험·성적</Link>}
          {isEnabled("PLACEMENT") && <Link href="/manage/placement">반편성</Link>}
          {isEnabled("ATTENDANCE") && <Link href="/manage/groups">반·출석</Link>}
          {isEnabled("COUNSELING") && <Link href="/manage/counseling">상담</Link>}
          <Link href="/courses">과정</Link>
          <Link href="/notices">공지</Link>
          {isAdmin && isEnabled("NOTIFICATIONS") && <Link href="/manage/notifications">알림</Link>}
          {isAdmin && <Link href="/manage/members">회원</Link>}
          {isAdmin && isEnabled("MARKET") && <Link href="/manage/market">마켓</Link>}
          {isAdmin && <Link href="/manage/settings">환경설정</Link>}
          {isAdmin && <Link href="/manage/features">기능</Link>}
        </span>
      )}
      <span className="spacer" />
      {session ? (
        <span className="row">
          <Link href="/account" className="muted">{session.displayName || session.subject}</Link>
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
