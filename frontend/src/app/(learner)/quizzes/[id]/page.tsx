"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useSession } from "@/components/SessionProvider";
import { useToast } from "@/components/ToastProvider";
import {
  addQuestion,
  deleteQuestion,
  deleteQuiz,
  getQuiz,
  myQuizSubmissions,
  QuizDetail,
  QuizQuestion,
  submitQuiz,
  SubmissionResult,
  updateQuestion,
} from "@/lib/api";

export default function QuizPage({ params }: { params: { id: string } }) {
  const quizId = params.id;
  const { session } = useSession();
  const { showToast } = useToast();
  const router = useRouter();

  const [quiz, setQuiz] = useState<QuizDetail | null>(null);
  const [answers, setAnswers] = useState<number[]>([]);
  const [result, setResult] = useState<SubmissionResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [best, setBest] = useState<{ score: number; total: number; attempts: number } | null>(null);

  // 강사용 문항 추가/수정 폼
  const [editingQid, setEditingQid] = useState<string | null>(null);
  const [qBody, setQBody] = useState("");
  const [qChoices, setQChoices] = useState("");
  const [qCorrect, setQCorrect] = useState(0);

  const canTeach = !!session && (session.roles.includes("INSTRUCTOR") || session.roles.includes("ADMIN"));
  const canTake = !!session && (session.roles.includes("STUDENT") || session.roles.includes("ADMIN"));

  const load = useCallback(async () => {
    if (!session) return;
    setError(null);
    try {
      const q = await getQuiz(session.token, quizId);
      setQuiz(q);
      setAnswers(new Array(q.questions.length).fill(-1));
      setResult(null);
      // 내 응시 이력(최고점)
      try {
        const subs = (await myQuizSubmissions(session.token)).filter((s) => s.quizId === quizId);
        if (subs.length > 0) {
          const top = subs.reduce((a, b) => (b.score / b.total > a.score / a.total ? b : a));
          setBest({ score: top.score, total: top.total, attempts: subs.length });
        } else {
          setBest(null);
        }
      } catch {
        setBest(null);
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : "불러오기 실패");
    }
  }, [session, quizId]);

  useEffect(() => {
    load();
  }, [load]);

  if (!session) {
    return <p className="notice">세션이 없습니다. <Link href="/login">로그인</Link>하세요.</p>;
  }

  const allAnswered = quiz !== null && quiz.questions.length > 0 && answers.every((a) => a >= 0);

  const onSubmit = async () => {
    if (!allAnswered) return;
    setBusy(true);
    setError(null);
    try {
      const r = await submitQuiz(session.token, quizId, answers);
      setResult(r);
      showToast(`채점 완료: ${r.score}/${r.total}`);
      await load();
      setResult(r); // load()가 result를 지우므로 다시 설정
    } catch (e) {
      setError(e instanceof Error ? e.message : "제출 실패");
    } finally {
      setBusy(false);
    }
  };

  const resetForm = () => {
    setEditingQid(null);
    setQBody("");
    setQChoices("");
    setQCorrect(0);
  };

  const onSaveQuestion = async () => {
    const choices = qChoices.split("\n").map((s) => s.trim()).filter(Boolean);
    if (!qBody.trim() || choices.length < 2) {
      setError("문항 내용과 보기(2개 이상, 한 줄에 하나)를 입력하세요");
      return;
    }
    if (qCorrect >= choices.length) {
      setError("정답 번호가 보기 개수를 벗어났습니다");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      if (editingQid) {
        await updateQuestion(session.token, quizId, editingQid, {
          body: qBody, choices, correctIndex: qCorrect,
          orderNo: quiz?.questions.find((x) => x.id === editingQid)?.orderNo ?? 1,
        });
        showToast("문항이 수정되었습니다");
      } else {
        await addQuestion(session.token, quizId, {
          body: qBody, choices, correctIndex: qCorrect, orderNo: (quiz?.questions.length ?? 0) + 1,
        });
        showToast("문항이 추가되었습니다");
      }
      resetForm();
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : "저장 실패");
    } finally {
      setBusy(false);
    }
  };

  const startEdit = (q: QuizQuestion) => {
    setEditingQid(q.id);
    setQBody(q.body);
    setQChoices(q.choices.join("\n"));
    setQCorrect(0); // 정답은 응답에 없으므로 다시 지정
    window.scrollTo({ top: document.body.scrollHeight, behavior: "smooth" });
  };

  const onDeleteQuestion = async (q: QuizQuestion) => {
    if (!window.confirm("이 문항을 삭제할까요?")) return;
    setBusy(true);
    try {
      await deleteQuestion(session.token, quizId, q.id);
      showToast("문항이 삭제되었습니다");
      if (editingQid === q.id) resetForm();
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : "삭제 실패");
    } finally {
      setBusy(false);
    }
  };

  const onDeleteQuiz = async () => {
    if (!window.confirm(`'${quiz?.title}' 퀴즈를 삭제할까요? 문항과 제출 기록이 모두 삭제됩니다.`)) return;
    setBusy(true);
    try {
      await deleteQuiz(session.token, quizId);
      showToast("퀴즈가 삭제되었습니다");
      router.push("/courses");
    } catch (e) {
      setError(e instanceof Error ? e.message : "삭제 실패");
      setBusy(false);
    }
  };

  const choose = (qIdx: number, cIdx: number) => {
    if (result) return;
    setAnswers((prev) => prev.map((a, i) => (i === qIdx ? cIdx : a)));
  };

  return (
    <div>
      <p className="muted"><Link href="/courses">← 과정 목록</Link></p>
      <div className="row" style={{ justifyContent: "space-between" }}>
        <h1 style={{ margin: 0 }}>{quiz ? quiz.title : "퀴즈"}</h1>
        {canTeach && quiz && (
          <button className="ghost" onClick={onDeleteQuiz} disabled={busy} style={{ color: "var(--danger)" }}>퀴즈 삭제</button>
        )}
      </div>
      {error && <p className="error">{error}</p>}

      {best && !result && (
        <p className="muted">내 최고 점수 <span className="badge">{best.score}/{best.total}</span> · 응시 {best.attempts}회</p>
      )}

      {result && (
        <div className="card" style={{ borderColor: "var(--accent-2)" }}>
          <h3>채점 결과: {result.score} / {result.total}</h3>
          <p className="muted">
            {result.score === result.total ? "만점입니다! 🎉" : "아래에서 정답 여부를 확인하세요."}
          </p>
          <button className="ghost" onClick={load} style={{ marginTop: 8 }}>다시 풀기</button>
        </div>
      )}

      <h2>문항 ({quiz?.questions.length ?? 0})</h2>
      {quiz?.questions.length === 0 && <p className="notice">아직 문항이 없습니다.</p>}

      {quiz?.questions.map((q, qi) => {
        const verdict = result?.correctness[qi];
        const border = verdict === undefined ? undefined : verdict ? "var(--accent-2)" : "var(--danger)";
        return (
          <div className="card" key={q.id} style={border ? { borderColor: border } : undefined}>
            <div className="row" style={{ justifyContent: "space-between" }}>
              <h3 style={{ margin: 0 }}>
                {q.orderNo}. {q.body}
                {verdict !== undefined && (
                  <span className="badge" style={{ color: verdict ? "var(--accent-2)" : "var(--danger)" }}>
                    {verdict ? "정답" : "오답"}
                  </span>
                )}
              </h3>
              {canTeach && !result && (
                <span className="row">
                  <button className="ghost" disabled={busy} onClick={() => startEdit(q)}>수정</button>
                  <button className="ghost" disabled={busy} onClick={() => onDeleteQuestion(q)} style={{ color: "var(--danger)" }}>삭제</button>
                </span>
              )}
            </div>
            {q.choices.map((choice, ci) => (
              <label key={ci} className="row" style={{ width: "auto", margin: "4px 0" }}>
                <input
                  type="radio"
                  style={{ width: "auto" }}
                  name={`q-${q.id}`}
                  checked={answers[qi] === ci}
                  disabled={!canTake || !!result}
                  onChange={() => choose(qi, ci)}
                />
                <span style={{ marginLeft: 8 }}>{choice}</span>
              </label>
            ))}
          </div>
        );
      })}

      {canTake && quiz && quiz.questions.length > 0 && !result && (
        <button onClick={onSubmit} disabled={busy || !allAnswered}>
          {busy ? "제출 중…" : allAnswered ? "제출하고 채점받기" : "모든 문항에 답해주세요"}
        </button>
      )}
      {!canTake && <p className="muted">응시는 STUDENT 역할이 필요합니다.</p>}

      {canTeach && (
        <>
          <h2>{editingQid ? "문항 수정" : "문항 추가"} <span className="badge role">INSTRUCTOR/ADMIN</span></h2>
          <div className="card" style={editingQid ? { borderColor: "var(--accent)" } : undefined}>
            {editingQid && <p className="muted">문항 수정 중 — 정답 번호를 다시 지정하세요.</p>}
            <label>문항</label>
            <input value={qBody} onChange={(e) => setQBody(e.target.value)} placeholder="예: 1 + 1 = ?" />
            <label>보기 (한 줄에 하나)</label>
            <textarea rows={4} value={qChoices} onChange={(e) => setQChoices(e.target.value)} placeholder={"2\n3\n4"} />
            <label>정답 번호 (0부터)</label>
            <input type="number" min={0} value={qCorrect} onChange={(e) => setQCorrect(Number(e.target.value))} style={{ width: 120 }} />
            <div className="row" style={{ marginTop: 12 }}>
              <button onClick={onSaveQuestion} disabled={busy}>{busy ? "저장 중…" : editingQid ? "문항 저장" : "문항 추가"}</button>
              {editingQid && <button className="ghost" onClick={resetForm} disabled={busy}>취소</button>}
            </div>
          </div>
        </>
      )}
    </div>
  );
}
