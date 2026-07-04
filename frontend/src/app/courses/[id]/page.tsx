"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useSession } from "@/components/SessionProvider";
import { useFeatures } from "@/components/FeaturesProvider";
import { useToast } from "@/components/ToastProvider";
import {
  addLesson,
  analyzeInsight,
  Certificate,
  ContentInsight,
  Course,
  createQuiz,
  deleteCourse,
  deleteLesson,
  enroll,
  Enrollment,
  getCourse,
  getCourseCertificate,
  getInsight,
  InterestCategory,
  LEVEL_LABELS,
  Lesson,
  levelLabel,
  listInterestCategories,
  listLessons,
  listQuizzes,
  myEnrollments,
  Quiz,
  updateCourse,
  updateLesson,
  uploadVideo,
  formatMoney,
  myPayments,
  payTuition,
  setCoursePublished,
  setCourseTuition,
} from "@/lib/api";

export default function CourseDetailPage({ params }: { params: { id: string } }) {
  const courseId = params.id;
  const { session } = useSession();
  const { isEnabled, loaded: featuresLoaded } = useFeatures();
  const { showToast } = useToast();
  const router = useRouter();

  const [course, setCourse] = useState<Course | null>(null);
  const [lessons, setLessons] = useState<Lesson[]>([]);
  const [enrollment, setEnrollment] = useState<Enrollment | null>(null);
  const [quizzes, setQuizzes] = useState<Quiz[]>([]);
  const [insight, setInsight] = useState<ContentInsight | null>(null);
  const [cert, setCert] = useState<Certificate | null>(null);
  const [categories, setCategories] = useState<InterestCategory[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [uploading, setUploading] = useState(false);

  // 레슨 추가 폼
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [videoUrl, setVideoUrl] = useState("");
  const [quizTitle, setQuizTitle] = useState("");

  // 과정 수정 폼
  const [editingCourse, setEditingCourse] = useState(false);
  const [cTitle, setCTitle] = useState("");
  const [cDesc, setCDesc] = useState("");
  const [cCat, setCCat] = useState("");
  const [cLevel, setCLevel] = useState(0);

  // 레슨 수정 폼 (인라인)
  const [editLessonId, setEditLessonId] = useState<string | null>(null);
  const [elTitle, setElTitle] = useState("");
  const [elContent, setElContent] = useState("");
  const [elVideo, setElVideo] = useState("");

  const canTeach = !!session && (session.roles.includes("INSTRUCTOR") || session.roles.includes("ADMIN"));
  const canEnroll = !!session && (session.roles.includes("STUDENT") || session.roles.includes("ADMIN"));

  const lessonsOn = isEnabled("LESSONS");
  const enrollmentsOn = isEnabled("ENROLLMENTS");
  const quizzesOn = isEnabled("QUIZZES");
  const aiOn = isEnabled("AI_CURATION");
  const aiCertOn = isEnabled("CERTIFICATES");

  const categoryName = (code: string | null) =>
    code ? categories.find((c) => c.code === code)?.name ?? code : null;

  const load = useCallback(async () => {
    if (!session || !featuresLoaded) return;
    setError(null);
    try {
      setCourse(await getCourse(session.token, courseId));
      setCategories(await listInterestCategories(session.token));
      setLessons(isEnabled("LESSONS") ? await listLessons(session.token, courseId) : []);
      setEnrollment(
        isEnabled("ENROLLMENTS")
          ? (await myEnrollments(session.token)).find((e) => e.courseId === courseId) ?? null
          : null,
      );
      setQuizzes(isEnabled("QUIZZES") ? await listQuizzes(session.token, courseId) : []);
      setInsight(isEnabled("AI_CURATION") ? await getInsight(session.token, courseId) : null);
      setCert(isEnabled("CERTIFICATES") ? await getCourseCertificate(session.token, courseId) : null);
    } catch (e) {
      setError(e instanceof Error ? e.message : "불러오기 실패");
    }
  }, [session, courseId, featuresLoaded, isEnabled]);

  useEffect(() => {
    load();
  }, [load]);

  if (!session) {
    return <p className="notice">세션이 없습니다. <Link href="/login">로그인</Link>하세요.</p>;
  }
  const token = session.token;

  // 영상 업로드 (추가/수정 폼 공용)
  const onUpload = async (file: File | undefined, setUrl: (u: string) => void) => {
    if (!file) return;
    setUploading(true);
    setError(null);
    try {
      const r = await uploadVideo(token, file);
      setUrl(r.url);
      showToast("동영상 업로드 완료");
    } catch (e) {
      setError(e instanceof Error ? e.message : "업로드 실패");
    } finally {
      setUploading(false);
    }
  };

  const onAddLesson = async () => {
    if (!title.trim()) return;
    setBusy(true);
    setError(null);
    try {
      await addLesson(token, courseId, {
        title,
        content: content || undefined,
        videoUrl: videoUrl || undefined,
        orderNo: lessons.length + 1,
      });
      setTitle("");
      setContent("");
      setVideoUrl("");
      showToast("레슨이 추가되었습니다");
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : "레슨 추가 실패");
    } finally {
      setBusy(false);
    }
  };

  const startEditLesson = (l: Lesson) => {
    setEditLessonId(l.id);
    setElTitle(l.title);
    setElContent(l.content ?? "");
    setElVideo(l.videoUrl ?? "");
  };

  const onSaveLesson = async (l: Lesson) => {
    if (!elTitle.trim()) return;
    setBusy(true);
    setError(null);
    try {
      await updateLesson(token, courseId, l.id, {
        title: elTitle,
        content: elContent || undefined,
        videoUrl: elVideo || undefined,
        orderNo: l.orderNo,
      });
      setEditLessonId(null);
      showToast("레슨이 수정되었습니다");
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : "레슨 수정 실패");
    } finally {
      setBusy(false);
    }
  };

  const onDeleteLesson = async (l: Lesson) => {
    if (!window.confirm(`'${l.title}' 레슨을 삭제할까요?`)) return;
    setBusy(true);
    setError(null);
    try {
      await deleteLesson(token, courseId, l.id);
      showToast("레슨이 삭제되었습니다");
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : "레슨 삭제 실패");
    } finally {
      setBusy(false);
    }
  };

  // 순서 변경: 이웃 레슨과 orderNo를 맞바꾼다
  const onMoveLesson = async (idx: number, dir: -1 | 1) => {
    const a = lessons[idx];
    const b = lessons[idx + dir];
    if (!a || !b) return;
    setBusy(true);
    setError(null);
    try {
      await updateLesson(token, courseId, a.id, {
        title: a.title, content: a.content || undefined, videoUrl: a.videoUrl || undefined, orderNo: b.orderNo,
      });
      await updateLesson(token, courseId, b.id, {
        title: b.title, content: b.content || undefined, videoUrl: b.videoUrl || undefined, orderNo: a.orderNo,
      });
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : "순서 변경 실패");
    } finally {
      setBusy(false);
    }
  };

  const startEditCourse = () => {
    if (!course) return;
    setCTitle(course.title);
    setCDesc(course.description ?? "");
    setCCat(course.categoryCode ?? "");
    setCLevel(course.level ?? 0);
    setEditingCourse(true);
  };

  const onSaveCourse = async () => {
    if (!cTitle.trim()) return;
    setBusy(true);
    setError(null);
    try {
      await updateCourse(token, courseId, {
        title: cTitle,
        description: cDesc || undefined,
        categoryCode: cCat || undefined,
        level: cLevel,
      });
      setEditingCourse(false);
      showToast("과정이 수정되었습니다");
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : "과정 수정 실패");
    } finally {
      setBusy(false);
    }
  };

  const onDeleteCourse = async () => {
    if (!window.confirm(`'${course?.title}' 과정을 삭제할까요? 레슨·수강·퀴즈·수료증이 모두 함께 삭제됩니다.`)) return;
    setBusy(true);
    setError(null);
    try {
      await deleteCourse(token, courseId);
      showToast("과정이 삭제되었습니다");
      router.push("/courses");
    } catch (e) {
      setError(e instanceof Error ? e.message : "과정 삭제 실패");
      setBusy(false);
    }
  };

  const onEnroll = async () => {
    setBusy(true);
    setError(null);
    try {
      await enroll(token, courseId);
      showToast("수강신청이 완료되었습니다");
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : "수강신청 실패");
    } finally {
      setBusy(false);
    }
  };

  const onCreateQuiz = async () => {
    if (!quizTitle.trim()) return;
    setBusy(true);
    setError(null);
    try {
      await createQuiz(token, courseId, quizTitle);
      setQuizTitle("");
      showToast("퀴즈가 생성되었습니다");
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : "퀴즈 생성 실패");
    } finally {
      setBusy(false);
    }
  };

  const onAnalyze = async () => {
    setBusy(true);
    setError(null);
    try {
      setInsight(await analyzeInsight(token, courseId));
      showToast("콘텐츠 분석이 완료되었습니다");
    } catch (e) {
      setError(e instanceof Error ? e.message : "분석 실패");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div>
      <p className="muted"><Link href="/courses">← 과정 목록</Link></p>

      <div className="row" style={{ justifyContent: "space-between", alignItems: "flex-start" }}>
        <h1 style={{ margin: 0 }}>{course ? course.title : "과정"}</h1>
        {canTeach && course && !editingCourse && (
          <span className="row">
            <button className="ghost" onClick={startEditCourse} disabled={busy}>과정 수정</button>
            <button className="ghost" onClick={onDeleteCourse} disabled={busy} style={{ color: "var(--danger)" }}>과정 삭제</button>
          </span>
        )}
      </div>

      {course && (course.categoryCode || course.level != null) && !editingCourse && (
        <p className="muted">
          {course.categoryCode && <span className="badge">{categoryName(course.categoryCode)}</span>}
          {course.level != null && <span className="badge">{levelLabel(course.level)}</span>}
        </p>
      )}
      {course?.description && !editingCourse && <p className="muted">{course.description}</p>}

      {course && canTeach && !editingCourse && (
        <CourseAdminControls token={token} courseId={courseId} published={course.published} fee={course.tuitionFee} />
      )}
      {course && course.tuitionFee > 0 && !canTeach && (
        <TuitionCard token={token} courseId={courseId} fee={course.tuitionFee} canPay={canEnroll} />
      )}
      {course && (
        <p><Link className="button ghost" href={`/courses/${courseId}/activity`}>질문 · 과제 · 자료실 열기</Link></p>
      )}

      {/* 과정 수정 폼 */}
      {canTeach && editingCourse && (
        <div className="card" style={{ borderColor: "var(--accent)" }}>
          <label>제목</label>
          <input value={cTitle} onChange={(e) => setCTitle(e.target.value)} />
          <label>설명</label>
          <input value={cDesc} onChange={(e) => setCDesc(e.target.value)} />
          <div className="row">
            <div style={{ flex: 1 }}>
              <label>분야</label>
              <select value={cCat} onChange={(e) => setCCat(e.target.value)}>
                <option value="">— 미지정 —</option>
                {categories.map((c) => <option key={c.code} value={c.code}>{c.name}</option>)}
              </select>
            </div>
            <div style={{ flex: 1 }}>
              <label>난이도</label>
              <select value={cLevel} onChange={(e) => setCLevel(Number(e.target.value))}>
                {LEVEL_LABELS.map((lab, lv) => <option key={lv} value={lv}>{lab}</option>)}
              </select>
            </div>
          </div>
          <div className="row" style={{ marginTop: 14 }}>
            <button onClick={onSaveCourse} disabled={busy}>{busy ? "저장 중…" : "저장"}</button>
            <button className="ghost" onClick={() => setEditingCourse(false)} disabled={busy}>취소</button>
          </div>
        </div>
      )}

      {error && <p className="error">{error}</p>}

      {/* AI 콘텐츠 분석 */}
      {aiOn && (
        <div className="card" style={{ borderColor: "var(--accent)" }}>
          <div className="row" style={{ justifyContent: "space-between" }}>
            <h3 style={{ margin: 0 }}>AI 콘텐츠 분석 <span className="badge role">AI</span></h3>
            {canTeach && (
              <button onClick={onAnalyze} disabled={busy}>
                {busy ? "분석 중…" : insight ? "다시 분석" : "분석 실행"}
              </button>
            )}
          </div>
          {insight ? (
            <div style={{ marginTop: 10 }}>
              <p className="muted" style={{ marginBottom: 6 }}>
                {insight.tags.map((t) => <span key={t} className="badge">{t}</span>)}
                {insight.difficulty != null && <span className="badge">{levelLabel(insight.difficulty)}</span>}
                {insight.estMinutes != null && <span className="badge">~{insight.estMinutes}분</span>}
                <span className="badge" style={{ color: "var(--muted)" }}>{insight.generatedBy}</span>
              </p>
              {insight.summary && <p>{insight.summary}</p>}
            </div>
          ) : (
            <p className="muted" style={{ marginTop: 8 }}>
              {canTeach ? "아직 분석되지 않았습니다. ‘분석 실행’을 눌러 태그·난이도·요약을 생성하세요." : "아직 분석 정보가 없습니다."}
            </p>
          )}
        </div>
      )}

      {/* 레슨 모듈 */}
      {lessonsOn && (
        <>
          <div className="row" style={{ justifyContent: "space-between" }}>
            <h2 style={{ margin: 0 }}>레슨 ({lessons.length})</h2>
            <span className="row" style={{ width: "auto" }}>
              <Link className="button ghost" href={`/courses/${courseId}/activity`}>질문·과제</Link>
              {enrollment && lessons.length > 0 && (
                <Link className="button" href={`/learn/${courseId}`}>학습창 열기 ▶</Link>
              )}
            </span>
          </div>
          {lessons.length === 0 && <p className="notice">아직 레슨이 없습니다.</p>}
          {lessons.map((l, idx) => (
            <div className="card" key={l.id}>
              {editLessonId === l.id ? (
                <>
                  <label>제목</label>
                  <input value={elTitle} onChange={(e) => setElTitle(e.target.value)} />
                  <label>내용</label>
                  <textarea rows={2} value={elContent} onChange={(e) => setElContent(e.target.value)} />
                  <label>동영상 URL</label>
                  <input value={elVideo} onChange={(e) => setElVideo(e.target.value)} placeholder="비우면 샘플 영상" />
                  <label>또는 동영상 파일 업로드</label>
                  <input type="file" accept="video/*" disabled={uploading}
                    onChange={(e) => onUpload(e.target.files?.[0], setElVideo)} />
                  {uploading && <p className="muted">업로드 중…</p>}
                  <div className="row" style={{ marginTop: 12 }}>
                    <button onClick={() => onSaveLesson(l)} disabled={busy || uploading}>저장</button>
                    <button className="ghost" onClick={() => setEditLessonId(null)} disabled={busy}>취소</button>
                  </div>
                </>
              ) : (
                <>
                  <div className="row" style={{ justifyContent: "space-between" }}>
                    <h3 style={{ margin: 0 }}>
                      {l.orderNo}. {l.title}{" "}
                      {l.videoUrl ? <span className="badge">🎬 영상</span> : <span className="badge" style={{ color: "var(--muted)" }}>샘플 영상</span>}
                    </h3>
                    {canTeach && (
                      <span className="row">
                        <button className="ghost" disabled={busy || idx === 0} onClick={() => onMoveLesson(idx, -1)} title="위로">▲</button>
                        <button className="ghost" disabled={busy || idx === lessons.length - 1} onClick={() => onMoveLesson(idx, 1)} title="아래로">▼</button>
                        <button className="ghost" disabled={busy} onClick={() => startEditLesson(l)}>수정</button>
                        <button className="ghost" disabled={busy} onClick={() => onDeleteLesson(l)} style={{ color: "var(--danger)" }}>삭제</button>
                      </span>
                    )}
                  </div>
                  {l.content && <p className="muted">{l.content}</p>}
                </>
              )}
            </div>
          ))}

          {canTeach && (
            <div className="card">
              <label>레슨 추가 <span className="badge role">INSTRUCTOR/ADMIN</span></label>
              <input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="제목 (예: 1강 - 소개)" />
              <textarea rows={3} value={content} onChange={(e) => setContent(e.target.value)} placeholder="내용" />
              <input value={videoUrl} onChange={(e) => setVideoUrl(e.target.value)} placeholder="동영상 URL (비우면 샘플 영상)" style={{ marginTop: 8 }} />
              <label>또는 동영상 파일 업로드</label>
              <input type="file" accept="video/*" disabled={uploading}
                onChange={(e) => onUpload(e.target.files?.[0], setVideoUrl)} />
              {uploading && <p className="muted">업로드 중…</p>}
              {videoUrl && <p className="muted">설정된 영상: {videoUrl}</p>}
              <div style={{ marginTop: 14 }}>
                <button onClick={onAddLesson} disabled={busy || uploading}>{busy ? "추가 중…" : "레슨 추가"}</button>
              </div>
            </div>
          )}
        </>
      )}

      {/* 수강 모듈 */}
      {enrollmentsOn && (
        <>
          <h2>수강</h2>
          {enrollment ? (
            <div className="card">
              <p>수강 중 · 상태 <span className="badge">{enrollment.status}</span> · 진도 {enrollment.progress}%</p>
              <div className="progress-bar"><span style={{ width: `${enrollment.progress}%` }} /></div>
              <p className="muted" style={{ marginTop: 8 }}>
                진도 조정·수강 취소는 <Link href="/my-learning">내 학습</Link>에서.
              </p>
              {aiCertOn && (
                cert ? (
                  <p style={{ marginTop: 8, color: "var(--accent-2)" }}>
                    🎓 <b>수료 완료</b> · 수료번호 <span className="badge">{cert.certificateNo}</span>
                    <Link href={`/certificates/${cert.id}`} style={{ marginLeft: 8 }}>수료증 보기·출력</Link>
                  </p>
                ) : (
                  <p className="muted" style={{ marginTop: 8 }}>
                    수료 조건: 진도 100% + 과정의 모든 퀴즈 통과(60%↑)
                  </p>
                )
              )}
            </div>
          ) : canEnroll ? (
            <div className="card">
              <p className="muted">아직 수강신청하지 않았습니다.</p>
              <button className="success" onClick={onEnroll} disabled={busy}>
                {busy ? "신청 중…" : "수강신청"}
              </button>
            </div>
          ) : (
            <p className="muted">수강신청은 STUDENT 역할이 필요합니다. (현재: {session.roles.join(", ")})</p>
          )}
        </>
      )}

      {/* 퀴즈 모듈 */}
      {quizzesOn && (
        <>
          <h2>퀴즈 ({quizzes.length})</h2>
          {quizzes.length === 0 && <p className="notice">아직 퀴즈가 없습니다.</p>}
          {quizzes.map((q) => (
            <div className="card" key={q.id}>
              <h3><Link href={`/quizzes/${q.id}`}>{q.title}</Link></h3>
              <p className="muted">응시하거나 문항을 추가하려면 클릭하세요.</p>
            </div>
          ))}

          {canTeach && (
            <div className="card">
              <label>새 퀴즈 제목 <span className="badge role">INSTRUCTOR/ADMIN</span></label>
              <input value={quizTitle} onChange={(e) => setQuizTitle(e.target.value)} placeholder="예: 1주차 퀴즈" />
              <div style={{ marginTop: 12 }}>
                <button onClick={onCreateQuiz} disabled={busy}>{busy ? "생성 중…" : "퀴즈 생성"}</button>
              </div>
            </div>
          )}
        </>
      )}

      {featuresLoaded && !lessonsOn && !enrollmentsOn && !quizzesOn && (
        <p className="notice">이 기관에서 활성화된 학습 모듈이 없습니다. 관리자가 “기능 설정”에서 켤 수 있습니다.</p>
      )}
    </div>
  );
}

