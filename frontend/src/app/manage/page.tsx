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

  const totalEnroll = stats.reduce((a, s) => a + s.enrollmentCount, 0);
  const totalCompleted = stats.reduce((a, s) => a + s.completedCount, 0);

  return (
    <div>
      <h1>강사 대시보드 <span className="badge tenant">테넌트 {session.tenantId.slice(0, 4)}</span></h1>
      <p className="muted">
        이 기관의 과정 현황입니다. 과정 {stats.length}개 · 총 수강 {totalEnroll}건 · 완료 {totalCompleted}건
      </p>
      {error && <p className="error">{error}</p>}
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
