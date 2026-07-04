"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useSession } from "@/components/SessionProvider";
import { useFeatures } from "@/components/FeaturesProvider";
import {
  Course,
  Enrollment,
  getProfile,
  getRecommendations,
  levelLabel,
  listCourses,
  myEnrollments,
  Recommendation,
} from "@/lib/api";

export default function DashboardPage() {
  const { session, hydrated } = useSession();
  const { isEnabled, loaded: featuresLoaded } = useFeatures();
  const router = useRouter();

  const [inProgress, setInProgress] = useState<Enrollment[]>([]);
  const [titles, setTitles] = useState<Record<string, string>>({});
  const [recs, setRecs] = useState<Recommendation[]>([]);
  const [error, setError] = useState<string | null>(null);

  const isLearner = !!session && session.roles.includes("STUDENT");

  // 세션 없으면 로그인으로
  useEffect(() => {
    if (hydrated && !session) router.replace("/login");
  }, [hydrated, session, router]);

  const load = useCallback(async () => {
    if (!session || !featuresLoaded) return;
    setError(null);
    try {
      // 학생이면 온보딩 여부 확인 → 미완료 시 온보딩으로
      if (isLearner && isEnabled("DIAGNOSIS")) {
        const profile = await getProfile(session.token);
        if (!profile.onboarded) {
          router.replace("/onboarding");
          return;
        }
      }
      // 이어보기 (진행 중 수강)
      if (isEnabled("ENROLLMENTS")) {
        const [mine, courses] = await Promise.all([
          myEnrollments(session.token),
          listCourses(session.token),
        ]);
        setInProgress(mine.filter((e) => e.progress < 100));
        setTitles(Object.fromEntries(courses.map((c: Course) => [c.id, c.title])));
      }
      // 추천
      if (isLearner && isEnabled("RECOMMENDATIONS")) {
        setRecs(await getRecommendations(session.token));
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : "불러오기 실패");
    }
  }, [session, featuresLoaded, isEnabled, isLearner, router]);

  useEffect(() => {
    load();
  }, [load]);

  if (!hydrated || !session) {
    return <p className="notice">불러오는 중…</p>;
  }

  return (
    <div>
      <h1>안녕하세요, {session.subject}님 👋</h1>
      <p className="muted">
        <span className="badge tenant">테넌트 {session.tenantId.slice(0, 4)}</span>
        {session.roles.map((r) => <span key={r} className="badge role">{r}</span>)}
      </p>
      {error && <p className="error">{error}</p>}

      {/* 이어보기 */}
      {isEnabled("ENROLLMENTS") && (
        <>
          <h2>이어보기</h2>
          {inProgress.length === 0 ? (
            <p className="notice">진행 중인 과정이 없습니다. <Link href="/courses">과정 둘러보기</Link></p>
          ) : (
            inProgress.map((e) => (
              <div className="card" key={e.id}>
                <h3><Link href={`/courses/${e.courseId}`}>{titles[e.courseId] ?? e.courseId.slice(0, 8)}</Link></h3>
                <p className="muted">진도 {e.progress}%</p>
                <div className="progress-bar"><span style={{ width: `${e.progress}%` }} /></div>
              </div>
            ))
          )}
        </>
      )}

      {/* 맞춤 추천 */}
      {isLearner && isEnabled("RECOMMENDATIONS") && (
        <>
          <h2>맞춤 추천</h2>
          {recs.length === 0 ? (
            <p className="notice">
              추천할 과정이 아직 없습니다. <Link href="/me/profile">관심분야·역량 설정</Link>을 업데이트해 보세요.
            </p>
          ) : (
            recs.map((r) => (
              <div className="card" key={r.courseId}>
                <h3><Link href={`/courses/${r.courseId}`}>{r.title}</Link></h3>
                <p className="muted">
                  {r.categoryCode && <span className="badge">{r.categoryCode}</span>}
                  {r.level != null && <span className="badge">{levelLabel(r.level)}</span>}
                  <span className="badge role">왜? {r.reason}</span>
                </p>
              </div>
            ))
          )}
          <p className="muted"><Link href="/me/profile">내 관심분야·역량 수정 →</Link></p>
        </>
      )}

      {/* 빠른 이동 */}
      <h2>바로가기</h2>
      <div className="row">
        <Link href="/courses"><button className="ghost">과정 전체</button></Link>
        {isEnabled("ENROLLMENTS") && <Link href="/my-learning"><button className="ghost">내 학습</button></Link>}
        {isLearner && isEnabled("DIAGNOSIS") && <Link href="/me/profile"><button className="ghost">내 프로필</button></Link>}
        {session.roles.includes("ADMIN") && <Link href="/admin/features"><button className="ghost">기능 설정</button></Link>}
      </div>
    </div>
  );
}
