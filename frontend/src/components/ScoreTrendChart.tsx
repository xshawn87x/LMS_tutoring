"use client";

// 성적 추이 라인 차트 — 외부 라이브러리 없이 인라인 SVG.
// series(과목)가 여러 개면 과목별로 색을 나눠 다계열로, 하나면 단일 라인(브랜드색)으로 그린다.

export interface TrendPoint {
  date: string;      // 시행일 (YYYY-MM-DD)
  percent: number;   // 0~100
  label?: string;    // 툴팁(시험명)
  series?: string | null; // 계열(과목). 없으면 "전체"
}

const COLORS = ["#0056d2", "#1f8a4c", "#d97706", "#7c3aed", "#dc2626", "#0891b2", "#db2777"];

export function ScoreTrendChart({ data, height = 260 }: { data: TrendPoint[]; height?: number }) {
  if (!data || data.length === 0) {
    return <p className="notice">아직 성적 데이터가 없습니다.</p>;
  }

  const W = 700, H = 280, padL = 34, padR = 18, padT = 18, padB = 42;
  const innerW = W - padL - padR;
  const innerH = H - padT - padB;

  // 전체 날짜 축(중복 제거·정렬) — 계열 간 x 정렬을 맞춘다
  const dates = Array.from(new Set(data.map((d) => d.date))).sort();
  const xOf = (date: string) => {
    const i = dates.indexOf(date);
    return dates.length === 1 ? padL + innerW / 2 : padL + (innerW * i) / (dates.length - 1);
  };
  const y = (v: number) => padT + innerH * (1 - Math.max(0, Math.min(100, v)) / 100);

  // 계열별 그룹
  const seriesNames = Array.from(new Set(data.map((d) => d.series || "전체")));
  const multi = seriesNames.length > 1;
  const groups = seriesNames.map((name, idx) => ({
    name,
    color: multi ? COLORS[idx % COLORS.length] : "var(--accent)",
    points: data.filter((d) => (d.series || "전체") === name).slice().sort((a, b) => a.date.localeCompare(b.date)),
  }));

  const gridVals = [0, 25, 50, 75, 100];
  const step = Math.ceil(dates.length / 6);

  return (
    <div style={{ width: "100%", overflowX: "auto" }}>
      {multi && (
        <div className="row" style={{ gap: 14, marginBottom: 8, flexWrap: "wrap" }}>
          {groups.map((g) => (
            <span key={g.name} style={{ display: "inline-flex", alignItems: "center", gap: 6, fontSize: 12, fontWeight: 700 }}>
              <span style={{ width: 12, height: 12, borderRadius: 3, background: g.color, display: "inline-block" }} />
              {g.name}
            </span>
          ))}
        </div>
      )}
      <svg viewBox={`0 0 ${W} ${H}`} width="100%" height={height} role="img" aria-label="성적 추이 그래프"
        style={{ display: "block", minWidth: 320 }}>
        <defs>
          {!multi && (
            <linearGradient id="scoreArea" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="var(--accent)" stopOpacity="0.22" />
              <stop offset="100%" stopColor="var(--accent)" stopOpacity="0" />
            </linearGradient>
          )}
        </defs>

        {gridVals.map((v) => (
          <g key={v}>
            <line x1={padL} y1={y(v)} x2={W - padR} y2={y(v)} stroke="var(--border)" strokeWidth="1" />
            <text x={padL - 8} y={y(v) + 4} textAnchor="end" fontSize="11" fill="var(--muted)">{v}</text>
          </g>
        ))}

        {/* x축 라벨 */}
        {dates.map((d, i) => (
          (i % step === 0 || i === dates.length - 1) && (
            <text key={d} x={xOf(d)} y={H - padB + 18} textAnchor="middle" fontSize="10.5" fill="var(--muted)">{d.slice(5)}</text>
          )
        ))}

        {/* 계열별 라인 + 점 */}
        {groups.map((g) => {
          const path = g.points.map((p, i) => `${i === 0 ? "M" : "L"}${xOf(p.date).toFixed(1)},${y(p.percent).toFixed(1)}`).join(" ");
          const area = !multi ? `${path} L${xOf(g.points[g.points.length - 1].date).toFixed(1)},${y(0).toFixed(1)} L${xOf(g.points[0].date).toFixed(1)},${y(0).toFixed(1)} Z` : "";
          return (
            <g key={g.name}>
              {!multi && <path d={area} fill="url(#scoreArea)" />}
              <path d={path} fill="none" stroke={g.color} strokeWidth="2.5" strokeLinejoin="round" strokeLinecap="round" />
              {g.points.map((p, i) => (
                <g key={i}>
                  <circle cx={xOf(p.date)} cy={y(p.percent)} r={3.5} fill="var(--panel)" stroke={g.color} strokeWidth="2.5">
                    <title>{`${p.label ? p.label + " · " : ""}${g.name !== "전체" ? g.name + " · " : ""}${p.date} · ${p.percent}%`}</title>
                  </circle>
                  {!multi && (
                    <text x={xOf(p.date)} y={y(p.percent) - 10} textAnchor="middle" fontSize="11" fontWeight="700"
                      fill="var(--heading, var(--text))">{p.percent}</text>
                  )}
                </g>
              ))}
            </g>
          );
        })}
      </svg>
    </div>
  );
}
