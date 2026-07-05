"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useSession } from "@/components/SessionProvider";
import { CourseStats, getInstructorCourses, levelLabel } from "@/lib/api";

export default function InstructorDashboardPage() {
  const { session, hydrated } = useSession();
  const [stats, setStats] = useState<CourseStats[]>([]);
  const [error, setError] = useState<string | null>(null);

  const canView = !!session && (session.roles.includes("INSTRUCTOR") || session.roles.includes("ADMIN"));

  const load = useCallback(async () => {
    if (!session || !canView) return;
    setError(null);
    try {
      setStats(await getInstructorCourses(session.token));
    } catch (e) {
      setError(e instanceof Error ? e.message : "불러오기 실패");
    }
  }, [session, canView]);

  useEffect(() => {
    load();
  }, [load]);

  if (!hydrated || !session) {
    return <p className="notice">세션이 없습니다. <Link href="/login">로그인</Link>하세요.</p>;
  }
  if (!canView) {
    return (
      <div>
        <h1>강사 대시보드</h1>
        <p className="error">INSTRUCTOR 또는 ADMIN 역할이 필요합니다. (현재: {session.roles.join(", ")})</p>
      </div>
    );
  }

  const isAdmin = session.roles.includes("ADMIN");
  const totalEnroll = stats.reduce((a, s) => a + s.enrollmentCount, 0);
  const totalCompleted = stats.reduce((a, s) => a + s.completedCount, 0);
  const avgProgress = stats.length ? Math.round(stats.reduce((a, s) => a + s.avgProgress, 0) / stats.length) : 0;

  const quick: { href: string; icon: string; label: string; admin?: boolean }[] = [
    { href: "/manage/exams", icon: "📝", label: "시험·성적" },
    { href: "/manage/placement", icon: "🧩", label: "반편성" },
    { href: "/manage/groups", icon: "🗓", label: "반·출석" },
    { href: "/courses", icon: "📚", label: "과정·수강" },
    { href: "/notices", icon: "📢", label: "공지" },
    { href: "/manage/members", icon: "👥", label: "회원", admin: true },
    { href: "/manage/market", icon: "🛒", label: "마켓", admin: true },
    { href: "/manage/settings", icon: "⚙️", label: "환경설정", admin: true },
    { href: "/manage/features", icon: "🎛", label: "기능", admin: true },
  ];

  return (
    <div>
      <h1>운영 대시보드 <span className="badge tenant">{session.orgCode ?? session.tenantId.slice(0, 4)}</span></h1>
      <p className="muted">{session.displayName || session.subject}님, 학원 운영 현황을 한눈에 확인하세요.</p>
      {error && <p className="error">{error}</p>}

      <div className="stat-row">
        <div className="stat-tile"><span className="st-label">과정</span><span className="st-value">{stats.length}</span></div>
        <div className="stat-tile"><span className="st-label">총 수강</span><span className="st-value">{totalEnroll}</span></div>
        <div className="stat-tile"><span className="st-label">완료</span><span className="st-value">{totalCompleted}</span></div>
        <div className="stat-tile"><span className="st-label">평균 진도</span><span className="st-value">{avgProgress}<small>%</small></span></div>
      </div>

      <div className="section-head" style={{ marginTop: 8 }}><h2>빠른 작업</h2></div>
      <div className="quick-grid">
        {quick.filter((q) => !q.admin || isAdmin).map((q) => (
          <Link className="quick-item" href={q.href} key={q.href}>
            <span className="quick-ic">{q.icon}</span>
            <span>{q.label}</span>
          </Link>
        ))}
      </div>

      <div className="section-head"><h2>과정 현황</h2></div>
      {stats.length === 0 && <p className="notice">과정이 없습니다. <Link href="/courses">과정 만들기</Link></p>}

      {stats.map((s) => (
        <div className="card" key={s.courseId}>
          <h3><Link href={`/courses/${s.courseId}`}>{s.title}</Link>
            {s.categoryCode && <span className="badge">{s.categoryCode}</span>}
            {s.level != null && <span className="badge">{levelLabel(s.level)}</span>}
          </h3>
          <div className="row" style={{ gap: 18, marginTop: 6 }}>
            <span className="muted">수강생 <b style={{ color: "var(--text)" }}>{s.enrollmentCount}</b>명</span>
            <span className="muted">완료 <b style={{ color: "var(--text)" }}>{s.completedCount}</b>명</span>
            <span className="muted">평균 진도 <b style={{ color: "var(--text)" }}>{s.avgProgress}%</b></span>
            <span className="muted">퀴즈 <b style={{ color: "var(--text)" }}>{s.quizCount}</b>개</span>
            <span className="muted">평균 점수 <b style={{ color: "var(--text)" }}>{s.avgQuizScore == null ? "—" : s.avgQuizScore + "%"}</b></span>
            <span className="muted">수료증 <b style={{ color: "var(--text)" }}>{s.certificateCount}</b>건</span>
          </div>
          <div className="progress-bar" style={{ marginTop: 10 }}>
            <span style={{ width: `${s.avgProgress}%` }} />
          </div>
          <div style={{ marginTop: 10 }}>
            <Link className="button" href={`/manage/courses/${s.courseId}`}>수강생 진도 상세 →</Link>
          </div>
        </div>
      ))}
    </div>
  );
}
