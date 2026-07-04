"use client";

import { useEffect } from "react";

/**
 * 라우트 에러 경계. 클라이언트 렌더/데이터 처리 중 예외가 나면 흰 화면/크래시 대신
 * 이 화면을 보여주고 다시 시도할 수 있게 한다.
 */
export default function Error({ error, reset }: { error: Error & { digest?: string }; reset: () => void }) {
  useEffect(() => {
    // 개발 중 콘솔에 원인 남기기
    console.error("route error:", error);
  }, [error]);

  return (
    <div className="card" style={{ maxWidth: 560, margin: "40px auto" }}>
      <h2 style={{ marginTop: 0 }}>문제가 발생했습니다</h2>
      <p className="muted">
        화면을 그리는 중 오류가 발생했습니다. 잠시 후 다시 시도하거나, 문제가 계속되면 새로고침해 주세요.
      </p>
      {error?.message && (
        <pre style={{
          background: "var(--panel-2)", border: "1px solid var(--border)", borderRadius: 8,
          padding: 12, fontSize: 12, overflowX: "auto", whiteSpace: "pre-wrap",
        }}>{error.message}</pre>
      )}
      <div className="row" style={{ marginTop: 12 }}>
        <button onClick={() => reset()}>다시 시도</button>
        <button className="ghost" onClick={() => { if (typeof window !== "undefined") window.location.href = "/"; }}>홈으로</button>
      </div>
    </div>
  );
}
