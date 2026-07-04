"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useSession } from "@/components/SessionProvider";
import { useFeatures } from "@/components/FeaturesProvider";
import { Certificate, getMyCertificates } from "@/lib/api";

export default function CertificateDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { session, hydrated } = useSession();
  const { isEnabled, loaded: featuresLoaded } = useFeatures();
  const [cert, setCert] = useState<Certificate | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loaded, setLoaded] = useState(false);

  const load = useCallback(async () => {
    if (!session || !featuresLoaded || !isEnabled("CERTIFICATES")) return;
    setError(null);
    try {
      const list = await getMyCertificates(session.token);
      setCert(list.find((c) => c.id === id) ?? null);
      setLoaded(true);
    } catch (e) {
      setError(e instanceof Error ? e.message : "불러오기 실패");
      setLoaded(true);
    }
  }, [session, featuresLoaded, isEnabled, id]);

  useEffect(() => {
    load();
  }, [load]);

  if (!hydrated || !session) {
    return <p className="notice">세션이 없습니다. <Link href="/login">로그인</Link>하세요.</p>;
  }
  if (featuresLoaded && !isEnabled("CERTIFICATES")) {
    return <div><h1>수료증</h1><p className="notice">이 기관에서는 수료증 기능이 비활성화되어 있습니다.</p></div>;
  }
  if (loaded && !cert) {
    return (
      <div>
        {error && <p className="error">{error}</p>}
        <p className="notice">수료증을 찾을 수 없습니다. <Link href="/certificates">목록으로</Link></p>
      </div>
    );
  }
  if (!cert) return <p className="notice">불러오는 중…</p>;

  const recipient = session.displayName || session.subject;
  const issued = new Date(cert.issuedAt).toLocaleDateString("ko-KR", { year: "numeric", month: "long", day: "numeric" });

  return (
    <div>
      <div className="row no-print" style={{ justifyContent: "space-between", marginBottom: 16 }}>
        <Link href="/certificates" className="muted">← 수료증 목록</Link>
        <button onClick={() => window.print()}>🖨 인쇄 / PDF 저장</button>
      </div>

      <div className="certificate-doc">
        <div className="seal">🎓</div>
        <div className="cert-title">수 료 증</div>
        <div className="cert-sub">CERTIFICATE OF COMPLETION</div>

        <p className="cert-body">아래의 수강생은 다음 과정을 성실히 이수하였기에<br />이 증서를 수여합니다.</p>

        <div className="cert-name">{recipient}</div>
        <div className="cert-course">「 {cert.courseTitle ?? "과정"} 」</div>

        <hr className="cert-rule" />

        <div className="cert-meta">
          <span>수료번호: <b>{cert.certificateNo}</b></span>
          <span>발급일: <b>{issued}</b></span>
        </div>
        <div className="cert-meta" style={{ marginTop: 12 }}>
          <span>발급기관: <b>{session.orgCode ? session.orgCode.toUpperCase() : "LMS"}</b></span>
          <span>LMS 러닝 센터</span>
        </div>
      </div>
    </div>
  );
}
