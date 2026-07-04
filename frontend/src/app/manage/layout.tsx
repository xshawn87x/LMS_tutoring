import { NavBar } from "@/components/NavBar";
import { PortalGuard } from "@/components/PortalGuard";

// 운영 포털(강사·관리자) — 상단 내비 + 집중형 다크. SSR부터 올바른 크롬으로 렌더.
export default function ManageLayout({ children }: { children: React.ReactNode }) {
  return (
    <>
      <NavBar />
      <main className="container">
        <PortalGuard>{children}</PortalGuard>
      </main>
    </>
  );
}
