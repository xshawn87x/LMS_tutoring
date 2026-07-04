"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useSession } from "@/components/SessionProvider";
import { useToast } from "@/components/ToastProvider";
import { getSettings, updateSettings } from "@/lib/api";

export default function SettingsPage() {
  const { session } = useSession();
  const { showToast } = useToast();
  const [displayName, setDisplayName] = useState("");
  const [logoUrl, setLogoUrl] = useState("");
  const [primaryColor, setPrimaryColor] = useState("#4f8cff");
  const [contact, setContact] = useState("");
  const [terms, setTerms] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (!session) return;
    try {
      const s = await getSettings(session.token);
      setDisplayName(s.displayName ?? ""); setLogoUrl(s.logoUrl ?? "");
      setPrimaryColor(s.primaryColor ?? "#4f8cff"); setContact(s.contact ?? ""); setTerms(s.terms ?? "");
    } catch (e) { setError(e instanceof Error ? e.message : "실패"); }
  }, [session]);
  useEffect(() => { load(); }, [load]);

  if (!session) return <p className="notice">세션이 없습니다. <Link href="/login">로그인</Link>하세요.</p>;
  if (!session.roles.includes("ADMIN")) return <div><h1>환경설정</h1><p className="error">ADMIN만 접근할 수 있습니다.</p></div>;

  const onSave = async () => {
    setBusy(true); setError(null);
    try { await updateSettings(session.token, { displayName, logoUrl, primaryColor, contact, terms }); showToast("저장했습니다"); }
    catch (e) { setError(e instanceof Error ? e.message : "저장 실패"); } finally { setBusy(false); }
  };

  return (
    <div>
      <h1>학원 환경설정 <span className="badge tenant">{session.orgCode ?? session.tenantId.slice(0, 4)}</span></h1>
      <p className="muted">학원 브랜딩(이름·로고·색상)과 연락처·약관을 설정합니다.</p>
      {error && <p className="error">{error}</p>}
      <div className="card">
        <label>학원 표시명</label>
        <input value={displayName} onChange={(e) => setDisplayName(e.target.value)} placeholder="OO 러닝센터" />
        <label>로고 URL</label>
        <input value={logoUrl} onChange={(e) => setLogoUrl(e.target.value)} placeholder="https://… 또는 /media/…" />
        <label>대표 색상</label>
        <div className="row" style={{ gap: 8 }}>
          <input type="color" value={primaryColor} onChange={(e) => setPrimaryColor(e.target.value)} style={{ width: 60, padding: 2 }} />
          <input value={primaryColor} onChange={(e) => setPrimaryColor(e.target.value)} style={{ maxWidth: 140 }} />
        </div>
        <label>연락처</label>
        <input value={contact} onChange={(e) => setContact(e.target.value)} placeholder="02-000-0000 / 카톡채널 등" />
        <label>약관 / 개인정보 안내</label>
        <textarea rows={5} value={terms} onChange={(e) => setTerms(e.target.value)} />
        <div style={{ marginTop: 12 }}><button onClick={onSave} disabled={busy}>{busy ? "저장 중…" : "저장"}</button></div>
      </div>
      {logoUrl && (
        <div className="card">
          <h3>미리보기</h3>
          <div className="row" style={{ alignItems: "center", gap: 12 }}>
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img src={logoUrl} alt="logo" style={{ height: 40, maxWidth: 160, objectFit: "contain" }} />
            <b style={{ color: primaryColor }}>{displayName || "학원명"}</b>
          </div>
        </div>
      )}
    </div>
  );
}
