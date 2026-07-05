"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useSession } from "@/components/SessionProvider";
import { useToast } from "@/components/ToastProvider";
import {
  Exam,
  ExamRanking,
  ScoreEntryInput,
  StudentGroup,
  createExam,
  deleteExam,
  examRanking,
  examScores,
  listExams,
  listGroupMembers,
  listGroups,
  recordScores,
  sendAllReports,
  sendReport,
} from "@/lib/api";

type Row = { studentSubject: string; score: string; comment: string };

export default function ExamsAdminPage() {
  const { session } = useSession();
  const { showToast } = useToast();
  const [exams, setExams] = useState<Exam[]>([]);
  const [groups, setGroups] = useState<StudentGroup[]>([]);
  const [selected, setSelected] = useState<Exam | null>(null);
  const [rows, setRows] = useState<Row[]>([]);
  const [ranking, setRanking] = useState<{ exam: Exam; rows: ExamRanking[] } | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  // 새 시험
  const [title, setTitle] = useState("");
  const [subject, setSubject] = useState("");
  const [examDate, setExamDate] = useState("");
  const [maxScore, setMaxScore] = useState("100");
  const [groupId, setGroupId] = useState("");

  const loadExams = useCallback(async () => {
    if (!session) return;
    try {
      setExams(await listExams(session.token));
      setGroups(await listGroups(session.token));
    }
    catch (e) { setError(e instanceof Error ? e.message : "불러오기 실패"); }
  }, [session]);

  useEffect(() => { loadExams(); }, [loadExams]);

  const groupName = (id: string | null) => id ? (groups.find((g) => g.id === id)?.name ?? "반") : null;

  // 시험에 연결된 반의 학생을 성적 입력표로 불러온다(기존 입력은 유지, 없는 학생만 추가).
  const loadRoster = async () => {
    if (!session || !selected?.groupId) return;
    try {
      const members = await listGroupMembers(session.token, selected.groupId);
      setRows((rs) => {
        const have = new Set(rs.map((r) => r.studentSubject.trim().toLowerCase()).filter(Boolean));
        const added = members
          .filter((m) => !have.has(m.studentSubject.toLowerCase()))
          .map((m) => ({ studentSubject: m.studentSubject, score: "", comment: "" }));
        const base = rs.filter((r) => r.studentSubject.trim());
        return [...base, ...added].length ? [...base, ...added] : [{ studentSubject: "", score: "", comment: "" }];
      });
      showToast(`${members.length}명의 명단을 불러왔습니다`);
    } catch (e) { setError(e instanceof Error ? e.message : "명단 불러오기 실패"); }
  };

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
      await createExam(session.token, { title, subject: subject || undefined, examDate, maxScore: Number(maxScore) || 100, groupId: groupId || null });
      showToast("시험을 만들었습니다");
      setTitle(""); setSubject(""); setExamDate(""); setMaxScore("100"); setGroupId("");
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

  const onSendAll = async () => {
    if (!confirm("연결된 자녀가 있는 모든 학생의 리포트를 학부모에게 발송할까요?")) return;
    setBusy(true); setError(null);
    try {
      const res = await sendAllReports(session.token);
      showToast(`학생 ${res.students}명 · 학부모 ${res.notified}명에게 발송했습니다`);
    } catch (e) { setError(e instanceof Error ? e.message : "일괄 발송 실패"); }
    finally { setBusy(false); }
  };

  const openRanking = async (ex: Exam) => {
    try { setRanking({ exam: ex, rows: await examRanking(session.token, ex.id) }); }
    catch (e) { setError(e instanceof Error ? e.message : "석차 불러오기 실패"); }
  };

  return (
    <div>
      <div className="row" style={{ justifyContent: "space-between", alignItems: "flex-start" }}>
        <div>
          <h1>시험 · 성적 <span className="badge tenant">{session.orgCode ?? session.tenantId.slice(0, 4)}</span></h1>
          <p className="muted">자체 시험·모의고사를 만들고 학생별 성적을 입력합니다. 성적은 학생·학부모의 추이 그래프와 리포트에 반영됩니다.</p>
        </div>
        <button className="ghost" onClick={onSendAll} disabled={busy} title="연결된 자녀 전체 학부모에게 리포트 발송">📨 전체 리포트 발송</button>
      </div>
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
          <div style={{ minWidth: 130 }}><label>대상 반 (선택)</label>
            <select value={groupId} onChange={(e) => setGroupId(e.target.value)}>
              <option value="">— 전체 —</option>
              {groups.map((g) => <option key={g.id} value={g.id}>{g.name}</option>)}
            </select></div>
          <button onClick={onCreate} disabled={busy}>시험 추가</button>
        </div>
        {groups.length === 0 && <p className="muted" style={{ marginBottom: 0 }}>반을 지정하면 성적 입력 시 <Link href="/manage/groups">반 명단</Link>을 불러올 수 있습니다.</p>}
      </div>

      <div className="card">
        <h3>시험 목록 ({exams.length})</h3>
        {exams.length === 0 ? <p className="notice">아직 시험이 없습니다.</p> : (
          <table className="grid">
            <thead><tr><th>시행일</th><th>시험</th><th>과목</th><th>대상 반</th><th>만점</th><th style={{ textAlign: "right" }}>작업</th></tr></thead>
            <tbody>
              {exams.map((ex) => (
                <tr key={ex.id} style={selected?.id === ex.id ? { background: "var(--panel-2)" } : undefined}>
                  <td>{ex.examDate}</td><td>{ex.title}</td><td>{ex.subject ?? "—"}</td><td>{groupName(ex.groupId) ?? "전체"}</td><td>{ex.maxScore}</td>
                  <td style={{ textAlign: "right" }}>
                    <button className="ghost" onClick={() => openExam(ex)}>성적 입력</button>
                    <button className="ghost" onClick={() => openRanking(ex)}>석차표</button>
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
          <h3>성적 입력 — {selected.title} <span className="muted">(만점 {selected.maxScore}{selected.groupId ? ` · ${groupName(selected.groupId)}` : ""})</span></h3>
          {selected.groupId && (
            <button className="ghost" onClick={loadRoster} style={{ marginBottom: 10 }}>👥 반 학생 명단 불러오기</button>
          )}
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

      {ranking && (
        <div className="card">
          <h3>석차표 — {ranking.exam.title} <button className="ghost" style={{ marginLeft: 8 }} onClick={() => setRanking(null)}>닫기</button></h3>
          {ranking.rows.length === 0 ? <p className="notice">입력된 성적이 없습니다.</p> : (
            <table className="grid">
              <thead><tr><th>석차</th><th>학생</th><th style={{ textAlign: "right" }}>점수</th><th style={{ textAlign: "right" }}>백분율</th><th style={{ textAlign: "right" }}>상위</th></tr></thead>
              <tbody>
                {ranking.rows.map((r) => (
                  <tr key={r.studentSubject}>
                    <td><b>{r.rank}</b> / {r.totalTakers}</td>
                    <td>{r.studentName ?? r.studentSubject}</td>
                    <td style={{ textAlign: "right" }}>{r.score}</td>
                    <td style={{ textAlign: "right", fontWeight: 700 }}>{r.percent}%</td>
                    <td style={{ textAlign: "right" }}>상위 {r.topPercent}%</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}
    </div>
  );
}
