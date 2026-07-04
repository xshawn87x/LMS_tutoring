"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useSession } from "@/components/SessionProvider";
import { useToast } from "@/components/ToastProvider";
import {
  Assignment,
  AssignmentSubmission,
  Material,
  Notice,
  QuestionSummary,
  QuestionThread,
  addMaterial,
  answerQuestion,
  askQuestion,
  createAssignment,
  createCourseNotice,
  deleteMaterial,
  getQuestionThread,
  gradeSubmission,
  listAssignmentSubmissions,
  listAssignments,
  listCourseNotices,
  listMaterials,
  listQuestions,
  myAssignmentSubmission,
  resolveQuestion,
  resolveMediaUrl,
  submitAssignment,
  uploadFile,
} from "@/lib/api";

export default function CourseActivityPage() {
  const { id: courseId } = useParams<{ id: string }>();
  const { session } = useSession();
  const { showToast } = useToast();
  const [tab, setTab] = useState<"qna" | "assignments" | "notices" | "materials">("qna");

  if (!session) return <p className="notice">세션이 없습니다. <Link href="/login">로그인</Link>하세요.</p>;
  const isTeacher = session.roles.includes("INSTRUCTOR") || session.roles.includes("ADMIN");
  const isStudent = session.roles.includes("STUDENT");
  const tabs: { k: typeof tab; l: string }[] = [
    { k: "qna", l: "Q&A 질문" }, { k: "assignments", l: "과제" }, { k: "notices", l: "강의 공지" }, { k: "materials", l: "자료실" },
  ];

  return (
    <div>
      <h1>강의 활동</h1>
      <p className="muted"><Link href={`/courses/${courseId}`}>← 강의로 돌아가기</Link></p>
      <div className="row" style={{ marginBottom: 12 }}>
        {tabs.map((t) => <button key={t.k} className={tab === t.k ? "" : "ghost"} onClick={() => setTab(t.k)}>{t.l}</button>)}
      </div>
      {tab === "qna" && <QnaSection courseId={courseId} token={session.token} isTeacher={isTeacher} showToast={showToast} />}
      {tab === "assignments" && <AssignmentsSection courseId={courseId} token={session.token} isTeacher={isTeacher} isStudent={isStudent} showToast={showToast} />}
      {tab === "notices" && <CourseNoticesSection courseId={courseId} token={session.token} isTeacher={isTeacher} showToast={showToast} />}
      {tab === "materials" && <MaterialsSection courseId={courseId} token={session.token} isTeacher={isTeacher} showToast={showToast} />}
    </div>
  );
}

function CourseNoticesSection({ courseId, token, isTeacher, showToast }:
  { courseId: string; token: string; isTeacher: boolean; showToast: (m: string) => void }) {
  const [items, setItems] = useState<Notice[]>([]);
  const [title, setTitle] = useState(""); const [body, setBody] = useState("");
  const [err, setErr] = useState<string | null>(null);
  const reload = useCallback(async () => { try { setItems(await listCourseNotices(token, courseId)); } catch (e) { setErr(String(e)); } }, [token, courseId]);
  useEffect(() => { reload(); }, [reload]);
  const onAdd = async () => {
    if (!title.trim()) return;
    try { await createCourseNotice(token, courseId, { title, body, pinned: false }); showToast("공지를 등록했습니다"); setTitle(""); setBody(""); await reload(); }
    catch (e) { setErr(e instanceof Error ? e.message : "실패"); }
  };
  return (
    <div>
      {err && <p className="error">{err}</p>}
      {isTeacher && (
        <div className="card">
          <h3>강의 공지 작성</h3>
          <label>제목</label><input value={title} onChange={(e) => setTitle(e.target.value)} />
          <label>내용</label><textarea rows={3} value={body} onChange={(e) => setBody(e.target.value)} />
          <div style={{ marginTop: 10 }}><button onClick={onAdd} disabled={!title.trim()}>등록</button></div>
        </div>
      )}
      {items.length === 0 ? <p className="notice">등록된 강의 공지가 없습니다.</p> : items.map((n) => (
        <div className="card" key={n.id}>
          <b>{n.pinned && "📌 "}{n.title}</b>
          {n.body && <p style={{ whiteSpace: "pre-wrap", marginBottom: 4 }}>{n.body}</p>}
          <p className="muted" style={{ margin: 0, fontSize: 12 }}>{n.author ?? ""} · {new Date(n.createdAt).toLocaleString("ko-KR")}</p>
        </div>
      ))}
    </div>
  );
}

