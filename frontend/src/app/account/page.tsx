"use client";

import { useState } from "react";
import Link from "next/link";
import { useSession } from "@/components/SessionProvider";
import { useToast } from "@/components/ToastProvider";
import { changePassword, updateAccount } from "@/lib/api";

export default function AccountPage() {
  const { session, updateDisplayName } = useSession();
  const { showToast } = useToast();

  const [displayName, setDisplayName] = useState(session?.displayName ?? "");
  const [curPw, setCurPw] = useState("");
  const [newPw, setNewPw] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!session) {
    return <p className="notice">세션이 없습니다. <Link href="/login">로그인</Link>하세요.</p>;
  }
  const isDevAccount = !session.orgCode; // dev 토큰 세션은 orgCode가 없다

  const onSaveName = async () => {
    setBusy(true);
    setError(null);
    try {
      const r = await updateAccount(session.token, { displayName });
      updateDisplayName(r.displayName);
      showToast("이름이 수정되었습니다");
    } catch (e) {
      setError(e instanceof Error ? e.message : "수정 실패");
    } finally {
      setBusy(false);
    }
  };

  const onChangePw = async () => {
    if (newPw.length < 8) {
      setError("새 비밀번호는 8자 이상이어야 합니다");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await changePassword(session.token, { currentPassword: curPw, newPassword: newPw });
      setCurPw("");
      setNewPw("");
      showToast("비밀번호가 변경되었습니다");
    } catch (e) {
      setError(e instanceof Error ? e.message : "변경 실패");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div>
      <h1>내 계정</h1>
      <p className="muted">
        {session.subject} · {session.orgCode ? `기관 ${session.orgCode}` : session.tenantId.slice(0, 8)} · {session.roles.join(", ")}
      </p>

      {error && <p className="error">{error}</p>}

      {isDevAccount && (
        <p className="notice">
          dev 토큰 세션은 실제 계정이 아니라 이름/비밀번호를 바꿀 수 없습니다. <Link href="/login">회원가입/로그인</Link> 후 이용하세요.
        </p>
      )}

      <div className="card">
        <h3 style={{ marginTop: 0 }}>표시 이름</h3>
        <label>이름</label>
        <input value={displayName} onChange={(e) => setDisplayName(e.target.value)} placeholder="예: 홍길동" />
        <div style={{ marginTop: 12 }}>
          <button onClick={onSaveName} disabled={busy || isDevAccount}>{busy ? "저장 중…" : "이름 저장"}</button>
        </div>
      </div>

      <div className="card">
        <h3 style={{ marginTop: 0 }}>비밀번호 변경</h3>
        <label>현재 비밀번호</label>
        <input type="password" value={curPw} onChange={(e) => setCurPw(e.target.value)} placeholder="••••••••" />
        <label>새 비밀번호 <span className="muted">(8자 이상)</span></label>
        <input type="password" value={newPw} onChange={(e) => setNewPw(e.target.value)} placeholder="••••••••" />
        <div style={{ marginTop: 12 }}>
          <button onClick={onChangePw} disabled={busy || isDevAccount || !curPw || !newPw}>
            {busy ? "변경 중…" : "비밀번호 변경"}
          </button>
        </div>
      </div>
    </div>
  );
}
