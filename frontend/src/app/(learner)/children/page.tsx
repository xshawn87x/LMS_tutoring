"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useSession } from "@/components/SessionProvider";
import { ScoreTrendChart } from "@/components/ScoreTrendChart";
import { ChildInfo, StudentReport, childReport, myChildren } from "@/lib/api";

const ATT_LABEL: Record<string, string> = { PRESENT: "출석", ABSENT: "결석", LATE: "지각", EXCUSED: "공결" };

export default function ChildrenPage() {
  const { session } = useSession();
  const [children, setChildren] = useState<ChildInfo[]>([]);
  const [selected, setSelected] = useState<string | null>(null);
  const [report, setReport] = useState<StudentReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(async () => {
    if (!session) return;
    setLoading(true);
    try {
      const kids = await myChildren(session.token);
      setChildren(kids);
      setSelected((cur) => cur ?? kids[0]?.subject ?? null);
    } catch (e) { setError(e instanceof Error ? e.message : "불러오기 실패"); }
    finally { setLoading(false); }
  }, [session]);

  useEffect(() => { reload(); }, [reload]);

  useEffect(() => {
    if (!session || !selected) { setReport(null); return; }
    childReport(session.token, selected).then(setReport).catch(() => setReport(null));
  }, [session, selected]);

  if (!session) return <p className="notice">세션이 없습니다. <Link href="/login">로그인</Link>하세요.</p>;
  if (!session.roles.includes("PARENT") && !session.roles.includes("ADMIN")) {
    return <div><h1>자녀 현황</h1><p className="error">학부모(PARENT) 계정만 접근할 수 있습니다.</p></div>;
  }

  const childName = children.find((c) => c.subject === selected)?.displayName ?? selected;

  return (
    <div>
      <h1>자녀 학습 리포트</h1>
      <p className="muted">연결된 자녀의 성적 추이·출석·과제·수강 현황을 한눈에 확인합니다. 연결은 학원 관리자가 설정합니다.</p>
      {error && <p className="error">{error}</p>}

      {loading ? <p className="notice">불러오는 중…</p> : children.length === 0 ? (
        <p className="notice">연결된 자녀가 없습니다. 학원 관리자에게 자녀 계정 연결을 요청하세요.</p>
      ) : (
        <>
          {children.length > 1 && (
            <div className="row" style={{ marginBottom: 16 }}>
              {children.map((c) => (
                <button key={c.subject} className={selected === c.subject ? "" : "ghost"} onClick={() => setSelected(c.subject)}>
                  {c.displayName ?? c.subject}
                </button>
              ))}
            </div>
          )}

          {!report ? <p className="notice">리포트를 불러오는 중…</p> : (
            <>
              <div className="stat-row">
                <div className="stat-tile"><span className="st-label">성적 평균</span>
                  <span className="st-value">{report.scoreAvgPercent ?? "—"}{report.scoreAvgPercent != null && <small>%</small>}</span></div>
                <div className="stat-tile"><span className="st-label">출석률</span>
                  <span className="st-value">{report.attendance.attendanceRate}<small>%</small></span></div>
                <div className="stat-tile"><span className="st-label">과제 제출</span>
                  <span className="st-value">{report.assignments.submitted}<small>건</small></span></div>
                <div className="stat-tile"><span className="st-label">수강</span>
                  <span className="st-value">{report.courses.enrolled}<small>개</small></span></div>
                <div className="stat-tile"><span className="st-label">평균 진도</span>
                  <span className="st-value">{report.courses.avgProgress}<small>%</small></span></div>
              </div>

              <div className="card">
                <h3>{childName} 님의 성적 추이 <span className="muted" style={{ fontSize: 13, fontWeight: 400 }}>(과목별)</span></h3>
                <ScoreTrendChart data={report.scores.map((s) => ({ date: s.examDate, percent: s.percent, label: s.title, series: s.subject }))} />
              </div>

              {report.scores.length > 0 && (
                <div className="card">
                  <h3>시험별 성적</h3>
                  <table className="grid">
                    <thead><tr><th>시행일</th><th>시험</th><th>과목</th><th style={{ textAlign: "right" }}>점수</th><th style={{ textAlign: "right" }}>백분율</th><th style={{ textAlign: "right" }}>석차</th></tr></thead>
                    <tbody>
                      {[...report.scores].reverse().map((s) => (
                        <tr key={s.examId}>
                          <td>{s.examDate}</td><td>{s.title}</td><td>{s.subject ?? "—"}</td>
                          <td style={{ textAlign: "right" }}>{s.score} / {s.maxScore}</td>
                          <td style={{ textAlign: "right", fontWeight: 700 }}>{s.percent}%</td>
                          <td style={{ textAlign: "right" }}>{s.rank}/{s.totalTakers} <span className="muted">(상위 {s.topPercent}%)</span></td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}

              <div className="card">
                <h3>출석 · 과제</h3>
                <p className="muted" style={{ marginTop: 0 }}>
                  출석 {report.attendance.total}일 중 출석 {report.attendance.present} · 지각 {report.attendance.late} · 결석 {report.attendance.absent} · 공결 {report.attendance.excused}
                </p>
                {report.recentAttendance.length > 0 && (
                  <table className="grid" style={{ marginTop: 4, marginBottom: 12 }}>
                    <thead><tr><th>날짜</th><th>상태</th><th>메모</th></tr></thead>
                    <tbody>
                      {report.recentAttendance.map((a, i) => (
                        <tr key={i}>
                          <td>{a.date}</td>
                          <td>{ATT_LABEL[a.status] ?? a.status}</td>
                          <td className="muted">{a.note ?? "—"}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
                <p className="muted" style={{ marginBottom: 0 }}>
                  과제 {report.assignments.submitted}건 제출 · {report.assignments.graded}건 채점
                  {report.assignments.avgScore != null && ` · 평균 ${report.assignments.avgScore}점`}
                </p>
              </div>
            </>
          )}
        </>
      )}
    </div>
  );
}
