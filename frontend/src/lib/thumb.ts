// 과정 카드 썸네일 유틸.
// 실제 업로드 이미지가 없으므로 예시 사진(Picsum, 시드 고정=매번 같은 사진)을 쓰고,
// 로드 실패 시엔 제목 기반 그라디언트가 뒤에서 자동으로 보이도록 폴백을 겹친다.

const CATEGORY_EMOJI: Record<string, string> = {
  dev: "💻", programming: "💻", web: "🌐", data: "📊", ai: "🤖", ml: "🤖",
  design: "🎨", business: "📈", marketing: "📣", language: "🗣️",
  math: "🔢", science: "🔬", music: "🎵", finance: "💰",
};

export function courseEmoji(categoryCode?: string | null): string {
  if (!categoryCode) return "📘";
  return CATEGORY_EMOJI[categoryCode.toLowerCase()] ?? "📘";
}

// 제목을 시드로 안정적인(매번 같은) 그라디언트 색을 만든다 — 이미지 폴백/오버레이용.
export function thumbGradient(seed: string): string {
  let h = 0;
  for (let i = 0; i < seed.length; i++) h = (h * 31 + seed.charCodeAt(i)) % 360;
  return `linear-gradient(135deg, hsl(${h} 66% 57%), hsl(${(h + 45) % 360} 70% 45%))`;
}

export function thumbStyle(seed: string): { background: string } {
  return { background: thumbGradient(seed) };
}

// 예시 사진 + 그라디언트 폴백을 겹친 배경. 이미지가 안 뜨면 그라디언트가 보인다.
export function thumbBg(seed: string): {
  backgroundImage: string; backgroundSize: string; backgroundPosition: string;
} {
  const img = `https://picsum.photos/seed/${encodeURIComponent(seed)}/640/360`;
  return {
    backgroundImage: `url("${img}"), ${thumbGradient(seed)}`,
    backgroundSize: "cover",
    backgroundPosition: "center",
  };
}
