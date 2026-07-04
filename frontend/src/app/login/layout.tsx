import { PortalGuard } from "@/components/PortalGuard";

// 로그인 — 사이드바 없는 미니멀 라이트 화면(가운데 좁게).
export default function LoginLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="learner">
      <main className="container" style={{ maxWidth: 460 }}>
        <PortalGuard>{children}</PortalGuard>
      </main>
    </div>
  );
}
