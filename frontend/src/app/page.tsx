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
import { courseEmoji, thumbStyle } from "@/lib/thumb";

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

  const displayName = session.displayName || session.subject;

  return (
    <div>
      {/* 히어로 인사 */}
      <section className="hero">
        <h1>안녕하세요, {displayName}님 👋</h1>
        <p>오늘도 학습을 이어가 볼까요? 지금까지의 진도와 맞춤 추천을 확인하세요.</p>
        <div className="hero-cta">
          <Link className="button" href="/courses">과정 둘러보기</Link>
          {isEnabled("ENROLLMENTS") && <Link className="button ghost" href="/my-learning">내 학습</Link>}
        </div>
      </section>
      {error && <p className="error">{error}</p>}

      {/* 이어보기 */}
      {isEnabled("ENROLLMENTS") && (
        <>
          <div className="section-head">
            <h2>이어보기</h2>
            {inProgress.length > 0 && <Link href="/my-learning">모두 보기 →</Link>}
          </div>
          {inProgress.length === 0 ? (
            <p className="notice">진행 중인 과정이 없습니다. <Link href="/courses">과정 둘러보기</Link></p>
          ) : (
            <div className="course-grid">
              {inProgress.map((e) => (
                <div className="course-card" key={e.id}>
                  <Link href={`/learn/${e.courseId}`}>
                    <div className="course-thumb" style={thumbStyle(titles[e.courseId] ?? e.courseId)}>📘</div>
                  </Link>
                  <div className="cc-body">
                    <Link href={`/courses/${e.courseId}`}><span className="cc-title">{titles[e.courseId] ?? e.courseId.slice(0, 8)}</span></Link>
                    <div className="progress-line">
                      <div className="progress-bar"><span style={{ width: `${e.progress}%` }} /></div>
                      {e.progress}%
                    </div>
                  </div>
                  <div className="cc-foot">
                    <Link className="button" href={`/learn/${e.courseId}`} style={{ width: "100%", textAlign: "center" }}>이어듣기 ▶</Link>
                  </div>
                </div>
              ))}
            </div>
          )}
        </>
      )}

      {/* 맞춤 추천 */}
      {isLearner && isEnabled("RECOMMENDATIONS") && (
        <>
          <div className="section-head">
            <h2>맞춤 추천</h2>
            <Link href="/me/profile">관심분야 수정 →</Link>
          </div>
          {recs.length === 0 ? (
            <p className="notice">
              추천할 과정이 아직 없습니다. <Link href="/me/profile">관심분야·역량 설정</Link>을 업데이트해 보세요.
            </p>
          ) : (
            <div className="course-grid">
              {recs.map((r) => (
                <Link className="course-card" href={`/courses/${r.courseId}`} key={r.courseId}>
                  <div className="course-thumb" style={thumbStyle(r.title)}>{courseEmoji(r.categoryCode)}</div>
                  <div className="cc-body">
                    <span className="cc-title">{r.title}</span>
                    <div className="cc-meta">
                      {r.categoryCode && <span className="chip">{r.categoryCode}</span>}
                      {r.level != null && <span className="chip">{levelLabel(r.level)}</span>}
                    </div>
                    <span className="chip accent" title={r.reason}>✨ {r.reason}</span>
                  </div>
                </Link>
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
}
