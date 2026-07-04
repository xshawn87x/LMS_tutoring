"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useSession } from "@/components/SessionProvider";
import { useToast } from "@/components/ToastProvider";
import {
  Exam,
  ScoreEntryInput,
  createExam,
  deleteExam,
  examScores,
  listExams,
  recordScores,
  sendReport,
} from "@/lib/api";

type Row = { studentSubject: string; score: string; comment: string };

export default function ExamsAdminPage() {
  const { session } = useSession();
  const { showToast } = useToast();
  const [exams, setExams] = useState<Exam[]>([]);
  const [selected, setSelected] = useState<Exam | null>(null);
  const [rows, setRows] = useState<Row[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  // 새 시험
  const [title, setTitle] = useState("");
  const [subject, setSubject] = useState("");
  const [examDate, setExamDate] = useState("");
  const [maxScore, setMaxScore] = useState("100");

  const loadExams = useCallback(async () => {
    if (!session) return;
    try { setExams(await listExams(session.token)); }
    catch (e) { setError(e instanceof Error ? e.message : "불러오기 실패"); }
  }, [session]);

  useEffect(() => { loadExams(); }, [loadExams]);

  const openExam = async (ex: Exam) => {
    setSelected(ex); setError(null);
    if (!session) return;
    try {
      const scores = await examScores(session.token, ex.id);
      setRows(scores.length
        ? scores.map((s) => ({ studentSubject: s.studentSubject, score: String(s.score), comment: s.comment ?? "" }))
        : [{ studentSubject: "", score: "", comment: "" }]);
    } catch (e) { setError(e instanceof Error ? e.message : "성적 불러오기 실패"); }
  };

  if (!session) return <p className="notice">세션이 없습니다. <Link href="/login">로그인</Link>하세요.</p>;
  if (!session.roles.includes("INSTRUCTOR") && !session.roles.includes("ADMIN")) {
    return <div><h1>시험·성적</h1><p className="error">INSTRUCTOR/ADMIN만 접근할 수 있습니다.</p></div>;
  }

  const onCreate = async () => {
    if (!title.trim() || !examDate) { setError("시험명과 시행일을 입력하세요"); return; }
    setBusy(true); setError(null);
    try {
      await createExam(session.token, { title, subject: subject || undefined, examDate, maxScore: Number(maxScore) || 100 });
      showToast("시험을 만들었습니다");
      setTitle(""); setSubject(""); setExamDate(""); setMaxScore("100");
      await loadExams();
    } catch (e) { setError(e instanceof Error ? e.message : "생성 실패"); }
    finally { setBusy(false); }
  };

  const onDeleteExam = async (ex: Exam) => {
    if (!confirm(`'${ex.title}' 시험과 성적을 삭제할까요?`)) return;
    try {
      await deleteExam(session.token, ex.id);
      showToast("삭제했습니다");
      if (selected?.id === ex.id) { setSelected(null); setRows([]); }
      await loadExams();
    } catch (e) { setError(e instanceof Error ? e.message : "삭제 실패"); }
  };

  const setRow = (i: number, patch: Partial<Row>) =>
    setRows((rs) => rs.map((r, idx) => (idx === i ? { ...r, ...patch } : r)));

  const onSaveScores = async () => {
    if (!selected) return;
    const entries: ScoreEntryInput[] = rows
      .filter((r) => r.studentSubject.trim() && r.score.trim() !== "")
      .map((r) => ({ studentSubject: r.studentSubject.trim(), score: Number(r.score), comment: r.comment || undefined }));
    if (entries.length === 0) { setError("입력된 성적이 없습니다"); return; }
    setBusy(true); setError(null);
    try {
      await recordScores(session.token, selected.id, entries);
      showToast(`${entries.length}명의 성적을 저장했습니다`);
      await openExam(selected);
    } catch (e) { setError(e instanceof Error ? e.message : "저장 실패"); }
    finally { setBusy(false); }
  };

  const emailLabel = (s: string) =>
    s === "SENT" ? "이메일 전송됨" : s === "SIMULATED" ? "이메일 미설정(인앱만)" : s === "FAILED" ? "이메일 실패" : "";

  const onSendReport = async (student: string) => {
    if (!student.trim()) return;
    try {
      const res = await sendReport(session.token, student.trim());
      showToast(res.sent > 0
        ? `학부모 ${res.sent}명에게 리포트 발송 · ${emailLabel(res.emailStatus)}`
        : "연결된 학부모가 없습니다");
    } catch (e) { setError(e instanceof Error ? e.message : "발송 실패"); }
  };

  return (
    <div>
      <h1>시험 · 성적 <span className="badge tenant">{session.orgCode ?? session.tenantId.slice(0, 4)}</span></h1>
      <p className="muted">자체 시험·모의고사를 만들고 학생별 성적을 입력합니다. 성적은 학생·학부모의 추이 그래프와 리포트에 반영됩니다.</p>
      {error && <p className="error">{error}</p>}

      <div className="card">
        <h3>새 시험 만들기</h3>
        <div className="row" style={{ gap: 8, alignItems: "flex-end", flexWrap: "wrap" }}>
          <div style={{ minWidth: 180 }}><label>시험명</label>
            <input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="예: 3월 모의고사" /></div>
          <div style={{ minWidth: 120 }}><label>과목</label>
            <input value={subject} onChange={(e) => setSubject(e.target.value)} placeholder="예: 수학" /></div>
          <div style={{ minWidth: 150 }}><label>시행일</label>
            <input type="date" value={examDate} onChange={(e) => setExamDate(e.target.value)} /></div>
          <div style={{ minWidth: 90 }}><label>만점</label>
            <input type="number" value={maxScore} onChange={(e) => setMaxScore(e.target.value)} /></div>
          <button onClick={onCreate} disabled={busy}>시험 추가</button>
        </div>
      </div>

      <div className="card">
        <h3>시험 목록 ({exams.length})</h3>
        {exams.length === 0 ? <p className="notice">아직 시험이 없습니다.</p> : (
          <table className="grid">
            <thead><tr><th>시행일</th><th>시험</th><th>과목</th><th>만점</th><th style={{ textAlign: "right" }}>작업</th></tr></thead>
            <tbody>
              {exams.map((ex) => (
                <tr key={ex.id} style={selected?.id === ex.id ? { background: "var(--panel-2)" } : undefined}>
                  <td>{ex.examDate}</td><td>{ex.title}</td><td>{ex.subject ?? "—"}</td><td>{ex.maxScore}</td>
                  <td style={{ textAlign: "right" }}>
                    <button className="ghost" onClick={() => openExam(ex)}>성적 입력</button>
                    <button className="ghost" onClick={() => onDeleteExam(ex)}>삭제</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {selected && (
        <div className="card">
          <h3>성적 입력 — {selected.title} <span className="muted">(만점 {selected.maxScore})</span></h3>
          <table className="grid">
            <thead><tr><th>학생 이메일</th><th style={{ width: 90 }}>점수</th><th>메모</th><th style={{ textAlign: "right" }}>리포트</th></tr></thead>
            <tbody>
              {rows.map((r, i) => (
                <tr key={i}>
                  <td><input value={r.studentSubject} onChange={(e) => setRow(i, { studentSubject: e.target.value })} placeholder="student@example.com" /></td>
                  <td><input type="number" value={r.score} onChange={(e) => setRow(i, { score: e.target.value })} placeholder="0" /></td>
                  <td><input value={r.comment} onChange={(e) => setRow(i, { comment: e.target.value })} placeholder="(선택)" /></td>
                  <td style={{ textAlign: "right" }}>
                    <button className="ghost" onClick={() => onSendReport(r.studentSubject)} title="이 학생의 리포트를 학부모에게 발송">📊 발송</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="row" style={{ marginTop: 12 }}>
            <button className="ghost" onClick={() => setRows((rs) => [...rs, { studentSubject: "", score: "", comment: "" }])}>+ 학생 추가</button>
            <button onClick={onSaveScores} disabled={busy}>{busy ? "저장 중…" : "성적 저장"}</button>
          </div>
        </div>
      )}
    </div>
  );
}
