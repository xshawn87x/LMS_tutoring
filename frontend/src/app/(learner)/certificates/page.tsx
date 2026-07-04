"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useSession } from "@/components/SessionProvider";
import { useFeatures } from "@/components/FeaturesProvider";
import { Certificate, getMyCertificates } from "@/lib/api";

export default function CertificatesPage() {
  const { session, hydrated } = useSession();
  const { isEnabled, loaded: featuresLoaded } = useFeatures();
  const [certs, setCerts] = useState<Certificate[]>([]);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (!session || !featuresLoaded || !isEnabled("CERTIFICATES")) return;
    setError(null);
    try {
      setCerts(await getMyCertificates(session.token));
    } catch (e) {
      setError(e instanceof Error ? e.message : "불러오기 실패");
    }
  }, [session, featuresLoaded, isEnabled]);

  useEffect(() => {
    load();
  }, [load]);

  if (!hydrated || !session) {
    return <p className="notice">세션이 없습니다. <Link href="/login">로그인</Link>하세요.</p>;
  }

  if (featuresLoaded && !isEnabled("CERTIFICATES")) {
    return <div><h1>수료증</h1><p className="notice">이 기관에서는 수료증 기능이 비활성화되어 있습니다.</p></div>;
  }

  return (
    <div>
      <h1>내 수료증</h1>
      <p className="muted">과정을 수료하면(진도 100% + 모든 퀴즈 통과) 수료증이 발급됩니다.</p>
      {error && <p className="error">{error}</p>}

      {certs.length === 0 && (
        <p className="notice">아직 수료한 과정이 없습니다. <Link href="/courses">과정 둘러보기</Link></p>
      )}

      {certs.map((c) => (
        <div className="card" key={c.id} style={{ borderColor: "var(--accent-2)", textAlign: "center", padding: "28px 20px" }}>
          <p className="muted" style={{ letterSpacing: 2, marginBottom: 6 }}>CERTIFICATE OF COMPLETION</p>
          <h2 style={{ border: "none", margin: "6px 0" }}>🎓 {c.courseTitle ?? "과정"}</h2>
          <p style={{ margin: "4px 0" }}><b>{session.displayName || session.subject}</b> 님이 위 과정을 수료하였습니다.</p>
          <p className="muted" style={{ marginTop: 10 }}>
            수료번호 <span className="badge">{c.certificateNo}</span>
            발급일 {new Date(c.issuedAt).toLocaleDateString("ko-KR")}
          </p>
          <div style={{ marginTop: 14 }}>
            <Link className="button" href={`/certificates/${c.id}`}>수료증 보기 · 출력</Link>
          </div>
        </div>
      ))}
    </div>
  );
}
