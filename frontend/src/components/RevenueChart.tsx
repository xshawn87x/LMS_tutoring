"use client";

// 월별 매출(발행/결제) 막대 차트 — 인라인 SVG, 무의존. 플랫폼 다크 테마용.
export interface RevPoint { period: string; issued: number; paid: number; }

const won = (n: number) => n.toLocaleString("ko-KR");

export function RevenueChart({ data, height = 240 }: { data: RevPoint[]; height?: number }) {
  if (!data || data.length === 0) return <p className="notice">아직 인보이스(매출) 데이터가 없습니다.</p>;

  const W = 720, H = 250, padL = 56, padR = 16, padT = 16, padB = 34;
  const innerW = W - padL - padR, innerH = H - padT - padB;
  const max = Math.max(1, ...data.map((d) => d.issued));
  const band = innerW / data.length;
  const barW = Math.min(34, band * 0.5);
  const y = (v: number) => padT + innerH * (1 - v / max);
  const base = padT + innerH;
  const gridVals = [0, 0.5, 1].map((f) => Math.round(max * f));

  return (
    <div style={{ width: "100%", overflowX: "auto" }}>
      <div className="row" style={{ gap: 16, marginBottom: 8, fontSize: 12, fontWeight: 700 }}>
        <span><span style={{ display: "inline-block", width: 11, height: 11, borderRadius: 2, background: "var(--accent)", marginRight: 5 }} />발행</span>
        <span><span style={{ display: "inline-block", width: 11, height: 11, borderRadius: 2, background: "var(--accent-2)", marginRight: 5 }} />결제</span>
      </div>
      <svg viewBox={`0 0 ${W} ${H}`} width="100%" height={height} role="img" aria-label="월별 매출" style={{ display: "block", minWidth: 360 }}>
        {gridVals.map((v) => (
          <g key={v}>
            <line x1={padL} y1={y(v)} x2={W - padR} y2={y(v)} stroke="var(--border)" strokeWidth="1" />
            <text x={padL - 8} y={y(v) + 4} textAnchor="end" fontSize="10.5" fill="var(--muted)">₩{won(v)}</text>
          </g>
        ))}
        {data.map((d, i) => {
          const cx = padL + band * i + band / 2;
          return (
            <g key={d.period}>
              <rect x={cx - barW / 2} y={y(d.issued)} width={barW} height={base - y(d.issued)} rx="3" fill="var(--accent)" opacity="0.55">
                <title>{`${d.period} 발행 ₩${won(d.issued)}`}</title>
              </rect>
              <rect x={cx - barW / 2} y={y(d.paid)} width={barW} height={base - y(d.paid)} rx="3" fill="var(--accent-2)">
                <title>{`${d.period} 결제 ₩${won(d.paid)}`}</title>
              </rect>
              <text x={cx} y={H - padB + 16} textAnchor="middle" fontSize="10.5" fill="var(--muted)">{d.period.slice(2)}</text>
            </g>
          );
        })}
      </svg>
    </div>
  );
}
