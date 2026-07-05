"use client";

import { ReactNode, useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useSession } from "./SessionProvider";
import { useFeatures } from "./FeaturesProvider";
import { PortalGuard } from "./PortalGuard";
import { getSettings, unreadCount } from "@/lib/api";

// 학습자 포털(학생·학부모) 좌측 사이드바 레이아웃 — 좌측 공간을 내비게이션에 활용.
export function LearnerLayout({ children }: { children: ReactNode }) {
  const { session, logout } = useSession();
  const { isEnabled } = useFeatures();
  const pathname = usePathname();
  const [unread, setUnread] = useState(0);
  const [brand, setBrand] = useState<{ name: string | null; logo: string | null }>({ name: null, logo: null });
  const [open, setOpen] = useState(false); // 모바일 서랍

  const isStudent = !!session && session.roles.includes("STUDENT");
  const isParent = !!session && session.roles.includes("PARENT");
  const isTeacher = !!session && (session.roles.includes("INSTRUCTOR") || session.roles.includes("ADMIN"));

  const refreshUnread = useCallback(async () => {
    if (!session) { setUnread(0); return; }
    try { setUnread((await unreadCount(session.token)).count); } catch { /* 무시 */ }
  }, [session]);
  useEffect(() => { refreshUnread(); }, [refreshUnread, pathname]);

  useEffect(() => {
    if (!session) { setBrand({ name: null, logo: null }); return; }
    getSettings(session.token).then((s) => {
      setBrand({ name: s.displayName ?? null, logo: s.logoUrl ?? null });
      if (s.primaryColor && typeof document !== "undefined") {
        document.documentElement.style.setProperty("--accent", s.primaryColor);
      }
    }).catch(() => { /* 무시 */ });
  }, [session]);

  useEffect(() => { setOpen(false); }, [pathname]); // 이동 시 서랍 닫기

  const item = (href: string, icon: string, label: string, badge?: number) => {
    const active = href === "/" ? pathname === "/" : pathname === href || pathname.startsWith(href + "/");
    return (
      <Link href={href} className={`ls-item${active ? " active" : ""}`}>
        <span className="ico">{icon}</span>
        <span>{label}</span>
        {badge != null && badge > 0 && <span className="ls-badge">{badge}</span>}
      </Link>
    );
  };

  const nav = session && (
    <nav className="ls-nav">
      {item("/home", "🏠", "홈")}
      {item("/courses", "🧭", "과정 탐색")}
      {isEnabled("ENROLLMENTS") && item("/my-learning", "📚", "내 학습")}
      {isStudent && item("/grades", "📈", "내 성적")}
      {isParent && item("/children", "👨‍👩‍👧", "자녀 현황")}
      {item("/notices", "📢", "공지사항")}
      {item("/me/notifications", "🔔", "알림", unread)}
      {isStudent && isEnabled("CERTIFICATES") && item("/certificates", "🎓", "수료증")}
      {isTeacher && (
        <>
          <div className="ls-sep" />
          <Link href="/manage" className="ls-item"><span className="ico">🛠</span><span>운영 포털</span></Link>
        </>
      )}
    </nav>
  );

  return (
    <div className="learner-shell">
      {/* 모바일 오버레이 */}
      {open && <div className="ls-overlay" onClick={() => setOpen(false)} />}

      <aside className={`learner-sidebar${open ? " open" : ""}`}>
        <Link href="/home" className="ls-brand">
          {brand.logo
            // eslint-disable-next-line @next/next/no-img-element
            ? <img src={brand.logo} alt="" style={{ height: 24, maxWidth: 110, objectFit: "contain" }} />
            : <span className="ls-brand-mark">🎓</span>}
          <span>{brand.name || "LMS"}</span>
        </Link>
        {nav}
        {session && (
          <div className="ls-foot">
            <Link href="/account" className="ls-user">
              <span className="ls-avatar">{(session.displayName || session.subject).slice(0, 1).toUpperCase()}</span>
              <span className="ls-user-meta">
                <b>{session.displayName || session.subject}</b>
                <span className="muted">{session.roles.includes("PARENT") ? "학부모" : "학생"}</span>
              </span>
            </Link>
            <button className="ghost" onClick={logout} style={{ width: "100%", marginTop: 8 }}>로그아웃</button>
          </div>
        )}
      </aside>

      <div className="learner-content">
        {/* 모바일 상단바 */}
        <div className="ls-topbar">
          <button className="ghost ls-hamburger" onClick={() => setOpen(true)} aria-label="메뉴">☰</button>
          <Link href="/home" className="ls-brand" style={{ padding: 0 }}>
            <span className="ls-brand-mark">🎓</span><span>{brand.name || "LMS"}</span>
          </Link>
          <span style={{ flex: 1 }} />
          <Link href="/me/notifications" className="ls-item" style={{ padding: 8 }}>
            🔔{unread > 0 && <span className="ls-badge">{unread}</span>}
          </Link>
        </div>
        <main className="container">
          <PortalGuard>{children}</PortalGuard>
        </main>
      </div>
    </div>
  );
}
