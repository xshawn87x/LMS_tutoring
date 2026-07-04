"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useSession } from "@/components/SessionProvider";
import { Course, getCourse, getCourseStudents, StudentProgress } from "@/lib/api";

export default function CourseStudentsPage() {
  const { courseId } = useParams<{ courseId: string }>();
  const { session, hydrated } = useSession();
  const [course, setCourse] = useState<Course | null>(null);
  const [students, setStudents] = useState<StudentProgress[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loaded, setLoaded] = useState(false);

  const canView = !!session && (session.roles.includes("INSTRUCTOR") || session.roles.includes("ADMIN"));

  const load = useCallback(async () => {
    if (!session || !canView) return;
    setError(null);
    try {
      const [c, s] = await Promise.all([
        getCourse(session.token, courseId),
        getCourseStudents(session.token, courseId),
      ]);
      setCourse(c);
      setStudents(s);
      setLoaded(true);
    } catch (e) {
      setError(e instanceof Error ? e.message : "불러오기 실패");
      setLoaded(true);
    }
  }, [session, canView, courseId]);

  useEffect(() => {
    load();
  }, [load]);

  if (!hydrated || !session) {
    return <p className="notice">세션이 없습니다. <Link href="/login">로그인</Link>하세요.</p>;
  }
  if (!canView) {
    return <div><h1>수강생 진도</h1><p className="error">INSTRUCTOR 또는 ADMIN 역할이 필요합니다.</p></div>;
  }

  const completedCount = students.filter((s) => s.completed).length;

  return (
    <div>
      <p className="muted"><Link href="/manage">← 강사 대시보드</Link></p>
      <h1>수강생 진도 <span className="muted">— {course?.title ?? courseId.slice(0, 8)}</span></h1>
      {error && <p className="error">{error}</p>}

      {loaded && students.length === 0 && <p className="notice">아직 수강생이 없습니다.</p>}

      {students.length > 0 && (
        <>
          <p className="muted">수강생 {students.length}명 · 완료 {completedCount}명</p>
          <div className="card" style={{ overflowX: "auto" }}>
            <table className="grid">
              <thead>
                <tr>
                  <th>수강생</th><th>진도</th><th>상태</th><th>퀴즈</th><th>평균점수</th><th>수료증</th>
                </tr>
              </thead>
              <tbody>
                {students.map((s) => (
                  <tr key={s.studentId}>
                    <td>{s.studentId}</td>
                    <td style={{ minWidth: 140 }}>
                      <div className="progress-bar"><span style={{ width: `${s.progress}%` }} /></div>
                      <span className="muted">{s.progress}%</span>
                    </td>
                    <td><span className="badge">{s.status}</span></td>
                    <td>{s.quizzesTaken}/{s.quizzesTotal}</td>
                    <td>{s.avgQuizScore == null ? "—" : s.avgQuizScore + "%"}</td>
                    <td>{s.certified ? "🎓 발급" : "—"}</td>
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
