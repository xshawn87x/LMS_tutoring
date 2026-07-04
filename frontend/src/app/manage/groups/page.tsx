"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useSession } from "@/components/SessionProvider";
import { useToast } from "@/components/ToastProvider";
import {
  AttendanceRecord,
  GroupMember,
  StudentGroup,
  addGroupMember,
  createGroup,
  deleteGroup,
  groupAttendance,
  listGroupMembers,
  listGroups,
  markAttendance,
  removeGroupMember,
} from "@/lib/api";

const STATUS = [
  { v: "PRESENT", l: "출석" }, { v: "LATE", l: "지각" }, { v: "ABSENT", l: "결석" }, { v: "EXCUSED", l: "공결" },
];

export default function GroupsAdminPage() {
  const { session } = useSession();
  const { showToast } = useToast();
  const [groups, setGroups] = useState<StudentGroup[]>([]);
  const [selected, setSelected] = useState<string | null>(null);
  const [members, setMembers] = useState<GroupMember[]>([]);
  const [date, setDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [att, setAtt] = useState<Record<string, string>>({});
  const [name, setName] = useState("");
  const [term, setTerm] = useState("");
  const [newStudent, setNewStudent] = useState("");
  const [error, setError] = useState<string | null>(null);

  const canManage = !!session && (session.roles.includes("INSTRUCTOR") || session.roles.includes("ADMIN"));

  const reloadGroups = useCallback(async () => {
    if (!session) return;
    try { const g = await listGroups(session.token); setGroups(g); setSelected((c) => c ?? g[0]?.id ?? null); }
    catch (e) { setError(e instanceof Error ? e.message : "실패"); }
  }, [session]);
  useEffect(() => { reloadGroups(); }, [reloadGroups]);

  const reloadMembers = useCallback(async () => {
    if (!session || !selected) { setMembers([]); return; }
    setMembers(await listGroupMembers(session.token, selected));
  }, [session, selected]);
  useEffect(() => { reloadMembers(); }, [reloadMembers]);

  const loadAttendance = useCallback(async () => {
    if (!session || !selected) return;
    const recs: AttendanceRecord[] = await groupAttendance(session.token, selected, date);
    setAtt(Object.fromEntries(recs.map((r) => [r.studentSubject, r.status])));
  }, [session, selected, date]);
  useEffect(() => { loadAttendance(); }, [loadAttendance]);

  if (!session) return <p className="notice">세션이 없습니다. <Link href="/login">로그인</Link>하세요.</p>;
  if (!canManage) return <div><h1>반 관리</h1><p className="error">강사/관리자만 접근할 수 있습니다.</p></div>;

  const onCreate = async () => {
    if (!name.trim()) return;
    await createGroup(session.token, { name, term: term || undefined });
    showToast("반을 만들었습니다"); setName(""); setTerm(""); await reloadGroups();
  };
  const onAddMember = async () => {
    if (!selected || !newStudent.trim()) return;
    try { await addGroupMember(session.token, selected, newStudent.trim()); showToast("배정했습니다"); setNewStudent(""); await reloadMembers(); }
    catch (e) { setError(e instanceof Error ? e.message : "실패"); }
  };
  const onSaveAttendance = async () => {
    if (!selected) return;
    const entries = members.map((m) => ({ studentSubject: m.studentSubject, status: att[m.studentSubject] ?? "PRESENT" }));
    await markAttendance(session.token, selected, date, entries);
    showToast("출석을 저장했습니다"); await loadAttendance();
  };

  return (
    <div>
      <h1>반 · 출석 관리 <span className="badge tenant">{session.orgCode ?? session.tenantId.slice(0, 4)}</span></h1>
      {error && <p className="error">{error}</p>}

      <div className="card">
        <h3>새 반/기수</h3>
        <div className="row" style={{ gap: 8, alignItems: "flex-end" }}>
          <div style={{ minWidth: 200 }}><label>반 이름</label><input value={name} onChange={(e) => setName(e.target.value)} placeholder="초등 3학년 A반" /></div>
          <div style={{ minWidth: 140 }}><label>기수</label><input value={term} onChange={(e) => setTerm(e.target.value)} placeholder="2026-1기" /></div>
          <button onClick={onCreate}>만들기</button>
        </div>
      </div>

      <div className="pf-td" style={{ display: "grid", gridTemplateColumns: "260px 1fr", gap: 20, alignItems: "start" }}>
        <div className="pf-tenant-list">
          {groups.map((g) => (
            <button key={g.id} className={`pf-tenant-card ${selected === g.id ? "active" : ""}`} onClick={() => setSelected(g.id)}>
              <div className="t-name">{g.name}</div>
              <div className="t-meta"><span className="muted">{g.term ?? ""}</span><span style={{ marginLeft: "auto" }} className="muted">{g.memberCount}명</span></div>
            </button>
          ))}
          {groups.length === 0 && <p className="notice">반이 없습니다.</p>}
        </div>

        <div>
          {!selected ? <div className="card muted">반을 선택하세요.</div> : (
            <>
              <div className="card">
                <div className="row" style={{ justifyContent: "space-between" }}>
                  <h3 style={{ margin: 0 }}>소속 학생 ({members.length})</h3>
                  <button className="ghost" onClick={async () => { if (confirm("반을 삭제할까요?")) { await deleteGroup(session.token, selected); setSelected(null); await reloadGroups(); } }}>반 삭제</button>
                </div>
                <div className="row" style={{ gap: 6, marginTop: 8 }}>
                  <input value={newStudent} onChange={(e) => setNewStudent(e.target.value)} placeholder="학생 이메일" style={{ maxWidth: 260 }} />
                  <button onClick={onAddMember}>배정</button>
                </div>
                <table className="grid" style={{ marginTop: 10 }}>
                  <tbody>
                    {members.map((m) => (
                      <tr key={m.id}>
                        <td>{m.studentSubject}</td>
                        <td style={{ textAlign: "right" }}><button className="ghost" onClick={async () => { await removeGroupMember(session.token, selected, m.studentSubject); await reloadMembers(); }}>제외</button></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <div className="card">
                <div className="row" style={{ justifyContent: "space-between", alignItems: "center" }}>
                  <h3 style={{ margin: 0 }}>출석 체크</h3>
                  <input type="date" value={date} onChange={(e) => setDate(e.target.value)} style={{ width: "auto" }} />
                </div>
                {members.length === 0 ? <p className="notice">학생을 먼저 배정하세요.</p> : (
                  <>
                    <table className="grid" style={{ marginTop: 8 }}>
                      <tbody>
                        {members.map((m) => (
                          <tr key={m.id}>
                            <td>{m.studentSubject}</td>
                            <td style={{ textAlign: "right" }}>
                              <select value={att[m.studentSubject] ?? "PRESENT"} onChange={(e) => setAtt((p) => ({ ...p, [m.studentSubject]: e.target.value }))} style={{ width: "auto" }}>
                                {STATUS.map((s) => <option key={s.v} value={s.v}>{s.l}</option>)}
                              </select>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                    <div style={{ marginTop: 10 }}><button onClick={onSaveAttendance}>출석 저장</button></div>
                  </>
                )}
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
