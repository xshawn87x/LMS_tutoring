"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useSession } from "@/components/SessionProvider";
import { useToast } from "@/components/ToastProvider";
import {
  PlacementBand,
  PlacementRecommendation,
  StudentGroup,
  applyPlacement,
  listGroups,
  recommendPlacement,
} from "@/lib/api";

type BandRow = { minPercent: string; groupId: string };

export default function PlacementPage() {
  const { session } = useSession();
  const { showToast } = useToast();
  const [groups, setGroups] = useState<StudentGroup[]>([]);
  const [bands, setBands] = useState<BandRow[]>([{ minPercent: "80", groupId: "" }, { minPercent: "60", groupId: "" }, { minPercent: "0", groupId: "" }]);
  const [recs, setRecs] = useState<PlacementRecommendation[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    if (!session) return;
    try { setGroups(await listGroups(session.token)); }
    catch (e) { setError(e instanceof Error ? e.message : "반 불러오기 실패"); }
  }, [session]);
  useEffect(() => { load(); }, [load]);

  if (!session) return <p className="notice">세션이 없습니다. <Link href="/login">로그인</Link>하세요.</p>;
  if (!session.roles.includes("INSTRUCTOR") && !session.roles.includes("ADMIN")) {
    return <div><h1>반편성</h1><p className="error">INSTRUCTOR/ADMIN만 접근할 수 있습니다.</p></div>;
  }

  const groupName = (id: string) => groups.find((g) => g.id === id)?.name ?? id.slice(0, 6);
  const setBand = (i: number, patch: Partial<BandRow>) => setBands((bs) => bs.map((b, idx) => idx === i ? { ...b, ...patch } : b));

  const toBands = (): PlacementBand[] => bands
    .filter((b) => b.groupId)
    .map((b) => ({ minPercent: Number(b.minPercent) || 0, groupId: b.groupId }));

  const onRecommend = async () => {
    const bd = toBands();
    if (bd.length === 0) { setError("반을 지정한 기준을 하나 이상 만드세요"); return; }
    setBusy(true); setError(null);
    try { setRecs(await recommendPlacement(session.token, bd)); }
    catch (e) { setError(e instanceof Error ? e.message : "추천 실패"); }
    finally { setBusy(false); }
  };

  const onApply = async () => {
    const bd = toBands();
    if (bd.length === 0) return;
    if (!confirm("추천대로 학생들을 각 반에 배정할까요? (기존 해당 반 배정이 조정됩니다)")) return;
    setBusy(true); setError(null);
    try {
      const res = await applyPlacement(session.token, bd);
      setRecs(res.recommendations);
      showToast(`${res.studentsPlaced}명 중 ${res.assigned}건 배정 완료`);
    } catch (e) { setError(e instanceof Error ? e.message : "적용 실패"); }
    finally { setBusy(false); }
  };

  return (
    <div>
      <h1>성적 기반 반편성 <span className="badge tenant">{session.orgCode ?? session.tenantId.slice(0, 4)}</span></h1>
      <p className="muted">학생들의 평균 시험 성적으로 레벨반을 자동 편성합니다. 기준(구간)을 정해 추천을 미리 본 뒤 적용하세요.</p>
      {error && <p className="error">{error}</p>}

      {groups.length === 0 ? (
        <p className="notice">먼저 <Link href="/manage/groups">반·출석</Link>에서 반을 만들어 주세요.</p>
      ) : (
        <div className="card">
          <h3>편성 기준</h3>
          <p className="muted" style={{ marginTop: 0 }}>“평균 N% 이상 → 이 반”. 위에서부터 높은 기준을 두면 됩니다. (예: 80→상위반, 60→중위반, 0→기초반)</p>
          <table className="grid">
            <thead><tr><th style={{ width: 140 }}>평균 이상(%)</th><th>배정 반</th><th style={{ textAlign: "right" }}></th></tr></thead>
            <tbody>
              {bands.map((b, i) => (
                <tr key={i}>
                  <td><input type="number" value={b.minPercent} onChange={(e) => setBand(i, { minPercent: e.target.value })} /></td>
                  <td>
                    <select value={b.groupId} onChange={(e) => setBand(i, { groupId: e.target.value })}>
                      <option value="">— 반 선택 —</option>
                      {groups.map((g) => <option key={g.id} value={g.id}>{g.name}</option>)}
                    </select>
                  </td>
                  <td style={{ textAlign: "right" }}>
                    <button className="ghost" onClick={() => setBands((bs) => bs.filter((_, idx) => idx !== i))}>삭제</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="row" style={{ marginTop: 12 }}>
            <button className="ghost" onClick={() => setBands((bs) => [...bs, { minPercent: "0", groupId: "" }])}>+ 기준 추가</button>
            <button onClick={onRecommend} disabled={busy}>추천 미리보기</button>
            <button className="success" onClick={onApply} disabled={busy || !recs}>이대로 배정</button>
          </div>
        </div>
      )}

      {recs && (
        <div className="card">
          <h3>배치 추천 ({recs.length}명)</h3>
          {recs.length === 0 ? <p className="notice">성적이 입력된 학생이 없습니다. <Link href="/manage/exams">시험·성적</Link>에서 성적을 먼저 입력하세요.</p> : (
            <>
            <p className="muted" style={{ marginTop: 0 }}>
              총 {recs.length}명 · 이동 {recs.filter((r) => r.moved).length}명 · 유지 {recs.filter((r) => !r.moved).length}명
            </p>
            <table className="grid">
              <thead><tr><th>학생</th><th style={{ textAlign: "right" }}>평균</th><th style={{ textAlign: "right" }}>응시</th><th>현재 → 추천</th></tr></thead>
              <tbody>
                {recs.map((r) => (
                  <tr key={r.studentSubject}>
                    <td>{r.studentName ?? r.studentSubject}</td>
                    <td style={{ textAlign: "right", fontWeight: 700 }}>{r.avgPercent}%</td>
                    <td style={{ textAlign: "right" }}>{r.examCount}</td>
                    <td>
                      <span className="muted">{r.currentGroupName ?? "미배정"}</span>
                      {" → "}
                      <span className="badge role">{r.groupName ?? groupName(r.groupId)}</span>
                      {r.moved && <span className="pf-pill issued" style={{ marginLeft: 6, fontSize: 11 }}>이동</span>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            </>
          )}
        </div>
      )}
    </div>
  );
}
