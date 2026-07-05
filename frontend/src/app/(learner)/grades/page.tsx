"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useSession } from "@/components/SessionProvider";
import { ScoreTrendChart } from "@/components/ScoreTrendChart";
import { myScores, StudentScore } from "@/lib/api";

export default function GradesPage() {
  const { session } = useSession();
  const [scores, setScores] = useState<StudentScore[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (!session) return;
    setLoading(true);
    try { setScores(await myScores(session.token)); }
    catch (e) { setError(e instanceof Error ? e.message : "불러오기 실패"); }
    finally { setLoading(false); }
  }, [session]);

  useEffect(() => { load(); }, [load]);

  if (!session) return <p className="notice">세션이 없습니다. <Link href="/login">로그인</Link>하세요.</p>;
  if (!session.roles.includes("STUDENT") && !session.roles.includes("ADMIN")) {
    return <div><h1>내 성적</h1><p className="notice">학생 계정의 성적 화면입니다. 자녀 성적은 <Link href="/children">자녀 현황</Link>에서 확인하세요.</p></div>;
  }

  const avg = scores.length ? Math.round(scores.reduce((s, x) => s + x.percent, 0) / scores.length) : null;
  const latest = scores.length ? scores[scores.length - 1].percent : null;
  const best = scores.length ? Math.max(...scores.map((s) => s.percent)) : null;

  return (
    <div>
      <h1>내 성적</h1>
      <p className="muted">시험 성적의 추이를 확인하세요. 성적은 강사·관리자가 입력합니다.</p>
      {error && <p className="error">{error}</p>}

      {loading ? (
        <p className="notice">불러오는 중…</p>
      ) : scores.length === 0 ? (
        <p className="notice">아직 등록된 성적이 없습니다.</p>
      ) : (
        <>
          <div className="stat-row">
            <div className="stat-tile"><span className="st-label">평균</span><span className="st-value">{avg}<small>%</small></span></div>
            <div className="stat-tile"><span className="st-label">최근</span><span className="st-value">{latest}<small>%</small></span></div>
            <div className="stat-tile"><span className="st-label">최고</span><span className="st-value">{best}<small>%</small></span></div>
            <div className="stat-tile"><span className="st-label">응시</span><span className="st-value">{scores.length}<small>회</small></span></div>
          </div>

          <div className="card">
            <h3>성적 추이 <span className="muted" style={{ fontSize: 13, fontWeight: 400 }}>(과목별)</span></h3>
            <ScoreTrendChart data={scores.map((s) => ({ date: s.examDate, percent: s.percent, label: s.title, series: s.subject }))} />
          </div>

          <div className="card">
            <h3>시험별 성적</h3>
            <table className="grid">
              <thead><tr><th>시행일</th><th>시험</th><th>과목</th><th style={{ textAlign: "right" }}>점수</th><th style={{ textAlign: "right" }}>백분율</th><th style={{ textAlign: "right" }}>석차</th></tr></thead>
              <tbody>
                {[...scores].reverse().map((s) => (
                  <tr key={s.examId}>
                    <td>{s.examDate}</td>
                    <td>{s.title}</td>
                    <td>{s.subject ?? "—"}</td>
                    <td style={{ textAlign: "right" }}>{s.score} / {s.maxScore}</td>
                    <td style={{ textAlign: "right", fontWeight: 700 }}>{s.percent}%</td>
                    <td style={{ textAlign: "right" }}>{s.rank}/{s.totalTakers} <span className="muted">(상위 {s.topPercent}%)</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  );
}
