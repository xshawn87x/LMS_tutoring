"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useSession } from "@/components/SessionProvider";
import { useToast } from "@/components/ToastProvider";
import {
  GuardianLink,
  Member,
  createGuardianLink,
  createMember,
  deleteGuardianLink,
  deleteMember,
  listGuardianLinks,
  listMembers,
  resetMemberPassword,
  updateMemberRoles,
} from "@/lib/api";

const ALL_ROLES = ["STUDENT", "INSTRUCTOR", "ADMIN"];

export default function MembersAdminPage() {
  const { session } = useSession();
  const { showToast } = useToast();
  const [members, setMembers] = useState<Member[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // 새 회원
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [roles, setRoles] = useState<string[]>(["STUDENT"]);
  const [busy, setBusy] = useState(false);

  // 학부모-자녀 연결
  const [links, setLinks] = useState<GuardianLink[]>([]);
  const [parentSubject, setParentSubject] = useState("");
  const [studentSubject, setStudentSubject] = useState("");

  const reload = useCallback(async () => {
    if (!session) return;
    setLoading(true);
    try {
      setMembers(await listMembers(session.token));
      setLinks(await listGuardianLinks(session.token));
    }
    catch (e) { setError(e instanceof Error ? e.message : "불러오기 실패"); }
    finally { setLoading(false); }
  }, [session]);

  useEffect(() => { reload(); }, [reload]);

  if (!session) return <p className="notice">세션이 없습니다. <Link href="/login">로그인</Link>하세요.</p>;
  if (!session.roles.includes("ADMIN")) {
    return <div><h1>회원 관리</h1><p className="error">ADMIN(학원 관리자)만 접근할 수 있습니다.</p></div>;
  }

  const toggleRole = (list: string[], r: string) =>
    list.includes(r) ? list.filter((x) => x !== r) : [...list, r];

  const onCreate = async () => {
    if (!email.trim() || password.length < 8 || roles.length === 0) {
      setError("이메일·비밀번호(8자+)·역할을 확인하세요"); return;
    }
    setBusy(true); setError(null);
    try {
      await createMember(session.token, { email, password, displayName: displayName || undefined, roles });
      showToast("회원을 추가했습니다");
      setEmail(""); setPassword(""); setDisplayName(""); setRoles(["STUDENT"]);
      await reload();
    } catch (e) { setError(e instanceof Error ? e.message : "추가 실패"); }
    finally { setBusy(false); }
  };

  const onChangeRoles = async (m: Member, next: string[]) => {
    if (next.length === 0) return;
    try { await updateMemberRoles(session.token, m.id, next); showToast("역할을 변경했습니다"); await reload(); }
    catch (e) { setError(e instanceof Error ? e.message : "변경 실패"); }
  };

  const onResetPw = async (m: Member) => {
    const pw = prompt(`${m.email}의 새 비밀번호(8자 이상)`);
    if (!pw) return;
    if (pw.length < 8) { setError("비밀번호는 8자 이상"); return; }
    try { await resetMemberPassword(session.token, m.id, pw); showToast("비밀번호를 재설정했습니다"); }
    catch (e) { setError(e instanceof Error ? e.message : "재설정 실패"); }
  };

  const onDelete = async (m: Member) => {
    if (!confirm(`${m.email} 계정을 삭제할까요?`)) return;
    try { await deleteMember(session.token, m.id); showToast("삭제했습니다"); await reload(); }
    catch (e) { setError(e instanceof Error ? e.message : "삭제 실패"); }
  };

  const onLink = async () => {
    if (!parentSubject.trim() || !studentSubject.trim()) return;
    try {
      await createGuardianLink(session.token, parentSubject.trim(), studentSubject.trim());
      showToast("학부모-자녀를 연결했습니다"); setParentSubject(""); setStudentSubject(""); await reload();
    } catch (e) { setError(e instanceof Error ? e.message : "연결 실패"); }
  };
  const onUnlink = async (l: GuardianLink) => {
    try { await deleteGuardianLink(session.token, l.id); showToast("연결을 해제했습니다"); await reload(); }
    catch (e) { setError(e instanceof Error ? e.message : "해제 실패"); }
  };

  return (
    <div>
      <h1>회원 · 강사 관리 <span className="badge tenant">{session.orgCode ?? session.tenantId.slice(0, 4)}</span></h1>
      <p className="muted">학원 소속 회원(수강생·강사·관리자)을 관리합니다.</p>
      {error && <p className="error">{error}</p>}

      <div className="card">
        <h3>새 회원 추가</h3>
        <div className="row" style={{ gap: 8, alignItems: "flex-end", flexWrap: "wrap" }}>
          <div style={{ minWidth: 200 }}><label>이메일</label>
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="user@example.com" /></div>
          <div style={{ minWidth: 140 }}><label>비밀번호(8자+)</label>
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="••••••••" /></div>
          <div style={{ minWidth: 120 }}><label>이름</label>
            <input value={displayName} onChange={(e) => setDisplayName(e.target.value)} placeholder="홍길동" /></div>
        </div>
        <label>역할</label>
        <div className="row">
          {ALL_ROLES.map((r) => (
            <label key={r} className="row" style={{ width: "auto", margin: 0 }}>
              <input type="checkbox" style={{ width: "auto" }} checked={roles.includes(r)} onChange={() => setRoles((p) => toggleRole(p, r))} />
              <span style={{ marginLeft: 6 }}>{r}</span>
            </label>
          ))}
        </div>
        <div style={{ marginTop: 12 }}><button onClick={onCreate} disabled={busy}>{busy ? "추가 중…" : "회원 추가"}</button></div>
      </div>

      <div className="card">
        <h3>회원 목록 {loading ? "" : `(${members.length})`}</h3>
        {loading ? <p className="notice">불러오는 중…</p> : (
          <table className="grid">
            <thead><tr><th>이메일</th><th>이름</th><th>역할</th><th style={{ textAlign: "right" }}>작업</th></tr></thead>
            <tbody>
              {members.map((m) => (
                <tr key={m.id}>
                  <td>{m.email}</td>
                  <td>{m.displayName ?? "—"}</td>
                  <td>
                    <span className="row" style={{ gap: 6 }}>
                      {ALL_ROLES.map((r) => (
                        <label key={r} className="row" style={{ width: "auto", margin: 0, fontSize: 12 }}>
                          <input type="checkbox" style={{ width: "auto" }} checked={m.roles.includes(r)}
                            onChange={() => onChangeRoles(m, toggleRole(m.roles, r))} />
                          <span style={{ marginLeft: 3 }}>{r}</span>
                        </label>
                      ))}
                    </span>
                  </td>
                  <td style={{ textAlign: "right" }}>
                    <button className="ghost" onClick={() => onResetPw(m)}>비번 재설정</button>
                    <button className="ghost" onClick={() => onDelete(m)}>삭제</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <div className="card">
        <h3>학부모 · 자녀 연결</h3>
        <p className="muted" style={{ marginTop: 0 }}>학부모 계정(이메일)과 자녀(학생 이메일)를 연결하면, 학부모가 자녀의 학습현황을 봅니다.</p>
        <div className="row" style={{ gap: 8, alignItems: "flex-end", flexWrap: "wrap" }}>
          <div style={{ minWidth: 200 }}><label>학부모 이메일</label>
            <input value={parentSubject} onChange={(e) => setParentSubject(e.target.value)} placeholder="parent@example.com" /></div>
          <div style={{ minWidth: 200 }}><label>자녀(학생) 이메일</label>
            <input value={studentSubject} onChange={(e) => setStudentSubject(e.target.value)} placeholder="student@example.com" /></div>
          <button onClick={onLink}>연결</button>
        </div>
        {links.length > 0 && (
          <table className="grid" style={{ marginTop: 12 }}>
            <thead><tr><th>학부모</th><th>자녀</th><th style={{ textAlign: "right" }}>작업</th></tr></thead>
            <tbody>
              {links.map((l) => (
                <tr key={l.id}>
                  <td>{l.parentSubject}</td><td>{l.studentSubject}</td>
                  <td style={{ textAlign: "right" }}><button className="ghost" onClick={() => onUnlink(l)}>해제</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
