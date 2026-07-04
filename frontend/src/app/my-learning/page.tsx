"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useSession } from "@/components/SessionProvider";
import { useFeatures } from "@/components/FeaturesProvider";
import { useToast } from "@/components/ToastProvider";
import { cancelEnrollment, Course, Enrollment, listCourses, myEnrollments } from "@/lib/api";

export default function MyLearningPage() {
  const { session } = useSession();
  const { isEnabled, loaded: featuresLoaded } = useFeatures();
  const { showToast } = useToast();
  const [enrollments, setEnrollments] = useState<Enrollment[]>([]);
  const [titles, setTitles] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);

  const enrollmentsOn = isEnabled("ENROLLMENTS");

  const load = useCallback(async () => {
    if (!session || !featuresLoaded || !isEnabled("ENROLLMENTS")) return;
    setError(null);
    try {
      const [mine, courses] = await Promise.all([
        myEnrollments(session.token),
        listCourses(session.token),
      ]);
      setEnrollments(mine);
      setTitles(Object.fromEntries(courses.map((c: Course) => [c.id, c.title])));
    } catch (e) {
      setError(e instanceof Error ? e.message : "불러오기 실패");
    }
  }, [session, featuresLoaded, isEnabled]);

  useEffect(() => {
    load();
  }, [load]);

  if (!session) {
    return <p className="notice">세션이 없습니다. <Link href="/login">로그인</Link>하세요.</p>;
  }

  if (featuresLoaded && !enrollmentsOn) {
    return (
      <div>
        <h1>내 학습</h1>
        <p className="notice">이 기관에서는 수강신청 기능이 비활성화되어 있습니다.</p>
      </div>
    );
  }

  const onCancel = async (e: Enrollment) => {
    const name = titles[e.courseId] ?? "이 과정";
    if (!window.confirm(`'${name}' 수강을 취소할까요? 진도와 수료 기록도 함께 초기화됩니다.`)) return;
    setBusyId(e.id);
    setError(null);
    try {
      await cancelEnrollment(session.token, e.id);
      showToast("수강이 취소되었습니다");
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "수강 취소 실패");
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div>
      <h1>내 학습 <span className="muted">({session.subject})</span></h1>
      <p className="muted">내가 수강 중인 과정만 보입니다. 학습창에서 영상을 보면 진도가 자동으로 기록되고, 마지막 지점부터 이어듣기가 됩니다.</p>

      {error && <p className="error">{error}</p>}
      {enrollments.length === 0 && (
        <p className="notice">수강 중인 과정이 없습니다. <Link href="/courses">과정 둘러보기</Link></p>
      )}

      {enrollments.map((e) => (
        <div className="card" key={e.id}>
          <h3>{titles[e.courseId] ?? e.courseId.slice(0, 8)}</h3>
          <p className="muted">상태 <span className="badge">{e.status}</span> · 진도 {e.progress}%</p>
          <div className="progress-bar"><span style={{ width: `${e.progress}%` }} /></div>
          <div className="row" style={{ marginTop: 12 }}>
            <Link className="button" href={`/learn/${e.courseId}`}>
              {e.progress > 0 && e.progress < 100 ? "이어듣기 ▶" : e.progress >= 100 ? "다시 보기" : "학습 시작 ▶"}
            </Link>
            <button className="ghost" disabled={busyId === e.id} onClick={() => onCancel(e)}>
              {busyId === e.id ? "취소 중…" : "수강 취소"}
            </button>
          </div>
        </div>
      ))}
    </div>
  );
}