// 강사/관리자용 강의 노출·수강료 관리 카드.
function CourseAdminControls({ token, courseId, published, fee }:
  { token: string; courseId: string; published: boolean; fee: number }) {
  const [pub, setPub] = useState(published);
  const [feeInput, setFeeInput] = useState(String(fee));
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState<string | null>(null);

  const onTogglePublish = async () => {
    setBusy(true); setMsg(null);
    try { const c = await setCoursePublished(token, courseId, !pub); setPub(c.published); setMsg(c.published ? "공개로 전환" : "비공개로 전환"); }
    catch (e) { setMsg(e instanceof Error ? e.message : "실패"); } finally { setBusy(false); }
  };
  const onSaveFee = async () => {
    setBusy(true); setMsg(null);
    try { await setCourseTuition(token, courseId, Number(feeInput) || 0); setMsg("수강료 저장됨"); }
    catch (e) { setMsg(e instanceof Error ? e.message : "실패"); } finally { setBusy(false); }
  };

  return (
    <div className="card" style={{ borderColor: "var(--border)" }}>
      <div className="row" style={{ justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: 10 }}>
        <span className="row" style={{ gap: 8, alignItems: "center" }}>
          <b>노출</b>
          <span className={pub ? "pf-pill paid" : "pf-pill issued"}>{pub ? "공개" : "비공개"}</span>
          <button className="ghost" onClick={onTogglePublish} disabled={busy}>{pub ? "비공개로" : "공개로"}</button>
        </span>
        <span className="row" style={{ gap: 6, alignItems: "center" }}>
          <b>수강료</b>
          <input type="number" value={feeInput} onChange={(e) => setFeeInput(e.target.value)} style={{ width: 120 }} />
          <span className="muted">원</span>
          <button className="ghost" onClick={onSaveFee} disabled={busy}>저장</button>
        </span>
      </div>
      {msg && <p className="muted" style={{ margin: "6px 0 0" }}>{msg}</p>}
    </div>
  );
}

