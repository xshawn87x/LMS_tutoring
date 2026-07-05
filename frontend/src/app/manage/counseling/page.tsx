"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useSession } from "@/components/SessionProvider";
import { useToast } from "@/components/ToastProvider";
import {
  Appointment,
  CounselingRecordItem,
  addCounselingRecord,
  allAppointments,
  counselingRecords,
  setAppointmentStatus,
} from "@/lib/api";

const STATUS_LABEL: Record<string, string> = { REQUESTED: "요청", CONFIRMED: "확정", CANCELLED: "취소" };

export default function CounselingAdminPage() {
  const { session } = useSession();
  const { showToast } = useToast();
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [student, setStudent] = useState("");
  const [records, setRecords] = useState<CounselingRecordItem[]>([]);
  const [content, setContent] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const loadAppointments = useCallback(async () => {
    if (!session) return;
    try { setAppointments(await allAppointments(session.token)); }
    catch (e) { setError(e instanceof Error ? e.message : "예약 불러오기 실패"); }
  }, [session]);
  useEffect(() => { loadAppointments(); }, [loadAppointments]);

  if (!session) return <p className="notice">세션이 없습니다. <Link href="/login">로그인</Link>하세요.</p>;
  if (!session.roles.includes("INSTRUCTOR") && !session.roles.includes("ADMIN")) {
    return <div><h1>상담</h1><p className="error">INSTRUCTOR/ADMIN만 접근할 수 있습니다.</p></div>;
  }

  const loadRecords = async () => {
    if (!student.trim()) return;
    setError(null);
    try { setRecords(await counselingRecords(session.token, student.trim())); }
    catch (e) { setError(e instanceof Error ? e.message : "상담 기록 불러오기 실패"); }
  };

  const onAddRecord = async () => {
    if (!student.trim() || !content.trim()) { setError("학생 이메일과 상담 내용을 입력하세요"); return; }
    setBusy(true); setError(null);
    try {
      await addCounselingRecord(session.token, student.trim(), content.trim());
      showToast("상담 기록을 저장했습니다"); setContent(""); await loadRecords();
    } catch (e) { setError(e instanceof Error ? e.message : "저장 실패"); }
    finally { setBusy(false); }
  };

  const onStatus = async (a: Appointment, status: string) => {
    try { await setAppointmentStatus(session.token, a.id, status); showToast("상태를 변경했습니다"); await loadAppointments(); }
    catch (e) { setError(e instanceof Error ? e.message : "변경 실패"); }
  };

  return (
    <div>
      <h1>상담 <span className="badge tenant">{session.orgCode ?? session.tenantId.slice(0, 4)}</span></h1>
      <p className="muted">학생 상담 기록을 남기고, 학부모·학생이 요청한 상담 예약을 관리합니다.</p>
      {error && <p className="error">{error}</p>}

      <div className="card">
        <h3>상담 기록</h3>
        <div className="row" style={{ gap: 8, alignItems: "flex-end", flexWrap: "wrap" }}>
          <div style={{ minWidth: 220 }}><label>학생 이메일</label>
            <input value={student} onChange={(e) => setStudent(e.target.value)} placeholder="student@example.com" /></div>
          <button className="ghost" onClick={loadRecords}>기록 조회</button>
        </div>
        <label>상담 내용</label>
        <textarea value={content} onChange={(e) => setContent(e.target.value)} rows={3} placeholder="상담 내용을 입력하세요" />
        <div style={{ marginTop: 10 }}><button onClick={onAddRecord} disabled={busy}>{busy ? "저장 중…" : "상담 기록 저장"}</button></div>

        {records.length > 0 && (
          <table className="grid" style={{ marginTop: 14 }}>
            <thead><tr><th>일시</th><th>상담자</th><th>내용</th></tr></thead>
            <tbody>
              {records.map((r) => (
                <tr key={r.id}><td>{r.createdAt?.slice(0, 10)}</td><td>{r.counselor}</td><td>{r.content}</td></tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <div className="card">
        <h3>상담 예약 ({appointments.length})</h3>
        {appointments.length === 0 ? <p className="notice">예약이 없습니다.</p> : (
          <table className="grid">
            <thead><tr><th>학생</th><th>요청자</th><th>희망 일시</th><th>메모</th><th>상태</th><th style={{ textAlign: "right" }}>변경</th></tr></thead>
            <tbody>
              {appointments.map((a) => (
                <tr key={a.id}>
                  <td>{a.studentSubject}</td>
                  <td className="muted">{a.requestedBy}</td>
                  <td>{a.preferredAt ? a.preferredAt.replace("T", " ").slice(0, 16) : "—"}</td>
                  <td className="muted">{a.memo ?? "—"}</td>
                  <td><span className="pf-pill">{STATUS_LABEL[a.status] ?? a.status}</span></td>
                  <td style={{ textAlign: "right" }}>
                    {a.status !== "CONFIRMED" && <button className="ghost" onClick={() => onStatus(a, "CONFIRMED")}>확정</button>}
                    {a.status !== "CANCELLED" && <button className="ghost" onClick={() => onStatus(a, "CANCELLED")}>취소</button>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
