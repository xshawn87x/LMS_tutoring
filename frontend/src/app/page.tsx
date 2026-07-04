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
import { courseEmoji, thumbBg } from "@/lib/thumb";

export default function DashboardPage() {
  const { session, hydrated } = useSession();
  const { isEnabled, loaded: featuresLoaded } = useFeatures();
  const router = useRouter();

  const [inProgress, setInProgress] = useState<Enrollment[]>([]);
  const [titles, setTitles] = useState<Record<string, string>>({});
  const [browse, setBrowse] = useState<Course[]>([]);
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
      const courses = await listCourses(session.token);
      setTitles(Object.fromEntries(courses.map((c: Course) => [c.id, c.title])));
      // 이어보기 (진행 중 수강)
      let progressIds = new Set<string>();
      if (isEnabled("ENROLLMENTS")) {
        const mine = await myEnrollments(session.token);
        const active = mine.filter((e) => e.progress < 100);
        setInProgress(active);
        progressIds = new Set(active.map((e) => e.courseId));
      }
      // 둘러보기: 진행 중이 아닌 공개 과정 몇 개
      setBrowse(courses.filter((c) => c.published !== false && !progressIds.has(c.id)).slice(0, 8));
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
        <span className="hero-eyebrow">오늘의 학습</span>
        <h1>안녕하세요, {displayName}님 👋</h1>
        <p>배움을 이어갈 시간이에요. 진행 중인 과정과 맞춤 추천을 한눈에 확인하세요.</p>
        <div className="hero-cta">
          <Link className="button" href="/courses">과정 둘러보기</Link>
          {isEnabled("ENROLLMENTS") && <Link className="button ghost" href="/my-learning">내 학습</Link>}
        </div>
      </section>
      {error && <p className="error">{error}</p>}

      {/* 이어보기 */}
      {isEnabled("ENROLLMENTS") && inProgress.length > 0 && (
        <>
          <div className="section-head">
            <h2>이어보기</h2>
            <Link href="/my-learning">모두 보기 →</Link>
          </div>
          <div className="course-grid">
            {inProgress.map((e) => (
              <div className="course-card" key={e.id}>
                <Link href={`/learn/${e.courseId}`}>
                  <div className="course-thumb" style={thumbBg(titles[e.courseId] ?? e.courseId)}>
                    <span className="thumb-cat">▶ 이어듣기</span>
                  </div>
                </Link>
                <div className="cc-body">
                  <Link href={`/courses/${e.courseId}`}><span className="cc-title">{titles[e.courseId] ?? e.courseId.slice(0, 8)}</span></Link>
                  <div className="progress-line">
                    <div className="progress-bar"><span style={{ width: `${e.progress}%` }} /></div>
                    {e.progress}%
                  </div>
                </div>
                <div className="cc-foot">
                  <Link className="button" href={`/learn/${e.courseId}`} style={{ display: "block", textAlign: "center" }}>이어듣기 ▶</Link>
                </div>
              </div>
            ))}
          </div>
        </>
      )}

      {/* 맞춤 추천 */}
      {isLearner && isEnabled("RECOMMENDATIONS") && recs.length > 0 && (
        <>
          <div className="section-head">
            <h2>회원님을 위한 추천</h2>
            <Link href="/me/profile">관심분야 수정 →</Link>
          </div>
          <div className="course-grid">
            {recs.map((r) => (
              <Link className="course-card" href={`/courses/${r.courseId}`} key={r.courseId}>
                <div className="course-thumb" style={thumbBg(r.title)}>
                  <span className="thumb-cat">{courseEmoji(r.categoryCode)} {r.level != null ? levelLabel(r.level) : "추천"}</span>
                </div>
                <div className="cc-body">
                  <span className="cc-title">{r.title}</span>
                  <span className="chip accent" title={r.reason}>✨ {r.reason}</span>
                </div>
              </Link>
            ))}
          </div>
        </>
      )}

      {/* 둘러보기 */}
      {browse.length > 0 && (
        <>
          <div className="section-head">
            <h2>이런 과정은 어때요?</h2>
            <Link href="/courses">전체 보기 →</Link>
          </div>
          <div className="course-grid">
            {browse.map((c) => (
              <Link className="course-card" href={`/courses/${c.id}`} key={c.id}>
                <div className="course-thumb" style={thumbBg(c.title)}>
                  {c.categoryCode && <span className="thumb-cat">{courseEmoji(c.categoryCode)} {c.level != null ? levelLabel(c.level) : ""}</span>}
                </div>
                <div className="cc-body">
                  <span className="cc-title">{c.title}</span>
                  <span className="cc-desc">{c.description ?? "설명이 없습니다."}</span>
                  <div className="cc-meta">
                    {c.tuitionFee > 0
                      ? <span className="chip paid">{c.tuitionFee.toLocaleString("ko-KR")}원</span>
                      : <span className="chip free">무료</span>}
                  </div>
                </div>
              </Link>
            ))}
          </div>
        </>
      )}
    </div>
  );
}
