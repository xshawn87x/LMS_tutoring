"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useSession } from "@/components/SessionProvider";
import { useToast } from "@/components/ToastProvider";
import { Appointment, CounselingRecordItem, myAppointments, myCounseling, requestAppointment } from "@/lib/api";

const STATUS_LABEL: Record<string, string> = { REQUESTED: "요청됨", CONFIRMED: "확정", CANCELLED: "취소" };

export default function StudentCounselingPage() {
  const { session } = useSession();
  const { showToast } = useToast();
  const [records, setRecords] = useState<CounselingRecordItem[]>([]);
  const [appts, setAppts] = useState<Appointment[]>([]);
  const [preferredAt, setPreferredAt] = useState("");
  const [memo, setMemo] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    if (!session) return;
    try {
      const [r, a] = await Promise.all([myCounseling(session.token), myAppointments(session.token)]);
      setRecords(r); setAppts(a);
    } catch (e) { setError(e instanceof Error ? e.message : "불러오기 실패"); }
  }, [session]);
  useEffect(() => { load(); }, [load]);

  if (!session) return <p className="notice">세션이 없습니다. <Link href="/login">로그인</Link>하세요.</p>;

  const onRequest = async () => {
    setBusy(true); setError(null);
    try {
      await requestAppointment(session.token, { preferredAt: preferredAt || undefined, memo: memo || undefined });
      showToast("상담을 신청했습니다"); setPreferredAt(""); setMemo(""); await load();
    } catch (e) { setError(e instanceof Error ? e.message : "신청 실패"); }
    finally { setBusy(false); }
  };

  return (
    <div>
      <h1>상담</h1>
      <p className="muted">선생님과의 상담을 신청하고, 지난 상담 기록을 확인합니다.</p>
      {error && <p className="error">{error}</p>}

      <div className="card">
        <h3>상담 신청</h3>
        <div className="row" style={{ gap: 8, alignItems: "flex-end", flexWrap: "wrap" }}>
          <div style={{ minWidth: 200 }}><label>희망 일시 (선택)</label>
            <input type="datetime-local" value={preferredAt} onChange={(e) => setPreferredAt(e.target.value)} /></div>
        </div>
        <label>메모 (선택)</label>
        <input value={memo} onChange={(e) => setMemo(e.target.value)} placeholder="상담 희망 내용" />
        <div style={{ marginTop: 10 }}><button onClick={onRequest} disabled={busy}>{busy ? "신청 중…" : "상담 신청"}</button></div>
      </div>

      <div className="card">
        <h3>내 상담 예약</h3>
        {appts.length === 0 ? <p className="notice">신청한 상담이 없습니다.</p> : (
          <table className="grid">
            <thead><tr><th>희망 일시</th><th>메모</th><th>상태</th></tr></thead>
            <tbody>
              {appts.map((a) => (
                <tr key={a.id}>
                  <td>{a.preferredAt ? a.preferredAt.replace("T", " ").slice(0, 16) : "—"}</td>
                  <td className="muted">{a.memo ?? "—"}</td>
                  <td><span className="chip accent">{STATUS_LABEL[a.status] ?? a.status}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {records.length > 0 && (
        <div className="card">
          <h3>지난 상담 기록</h3>
          <table className="grid">
            <thead><tr><th>일시</th><th>상담자</th><th>내용</th></tr></thead>
            <tbody>
              {records.map((r) => (
                <tr key={r.id}><td>{r.createdAt?.slice(0, 10)}</td><td>{r.counselor}</td><td>{r.content}</td></tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
