"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useSession } from "@/components/SessionProvider";
import { useToast } from "@/components/ToastProvider";
import {
  Notice,
  createAcademyNotice,
  deleteNotice,
  listAcademyNotices,
  updateNotice,
} from "@/lib/api";

export default function NoticesPage() {
  const { session } = useSession();
  const { showToast } = useToast();
  const [notices, setNotices] = useState<Notice[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const canWrite = !!session && (session.roles.includes("INSTRUCTOR") || session.roles.includes("ADMIN"));

  // 작성/수정 폼
  const [editId, setEditId] = useState<string | null>(null);
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [pinned, setPinned] = useState(false);
  const [busy, setBusy] = useState(false);

  const reload = useCallback(async () => {
    if (!session) return;
    setLoading(true);
    try {
      setNotices(await listAcademyNotices(session.token));
    } catch (e) {
      setError(e instanceof Error ? e.message : "불러오기 실패");
    } finally {
      setLoading(false);
    }
  }, [session]);

  useEffect(() => { reload(); }, [reload]);

  const resetForm = () => { setEditId(null); setTitle(""); setBody(""); setPinned(false); };

  const onSubmit = async () => {
    if (!session || !title.trim()) return;
    setBusy(true);
    try {
      if (editId) {
        await updateNotice(session.token, editId, { title, body, pinned });
        showToast("공지를 수정했습니다");
      } else {
        await createAcademyNotice(session.token, { title, body, pinned });
        showToast("공지를 등록했습니다");
      }
      resetForm();
      await reload();
    } catch (e) {
      setError(e instanceof Error ? e.message : "저장 실패");
    } finally {
      setBusy(false);
    }
  };

  const onEdit = (n: Notice) => { setEditId(n.id); setTitle(n.title); setBody(n.body ?? ""); setPinned(n.pinned); };

  const onDelete = async (n: Notice) => {
    if (!session || !confirm("이 공지를 삭제할까요?")) return;
    try {
      await deleteNotice(session.token, n.id);
      showToast("공지를 삭제했습니다");
      await reload();
    } catch (e) {
      setError(e instanceof Error ? e.message : "삭제 실패");
    }
  };

  if (!session) return <p className="notice">세션이 없습니다. <Link href="/login">로그인</Link>하세요.</p>;

  return (
    <div>
      <h1>공지사항 <span className="badge tenant">{session.orgCode ?? session.tenantId.slice(0, 4)}</span></h1>
      <p className="muted">학원 전체 공지입니다. 강의별 공지는 각 강의 상세에서 확인하세요.</p>
      {error && <p className="error">{error}</p>}

      {canWrite && (
        <div className="card">
          <h3>{editId ? "공지 수정" : "새 공지 작성"}</h3>
          <label>제목</label>
          <input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="공지 제목" />
          <label>내용</label>
          <textarea value={body} onChange={(e) => setBody(e.target.value)} rows={4} placeholder="공지 내용" />
          <label className="row" style={{ width: "auto", marginTop: 8 }}>
            <input type="checkbox" style={{ width: "auto" }} checked={pinned} onChange={(e) => setPinned(e.target.checked)} />
            <span style={{ marginLeft: 6 }}>상단 고정</span>
          </label>
          <div className="row" style={{ marginTop: 12 }}>
            <button onClick={onSubmit} disabled={busy || !title.trim()}>{busy ? "저장 중…" : editId ? "수정" : "등록"}</button>
            {editId && <button className="ghost" onClick={resetForm}>취소</button>}
          </div>
        </div>
      )}

      {loading ? <p className="notice">불러오는 중…</p> : notices.length === 0 ? (
        <p className="notice">등록된 공지가 없습니다.</p>
      ) : (
        notices.map((n) => (
          <div className="card" key={n.id}>
            <div className="row" style={{ justifyContent: "space-between" }}>
              <h3 style={{ margin: 0 }}>{n.pinned && "📌 "}{n.title}</h3>
              {canWrite && (
                <span className="row" style={{ width: "auto" }}>
                  <button className="ghost" onClick={() => onEdit(n)}>수정</button>
                  <button className="ghost" onClick={() => onDelete(n)}>삭제</button>
                </span>
              )}
            </div>
            {n.body && <p style={{ whiteSpace: "pre-wrap", marginBottom: 6 }}>{n.body}</p>}
            <p className="muted" style={{ margin: 0, fontSize: 12 }}>
              {n.author ?? "관리자"} · {new Date(n.createdAt).toLocaleString("ko-KR")}
            </p>
          </div>
        ))
      )}
    </div>
  );
}
