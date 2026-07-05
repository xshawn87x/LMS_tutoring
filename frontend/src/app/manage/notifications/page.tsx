"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useSession } from "@/components/SessionProvider";
import { useToast } from "@/components/ToastProvider";
import { DeliveryLogItem, notificationLogs, sendNotification } from "@/lib/api";

const CHANNELS = [
  { v: "IN_APP", l: "인앱(앱 알림)" },
  { v: "EMAIL", l: "이메일(SMTP 설정 시 실발송)" },
  { v: "SMS", l: "SMS(현재 스텁)" },
  { v: "KAKAO", l: "카카오 알림톡(현재 스텁)" },
];

export default function NotificationsAdminPage() {
  const { session } = useSession();
  const { showToast } = useToast();
  const [logs, setLogs] = useState<DeliveryLogItem[]>([]);
  const [recipient, setRecipient] = useState("");
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [channel, setChannel] = useState("IN_APP");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const loadLogs = useCallback(async () => {
    if (!session) return;
    try { setLogs(await notificationLogs(session.token)); }
    catch (e) { setError(e instanceof Error ? e.message : "이력 불러오기 실패"); }
  }, [session]);
  useEffect(() => { loadLogs(); }, [loadLogs]);

  if (!session) return <p className="notice">세션이 없습니다. <Link href="/login">로그인</Link>하세요.</p>;
  if (!session.roles.includes("INSTRUCTOR") && !session.roles.includes("ADMIN")) {
    return <div><h1>알림 발송</h1><p className="error">INSTRUCTOR/ADMIN만 접근할 수 있습니다.</p></div>;
  }

  const onSend = async () => {
    if (!recipient.trim() || !title.trim()) { setError("수신자와 제목을 입력하세요"); return; }
    setBusy(true); setError(null);
    try {
      const res = await sendNotification(session.token, { recipient: recipient.trim(), title: title.trim(), body: body || undefined, channel });
      showToast(`발송: ${res.status}`);
      setTitle(""); setBody(""); await loadLogs();
    } catch (e) { setError(e instanceof Error ? e.message : "발송 실패"); }
    finally { setBusy(false); }
  };

  return (
    <div>
      <h1>알림 발송 <span className="badge tenant">{session.orgCode ?? session.tenantId.slice(0, 4)}</span></h1>
      <p className="muted">회원(학생·학부모)에게 개별 알림을 보냅니다. 인앱은 즉시, 이메일은 SMTP 설정 시 실발송됩니다.</p>
      {error && <p className="error">{error}</p>}

      <div className="card">
        <h3>새 알림</h3>
        <div className="row" style={{ gap: 8, alignItems: "flex-end", flexWrap: "wrap" }}>
          <div style={{ minWidth: 220 }}><label>수신자 이메일</label>
            <input value={recipient} onChange={(e) => setRecipient(e.target.value)} placeholder="user@example.com" /></div>
          <div style={{ minWidth: 200 }}><label>채널</label>
            <select value={channel} onChange={(e) => setChannel(e.target.value)}>
              {CHANNELS.map((c) => <option key={c.v} value={c.v}>{c.l}</option>)}
            </select></div>
        </div>
        <label>제목</label>
        <input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="예: 3월 상담 안내" />
        <label>내용</label>
        <textarea value={body} onChange={(e) => setBody(e.target.value)} rows={3} placeholder="알림 내용" />
        <div style={{ marginTop: 10 }}><button onClick={onSend} disabled={busy}>{busy ? "발송 중…" : "발송"}</button></div>
      </div>

      <div className="card">
        <h3>발송 이력 ({logs.length})</h3>
        {logs.length === 0 ? <p className="notice">발송 이력이 없습니다.</p> : (
          <table className="grid">
            <thead><tr><th>일시</th><th>채널</th><th>수신자</th><th>제목</th><th>상태</th></tr></thead>
            <tbody>
              {logs.map((l) => (
                <tr key={l.id}>
                  <td>{l.createdAt?.replace("T", " ").slice(0, 16)}</td>
                  <td>{l.channel}</td>
                  <td className="muted">{l.recipient}</td>
                  <td>{l.title ?? "—"}</td>
                  <td><span className={`pf-pill ${l.status === "SENT" ? "paid" : "issued"}`}>{l.status}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
