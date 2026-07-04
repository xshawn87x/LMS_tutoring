// 플랫폼 콘솔(슈퍼관리자) — 자체 토큰·사이드바를 가진 자족 화면이라 별도 크롬 없이 다크 기본.
export default function PlatformLayout({ children }: { children: React.ReactNode }) {
  return <main className="container">{children}</main>;
}
