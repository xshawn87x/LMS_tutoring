"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useSession } from "@/components/SessionProvider";
import { ChildInfo, Enrollment, childEnrollments, myChildren } from "@/lib/api";

export default function ChildrenPage() {
  const { session } = useSession();
  const [children, setChildren] = useState<ChildInfo[]>([]);
  const [selected, setSelected] = useState<string | null>(null);
  const [enrollments, setEnrollments] = useState<Enrollment[]>([]);
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
    if (!session || !selected) { setEnrollments([]); return; }
    childEnrollments(session.token, selected).then(setEnrollments).catch(() => setEnrollments([]));
  }, [session, selected]);

  if (!session) return <p className="notice">세션이 없습니다. <Link href="/login">로그인</Link>하세요.</p>;
  if (!session.roles.includes("PARENT") && !session.roles.includes("ADMIN")) {
    return <div><h1>자녀 학습현황</h1><p className="error">학부모(PARENT) 계정만 접근할 수 있습니다.</p></div>;
  }

  return (
    <div>
      <h1>자녀 학습현황</h1>
      <p className="muted">연결된 자녀의 수강 강의와 진도를 확인합니다. 연결은 학원 관리자가 설정합니다.</p>
      {error && <p className="error">{error}</p>}

      {loading ? <p className="notice">불러오는 중…</p> : children.length === 0 ? (
        <p className="notice">연결된 자녀가 없습니다. 학원 관리자에게 자녀 계정 연결을 요청하세요.</p>
      ) : (
        <>
          <div className="row" style={{ marginBottom: 12 }}>
            {children.map((c) => (
              <button key={c.subject} className={selected === c.subject ? "" : "ghost"} onClick={() => setSelected(c.subject)}>
                {c.displayName ? `${c.displayName} (${c.subject})` : c.subject}
              </button>
            ))}
          </div>
          <div className="card">
            <h3>{children.find((c) => c.subject === selected)?.displayName ?? selected} 님의 수강 현황</h3>
            {enrollments.length === 0 ? <p className="notice">수강 중인 강의가 없습니다.</p> : (
              <table className="grid">
                <thead><tr><th>강의</th><th>상태</th><th style={{ textAlign: "right" }}>진도</th></tr></thead>
                <tbody>
                  {enrollments.map((e) => (
                    <tr key={e.id}>
                      <td>{e.courseId.slice(0, 8)}…</td>
                      <td>{e.status === "COMPLETED" ? <span className="pf-pill paid">완료</span> : "수강중"}</td>
                      <td style={{ textAlign: "right" }}>
                        <span className="progress-bar" style={{ display: "inline-block", width: 120, verticalAlign: "middle" }}>
                          <span style={{ width: `${e.progress}%` }} />
                        </span> {e.progress}%
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </>
      )}
    </div>
  );
}
