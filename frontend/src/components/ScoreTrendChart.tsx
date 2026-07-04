"use client";

// 성적 추이 라인 차트 — 외부 라이브러리 없이 인라인 SVG로 그린다.
// percent(0~100)를 시행일 순서대로 잇는다. 색은 CSS 변수(--accent)를 따라 브랜딩 반영.

export interface TrendPoint {
  date: string;      // 시행일 (표시용)
  percent: number;   // 0~100
  label?: string;    // 시험명/과목 (툴팁)
}

export function ScoreTrendChart({ data, height = 240 }: { data: TrendPoint[]; height?: number }) {
  if (!data || data.length === 0) {
    return <p className="notice">아직 성적 데이터가 없습니다.</p>;
  }

  const W = 680;
  const H = 260;
  const padL = 34;
  const padR = 18;
  const padT = 18;
  const padB = 40;
  const innerW = W - padL - padR;
  const innerH = H - padT - padB;
  const n = data.length;

  const x = (i: number) => (n === 1 ? padL + innerW / 2 : padL + (innerW * i) / (n - 1));
  const y = (v: number) => padT + innerH * (1 - Math.max(0, Math.min(100, v)) / 100);

  const gridVals = [0, 25, 50, 75, 100];
  const linePath = data.map((d, i) => `${i === 0 ? "M" : "L"}${x(i).toFixed(1)},${y(d.percent).toFixed(1)}`).join(" ");
  const areaPath = `${linePath} L${x(n - 1).toFixed(1)},${y(0).toFixed(1)} L${x(0).toFixed(1)},${y(0).toFixed(1)} Z`;

  // x축 라벨이 너무 많으면 솎아낸다
  const step = Math.ceil(n / 6);

  return (
    <div style={{ width: "100%", overflowX: "auto" }}>
      <svg viewBox={`0 0 ${W} ${H}`} width="100%" height={height} role="img" aria-label="성적 추이 그래프"
        style={{ display: "block", minWidth: 320 }}>
        <defs>
          <linearGradient id="scoreArea" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="var(--accent)" stopOpacity="0.22" />
            <stop offset="100%" stopColor="var(--accent)" stopOpacity="0" />
          </linearGradient>
        </defs>

        {/* 가로 그리드 + y축 라벨 */}
        {gridVals.map((v) => (
          <g key={v}>
            <line x1={padL} y1={y(v)} x2={W - padR} y2={y(v)} stroke="var(--border)" strokeWidth="1" />
            <text x={padL - 8} y={y(v) + 4} textAnchor="end" fontSize="11" fill="var(--muted)">{v}</text>
          </g>
        ))}

        {/* 면적 + 라인 */}
        <path d={areaPath} fill="url(#scoreArea)" />
        <path d={linePath} fill="none" stroke="var(--accent)" strokeWidth="2.5"
          strokeLinejoin="round" strokeLinecap="round" />

        {/* 점 + 값 라벨 + x축 라벨 */}
        {data.map((d, i) => (
          <g key={i}>
            <circle cx={x(i)} cy={y(d.percent)} r={i === n - 1 ? 5 : 3.5}
              fill="var(--panel)" stroke="var(--accent)" strokeWidth="2.5">
              <title>{`${d.label ? d.label + " · " : ""}${d.date} · ${d.percent}%`}</title>
            </circle>
            <text x={x(i)} y={y(d.percent) - 10} textAnchor="middle" fontSize="11" fontWeight="700"
              fill="var(--heading, var(--text))">{d.percent}</text>
            {(i % step === 0 || i === n - 1) && (
              <text x={x(i)} y={H - padB + 18} textAnchor="middle" fontSize="10.5" fill="var(--muted)">
                {d.date.slice(5)}
              </text>
            )}
          </g>
        ))}
      </svg>
    </div>
  );
}