function MaterialsSection({ courseId, token, isTeacher, showToast }:
  { courseId: string; token: string; isTeacher: boolean; showToast: (m: string) => void }) {
  const [items, setItems] = useState<Material[]>([]);
  const [title, setTitle] = useState(""); const [uploading, setUploading] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const reload = useCallback(async () => { try { setItems(await listMaterials(token, courseId)); } catch (e) { setErr(String(e)); } }, [token, courseId]);
  useEffect(() => { reload(); }, [reload]);
  const onUpload = async (file: File | undefined) => {
    if (!file) return;
    setUploading(true); setErr(null);
    try {
      const up = await uploadFile(token, file);
      await addMaterial(token, courseId, { title: title.trim() || file.name, fileUrl: up.url });
      showToast("자료를 올렸습니다"); setTitle(""); await reload();
    } catch (e) { setErr(e instanceof Error ? e.message : "업로드 실패"); } finally { setUploading(false); }
  };
  return (
    <div>
      {err && <p className="error">{err}</p>}
      {isTeacher && (
        <div className="card">
          <h3>자료 올리기</h3>
          <label>제목(선택)</label><input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="비우면 파일명 사용" />
          <label>파일 (pdf·이미지·문서 등)</label>
          <input type="file" onChange={(e) => onUpload(e.target.files?.[0])} disabled={uploading} />
          {uploading && <p className="muted">업로드 중…</p>}
        </div>
      )}
      {items.length === 0 ? <p className="notice">등록된 자료가 없습니다.</p> : (
        <table className="grid">
          <thead><tr><th>제목</th><th>등록</th><th style={{ textAlign: "right" }}></th></tr></thead>
          <tbody>
            {items.map((m) => (
              <tr key={m.id}>
                <td><a href={resolveMediaUrl(m.fileUrl) ?? m.fileUrl} target="_blank" rel="noreferrer">⬇ {m.title}</a></td>
                <td className="muted">{new Date(m.createdAt).toLocaleDateString("ko-KR")}</td>
                <td style={{ textAlign: "right" }}>{isTeacher && <button className="ghost" onClick={async () => { await deleteMaterial(token, m.id); await reload(); }}>삭제</button>}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

function QnaSection({ courseId, token, isTeacher, showToast }:
  { courseId: string; token: string; isTeacher: boolean; showToast: (m: string) => void }) {
  const [questions, setQuestions] = useState<QuestionSummary[]>([]);
  const [openThread, setOpenThread] = useState<QuestionThread | null>(null);
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [answerText, setAnswerText] = useState("");
  const [err, setErr] = useState<string | null>(null);

  const reload = useCallback(async () => {
    try { setQuestions(await listQuestions(token, courseId)); } catch (e) { setErr(e instanceof Error ? e.message : "실패"); }
  }, [token, courseId]);
  useEffect(() => { reload(); }, [reload]);

  const onAsk = async () => {
    if (!title.trim()) return;
    try { await askQuestion(token, courseId, { title, body }); showToast("질문을 등록했습니다"); setTitle(""); setBody(""); await reload(); }
    catch (e) { setErr(e instanceof Error ? e.message : "실패"); }
  };
  const openQ = async (id: string) => { try { setOpenThread(await getQuestionThread(token, id)); } catch (e) { setErr(String(e)); } };
  const onAnswer = async () => {
    if (!openThread || !answerText.trim()) return;
    try { await answerQuestion(token, openThread.id, answerText); setAnswerText(""); await openQ(openThread.id); await reload(); showToast("답변을 등록했습니다"); }
    catch (e) { setErr(e instanceof Error ? e.message : "실패"); }
  };
  const onResolve = async () => {
    if (!openThread) return;
    try { const t = await resolveQuestion(token, openThread.id, !openThread.resolved); setOpenThread(t); await reload(); }
    catch (e) { setErr(e instanceof Error ? e.message : "실패"); }
  };

  return (
    <div>
      {err && <p className="error">{err}</p>}
      <div className="card">
        <h3>질문하기</h3>
        <label>제목</label>
        <input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="무엇이 궁금한가요?" />
        <label>내용</label>
        <textarea rows={3} value={body} onChange={(e) => setBody(e.target.value)} />
        <div style={{ marginTop: 10 }}><button onClick={onAsk} disabled={!title.trim()}>질문 등록</button></div>
      </div>

      {questions.length === 0 ? <p className="notice">등록된 질문이 없습니다.</p> : questions.map((q) => (
        <div className="card" key={q.id}>
          <div className="row" style={{ justifyContent: "space-between" }}>
            <b>{q.resolved && "✅ "}{q.title}</b>
            <button className="ghost" onClick={() => openQ(q.id)}>보기 ({q.answerCount})</button>
          </div>
          <p className="muted" style={{ margin: "4px 0 0", fontSize: 12 }}>{q.author} · {new Date(q.createdAt).toLocaleString("ko-KR")}</p>
          {openThread?.id === q.id && (
            <div style={{ marginTop: 10, borderTop: "1px solid var(--border)", paddingTop: 10 }}>
              {openThread.body && <p style={{ whiteSpace: "pre-wrap" }}>{openThread.body}</p>}
              {openThread.answers.map((a) => (
                <div key={a.id} className="card" style={{ background: "var(--panel-2)" }}>
                  <p style={{ margin: 0, whiteSpace: "pre-wrap" }}>{a.body}</p>
                  <p className="muted" style={{ margin: "4px 0 0", fontSize: 12 }}>{a.author} · {new Date(a.createdAt).toLocaleString("ko-KR")}</p>
                </div>
              ))}
              {isTeacher && (
                <div style={{ marginTop: 8 }}>
                  <textarea rows={2} value={answerText} onChange={(e) => setAnswerText(e.target.value)} placeholder="답변 작성" />
                  <div className="row" style={{ marginTop: 8 }}>
                    <button onClick={onAnswer} disabled={!answerText.trim()}>답변 등록</button>
                    <button className="ghost" onClick={onResolve}>{openThread.resolved ? "미해결로" : "해결됨으로"}</button>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      ))}
    </div>
  );
}

function AssignmentsSection({ courseId, token, isTeacher, isStudent, showToast }:
  { courseId: string; token: string; isTeacher: boolean; isStudent: boolean; showToast: (m: string) => void }) {
  const [assignments, setAssignments] = useState<Assignment[]>([]);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [maxScore, setMaxScore] = useState("100");
  const [err, setErr] = useState<string | null>(null);

  const reload = useCallback(async () => {
    try { setAssignments(await listAssignments(token, courseId)); } catch (e) { setErr(e instanceof Error ? e.message : "실패"); }
  }, [token, courseId]);
  useEffect(() => { reload(); }, [reload]);

  const onCreate = async () => {
    if (!title.trim()) return;
    try { await createAssignment(token, courseId, { title, description, maxScore: Number(maxScore) || 100 }); showToast("과제를 등록했습니다"); setTitle(""); setDescription(""); await reload(); }
    catch (e) { setErr(e instanceof Error ? e.message : "실패"); }
  };

  return (
    <div>
      {err && <p className="error">{err}</p>}
      {isTeacher && (
        <div className="card">
          <h3>과제 등록</h3>
          <label>제목</label>
          <input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="과제 제목" />
          <label>설명</label>
          <textarea rows={3} value={description} onChange={(e) => setDescription(e.target.value)} />
          <label>만점</label>
          <input type="number" style={{ width: 120 }} value={maxScore} onChange={(e) => setMaxScore(e.target.value)} />
          <div style={{ marginTop: 10 }}><button onClick={onCreate} disabled={!title.trim()}>등록</button></div>
        </div>
      )}
      {assignments.length === 0 ? <p className="notice">등록된 과제가 없습니다.</p> :
        assignments.map((a) => (
          <AssignmentCard key={a.id} a={a} token={token} isTeacher={isTeacher} isStudent={isStudent} showToast={showToast} />
        ))}
    </div>
  );
}

function AssignmentCard({ a, token, isTeacher, isStudent, showToast }:
  { a: Assignment; token: string; isTeacher: boolean; isStudent: boolean; showToast: (m: string) => void }) {
  const [mine, setMine] = useState<AssignmentSubmission | null>(null);
  const [subs, setSubs] = useState<AssignmentSubmission[]>([]);
  const [text, setText] = useState("");
  const [fileUrl, setFileUrl] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (isStudent) {
      try { setMine(await myAssignmentSubmission(token, a.id)); } catch { setMine(null); }
    }
    if (isTeacher) {
      try { setSubs(await listAssignmentSubmissions(token, a.id)); } catch { /* */ }
    }
  }, [a.id, token, isStudent, isTeacher]);
  useEffect(() => { load(); }, [load]);

  const onUpload = async (file: File | undefined) => {
    if (!file) return;
    setUploading(true); setErr(null);
    try { setFileUrl((await uploadFile(token, file)).url); }
    catch (e) { setErr(e instanceof Error ? e.message : "업로드 실패"); } finally { setUploading(false); }
  };
  const onSubmit = async () => {
    if (!text.trim() && !fileUrl) return;
    try { const s = await submitAssignment(token, a.id, { textAnswer: text, fileUrl: fileUrl ?? undefined }); setMine(s); setText(""); setFileUrl(null); showToast("제출했습니다"); }
    catch (e) { setErr(e instanceof Error ? e.message : "실패"); }
  };
  const onGrade = async (s: AssignmentSubmission) => {
    const sc = prompt(`점수 (0~${a.maxScore})`, s.score != null ? String(s.score) : "");
    if (sc == null) return;
    const fb = prompt("피드백", s.feedback ?? "") ?? "";
    try { await gradeSubmission(token, s.id, { score: Number(sc), feedback: fb }); showToast("채점했습니다"); await load(); }
    catch (e) { setErr(e instanceof Error ? e.message : "실패"); }
  };

  return (
    <div className="card">
      <div className="row" style={{ justifyContent: "space-between" }}>
        <b>{a.title}</b><span className="muted">만점 {a.maxScore}</span>
      </div>
      {a.description && <p style={{ whiteSpace: "pre-wrap" }}>{a.description}</p>}
      {err && <p className="error">{err}</p>}

      {isStudent && (
        <div style={{ borderTop: "1px solid var(--border)", paddingTop: 10, marginTop: 8 }}>
          {mine ? (
            <div>
              <p className="muted" style={{ margin: 0, fontSize: 12 }}>제출: {new Date(mine.submittedAt).toLocaleString("ko-KR")}</p>
              <p style={{ whiteSpace: "pre-wrap" }}>{mine.textAnswer}</p>
              {mine.fileUrl && <p style={{ margin: "4px 0" }}><a href={resolveMediaUrl(mine.fileUrl) ?? mine.fileUrl} target="_blank" rel="noreferrer">📎 첨부파일</a></p>}
              {mine.score != null
                ? <p className="pf-pill paid" style={{ display: "inline-block" }}>채점 {mine.score}/{a.maxScore}{mine.feedback ? ` · ${mine.feedback}` : ""}</p>
                : <p className="muted">채점 대기 중</p>}
            </div>
          ) : null}
          <textarea rows={2} value={text} onChange={(e) => setText(e.target.value)} placeholder={mine ? "재제출 내용" : "답안 작성"} />
          <div className="row" style={{ marginTop: 8, gap: 8, alignItems: "center" }}>
            <input type="file" onChange={(e) => onUpload(e.target.files?.[0])} disabled={uploading} />
            {fileUrl && <span className="pf-pill paid">첨부됨</span>}
            <button onClick={onSubmit} disabled={(!text.trim() && !fileUrl) || uploading}>{mine ? "재제출" : "제출"}</button>
          </div>
        </div>
      )}

      {isTeacher && (
        <div style={{ borderTop: "1px solid var(--border)", paddingTop: 10, marginTop: 8 }}>
          <p className="muted" style={{ marginTop: 0 }}>제출 {subs.length}건</p>
          {subs.map((s) => (
            <div key={s.id} className="row" style={{ justifyContent: "space-between", borderBottom: "1px solid var(--border)", padding: "6px 0" }}>
              <span>{s.student} {s.fileUrl && <a href={resolveMediaUrl(s.fileUrl) ?? s.fileUrl} target="_blank" rel="noreferrer">📎</a>} {s.score != null && <span className="pf-pill paid">{s.score}점</span>}</span>
              <button className="ghost" onClick={() => onGrade(s)}>채점</button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
