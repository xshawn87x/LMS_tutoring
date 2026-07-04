"use client";

import { useState } from "react";
import Link from "next/link";
import { useSession } from "@/components/SessionProvider";
import { useFeatures } from "@/components/FeaturesProvider";
import { useToast } from "@/components/ToastProvider";
import { toggleFeature } from "@/lib/api";

export default function FeaturesAdminPage() {
  const { session } = useSession();
  const { features, loaded, reload } = useFeatures();
  const { showToast } = useToast();
  const [busy, setBusy] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  if (!session) {
    return <p className="notice">세션이 없습니다. <Link href="/">로그인</Link>하세요.</p>;
  }
  if (!session.roles.includes("ADMIN")) {
    return (
      <div>
        <h1>기능 설정</h1>
        <p className="error">이 페이지는 ADMIN(기관 관리자)만 접근할 수 있습니다. (현재: {session.roles.join(", ")})</p>
      </div>
    );
  }

  const onToggle = async (name: string, next: boolean) => {
    setBusy(name);
    setError(null);
    try {
      await toggleFeature(session.token, name, next);
      await reload(); // 변경을 전체 UI에 반영
      showToast(`'${name}' 기능을 ${next ? "켰습니다" : "껐습니다"}`);
    } catch (e) {
      setError(e instanceof Error ? e.message : "변경 실패");
    } finally {
      setBusy(null);
    }
  };

  return (
    <div>
      <h1>기능 설정 <span className="badge tenant">테넌트 {session.tenantId.slice(0, 4)}</span></h1>
      <p className="muted">
        이 기관에서 사용할 모듈을 선택적으로 켜고 끕니다. 끄면 해당 기능의 화면과 API가 이 기관에 한해 비활성화됩니다.
        (다른 기관에는 영향 없음 — 테넌트별 독립)
      </p>
      {error && <p className="error">{error}</p>}
      {!loaded && <p className="notice">불러오는 중…</p>}

      {features.map((f) => (
        <div className="card" key={f.name} style={!f.entitled ? { opacity: 0.7 } : undefined}>
          <div className="row" style={{ justifyContent: "space-between" }}>
            <div>
              <h3 style={{ marginBottom: 2 }}>
                {!f.entitled && "🔒 "}
                {f.displayName}
                <span className="badge">{f.name}</span>
                {!f.implemented && <span className="badge" style={{ color: "var(--muted)" }}>예정</span>}
              </h3>
              <p className="muted" style={{ margin: 0 }}>
                {!f.entitled
                  ? "요금제에 미포함 — 사용하려면 플랜 업그레이드가 필요합니다"
                  : f.enabled ? "활성화됨" : "비활성화됨"}
                {f.entitled && !f.implemented && " · 아직 구현되지 않은 기능(플래그만 제공)"}
              </p>
            </div>
            {f.entitled ? (
              <button
                className={f.enabled ? "ghost" : "success"}
                disabled={busy === f.name}
                onClick={() => onToggle(f.name, !f.enabled)}
              >
                {busy === f.name ? "변경 중…" : f.enabled ? "끄기" : "켜기"}
              </button>
            ) : (
              <button className="ghost" disabled title="요금제에 포함되지 않은 기능입니다">잠김</button>
            )}
          </div>
        </div>
      ))}

      <p className="muted">
        예: <b>퀴즈/평가</b>를 끄면 과정 상세의 퀴즈 섹션과 내비가 사라지고, 퀴즈 API 호출은 403을 반환합니다.
        🔒 표시된 기능은 <b>요금제에 포함되지 않아</b> 켤 수 없습니다 — 플랫폼 관리자가 플랜을 올리거나 애드온을 부여해야 합니다.
      </p>
    </div>
  );
}