// 수강료 결제 카드 (유료 강의). 이미 결제했으면 완료 표시.
function TuitionCard({ token, courseId, fee, canPay }: { token: string; courseId: string; fee: number; canPay: boolean }) {
  const [paid, setPaid] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(async () => {
    try { const ps = await myPayments(token); setPaid(ps.some((p) => p.courseId === courseId && p.status === "PAID")); }
    catch { /* 무시 */ }
  }, [token, courseId]);
  useEffect(() => { reload(); }, [reload]);

  const onPay = async () => {
    setBusy(true); setError(null);
    try { await payTuition(token, courseId); setPaid(true); }
    catch (e) { setError(e instanceof Error ? e.message : "결제 실패"); }
    finally { setBusy(false); }
  };

  return (
    <div className="card">
      <div className="row" style={{ justifyContent: "space-between", alignItems: "center" }}>
        <div><b>수강료</b> <span className="pf-money" style={{ fontWeight: 700 }}>{formatMoney(fee)}</span></div>
        {paid ? <span className="pf-pill paid">결제완료</span>
          : canPay ? <button className="success" onClick={onPay} disabled={busy}>{busy ? "결제 중…" : "수강료 결제"}</button>
          : <span className="muted">학생 계정으로 결제 가능</span>}
      </div>
      {error && <p className="error" style={{ marginBottom: 0 }}>{error}</p>}
    </div>
  );
}
