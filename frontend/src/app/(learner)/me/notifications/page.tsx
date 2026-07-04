"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useSession } from "@/components/SessionProvider";
import { AppNotification, myNotifications, readAllNotifications, readNotification } from "@/lib/api";

export default function NotificationsPage() {
  const { session } = useSession();
  const [items, setItems] = useState<AppNotification[]>([]);
  const [loading, setLoading] = useState(true);

  const reload = useCallback(async () => {
    if (!session) return;
    setLoading(true);
    try { setItems(await myNotifications(session.token)); } finally { setLoading(false); }
  }, [session]);
  useEffect(() => { reload(); }, [reload]);

  if (!session) return <p className="notice">세션이 없습니다. <Link href="/login">로그인</Link>하세요.</p>;

  const onRead = async (n: AppNotification) => { await readNotification(session.token, n.id); await reload(); };
  const onReadAll = async () => { await readAllNotifications(session.token); await reload(); };

  return (
    <div>
      <div className="row" style={{ justifyContent: "space-between" }}>
        <h1 style={{ margin: 0 }}>알림</h1>
        <button className="ghost" onClick={onReadAll}>모두 읽음</button>
      </div>
      {loading ? <p className="notice">불러오는 중…</p> : items.length === 0 ? (
        <p className="notice">알림이 없습니다.</p>
      ) : items.map((n) => (
        <div className="card" key={n.id} style={n.read ? { opacity: 0.6 } : undefined}>
          <div className="row" style={{ justifyContent: "space-between" }}>
            <b>{!n.read && "🔵 "}{n.title}</b>
            {!n.read && <button className="ghost" onClick={() => onRead(n)}>읽음</button>}
          </div>
          {n.body && <p style={{ margin: "4px 0 0" }}>{n.body}</p>}
          <p className="muted" style={{ margin: "4px 0 0", fontSize: 12 }}>{new Date(n.createdAt).toLocaleString("ko-KR")}</p>
        </div>
      ))}
    </div>
  );
}
