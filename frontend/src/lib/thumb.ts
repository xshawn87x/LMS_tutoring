// 과정 카드 썸네일 유틸 — 실제 이미지가 없으므로 제목에서 결정적 색상,
// 분야 코드에서 대표 이모지를 뽑아 컬러 타일을 만든다.

const CATEGORY_EMOJI: Record<string, string> = {
  dev: "💻", programming: "💻", web: "🌐", data: "📊", ai: "🤖", ml: "🤖",
  design: "🎨", business: "📈", marketing: "📣", language: "🗣️",
  math: "🔢", science: "🔬", music: "🎵", finance: "💰",
};

export function courseEmoji(categoryCode?: string | null): string {
  if (!categoryCode) return "📘";
  return CATEGORY_EMOJI[categoryCode.toLowerCase()] ?? "📘";
}

// 제목을 시드로 안정적인(매번 같은) 그라디언트 색을 만든다.
export function thumbStyle(seed: string): { background: string } {
  let h = 0;
  for (let i = 0; i < seed.length; i++) h = (h * 31 + seed.charCodeAt(i)) % 360;
  return { background: `linear-gradient(135deg, hsl(${h} 66% 57%), hsl(${(h + 45) % 360} 70% 45%))` };
}
