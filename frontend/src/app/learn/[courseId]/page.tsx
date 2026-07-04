"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useSession } from "@/components/SessionProvider";
import { useToast } from "@/components/ToastProvider";
import {
  Course,
  Lesson,
  LessonProgress,
  Quiz,
  SAMPLE_VIDEO,
  getCourse,
  getLessonProgress,
  listLessons,
  listQuizzes,
  resolveMediaUrl,
  saveLessonProgress,
} from "@/lib/api";

export default function LearnPage() {
  const { courseId } = useParams<{ courseId: string }>();
  const { session } = useSession();
  const { showToast } = useToast();
  const videoRef = useRef<HTMLVideoElement>(null);

  const [course, setCourse] = useState<Course | null>(null);
  const [lessons, setLessons] = useState<Lesson[]>([]);
  const [progress, setProgress] = useState<Record<string, LessonProgress>>({});
  const [quizzes, setQuizzes] = useState<Quiz[]>([]);
  const [currentId, setCurrentId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loaded, setLoaded] = useState(false);

  // timeupdate 저장 쓰로틀용 — 마지막 저장 시각
  const lastSavedAt = useRef(0);

  const load = useCallback(async () => {
    if (!session) return;
    setError(null);
    try {
      const [c, ls, ps] = await Promise.all([
        getCourse(session.token, courseId),
        listLessons(session.token, courseId),
        getLessonProgress(session.token, courseId),
      ]);
      setCourse(c);
      setLessons(ls);
      const pmap = Object.fromEntries(ps.map((p) => [p.lessonId, p]));
      setProgress(pmap);
      // 이어듣기: 첫 미완료 레슨을 기본 선택 (없으면 첫 레슨)
      const firstIncomplete = ls.find((l) => !pmap[l.id]?.completed) ?? ls[0];
      setCurrentId((prev) => prev ?? firstIncomplete?.id ?? null);
      // 퀴즈는 선택 기능 — 비활성 기관(403)이면 조용히 건너뛴다
      try {
        setQuizzes(await listQuizzes(session.token, courseId));
      } catch {
        setQuizzes([]);
      }
      setLoaded(true);
    } catch (e) {
      setError(e instanceof Error ? e.message : "불러오기 실패");
      setLoaded(true);
    }
  }, [session, courseId]);

  useEffect(() => {
    load();
  }, [load]);

  const current = lessons.find((l) => l.id === currentId) ?? null;
  const currentProgress = current ? progress[current.id] : undefined;

  // 진도 저장 (서버가 수강 진도율을 재계산). 완료 토글 시 토스트 + 목록 갱신.
  const persist = useCallback(
    async (lesson: Lesson, position: number, completed: boolean) => {
      if (!session) return;
      try {
        const saved = await saveLessonProgress(session.token, lesson.id, {
          lastPositionSeconds: Math.floor(position),
          completed,
        });
        setProgress((prev) => ({ ...prev, [lesson.id]: saved }));
        if (completed && !progress[lesson.id]?.completed) {
          showToast(`'${lesson.title}' 수강 완료로 기록되었습니다 ✅`);
        }
      } catch (e) {
        setError(e instanceof Error ? e.message : "진도 저장 실패");
      }
    },
    [session, progress, showToast],
  );

  // 레슨 전환 시 현재 위치를 먼저 저장한다.
  const switchTo = (lessonId: string) => {
    const v = videoRef.current;
    if (v && current && !v.ended) {
      void persist(current, v.currentTime, currentProgress?.completed ?? false);
    }
    setCurrentId(lessonId);
  };

  // 메타데이터 로드 시 마지막 위치로 점프 (이어듣기)
  const handleLoadedMetadata = () => {
    const v = videoRef.current;
    if (!v || !current) return;
    const last = progress[current.id]?.lastPositionSeconds ?? 0;
    if (last > 0 && last < v.duration - 1) v.currentTime = last;
  };

  // 재생 중 10초마다 위치 저장 (이어듣기 보장)
  const handleTimeUpdate = () => {
    const v = videoRef.current;
    if (!v || !current) return;
    const now = Date.now();
    if (now - lastSavedAt.current > 10000) {
      lastSavedAt.current = now;
      void persist(current, v.currentTime, currentProgress?.completed ?? false);
    }
  };

  // 영상이 끝나면 완료 처리 후 다음 레슨으로
  const handleEnded = () => {
    if (!current) return;
    void persist(current, 0, true).then(() => {
      const idx = lessons.findIndex((l) => l.id === current.id);
      const next = lessons[idx + 1];
      if (next) setCurrentId(next.id);
    });
  };

  // 일시정지/이탈 시 현재 위치 저장
  const handlePause = () => {
    const v = videoRef.current;
    if (v && current && !v.ended) {
      void persist(current, v.currentTime, currentProgress?.completed ?? false);
    }
  };

  if (!session) {
    return <p className="notice">세션이 없습니다. <Link href="/login">로그인</Link>하세요.</p>;
  }
  if (loaded && error && !course) {
    return (
      <div>
        <p className="error">{error}</p>
        <p className="notice"><Link href="/my-learning">← 내 학습으로</Link></p>
      </div>
    );
  }
  if (!loaded || !course) return <p className="notice">불러오는 중…</p>;

  const completedCount = lessons.filter((l) => progress[l.id]?.completed).length;
  const pct = lessons.length ? Math.round((completedCount / lessons.length) * 100) : 0;

  return (
    <div>
      <p className="muted"><Link href="/my-learning">← 내 학습</Link></p>
      <h1>{course.title}</h1>
      <p className="muted">
        진도 {pct}% · {completedCount}/{lessons.length} 레슨 완료 · 영상이 끝나면 자동으로 완료 처리됩니다.
      </p>
      <div className="progress-bar" style={{ marginBottom: 16 }}><span style={{ width: `${pct}%` }} /></div>

      {error && <p className="error">{error}</p>}
      {lessons.length === 0 && <p className="notice">아직 등록된 레슨이 없습니다.</p>}

      {current && (
        <div className="learn-layout">
          <div>
            <video
              ref={videoRef}
              key={current.id}
              src={resolveMediaUrl(current.videoUrl) ?? SAMPLE_VIDEO}
              controls
              width="100%"
              style={{ borderRadius: 12, background: "#000", aspectRatio: "16 / 9" }}
              onLoadedMetadata={handleLoadedMetadata}
              onTimeUpdate={handleTimeUpdate}
              onEnded={handleEnded}
              onPause={handlePause}
            />
            <div className="card" style={{ marginTop: 14 }}>
              <h3>
                {current.title}{" "}
                {currentProgress?.completed && <span className="badge">완료</span>}
              </h3>
              {current.content && <p className="muted">{current.content}</p>}
              {!current.videoUrl && (
                <p className="notice">※ 이 레슨엔 동영상이 없어 샘플 영상으로 대체 재생합니다.</p>
              )}
            </div>
          </div>

          <aside>
            <h3 style={{ marginTop: 0 }}>레슨 목록</h3>
            {lessons.map((l) => {
              const done = progress[l.id]?.completed;
              const active = l.id === currentId;
              return (
                <button
                  key={l.id}
                  className={active ? "" : "ghost"}
                  style={{ width: "100%", textAlign: "left", marginBottom: 8 }}
                  onClick={() => switchTo(l.id)}
                >
                  {done ? "✅ " : "▷ "} {l.orderNo}. {l.title}
                </button>
              );
            })}
          </aside>
        </div>
      )}

      {quizzes.length > 0 && (
        <div style={{ marginTop: 20 }}>
          <h2>퀴즈 ({quizzes.length})</h2>
          <p className="muted">레슨을 다 들었다면 퀴즈로 이해도를 확인하세요. 모든 퀴즈를 통과(60%↑)하면 수료 조건을 채웁니다.</p>
          {quizzes.map((q) => (
            <div className="card" key={q.id} style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <span>{q.title}</span>
              <Link className="button" href={`/quizzes/${q.id}`}>응시하기 ▶</Link>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
