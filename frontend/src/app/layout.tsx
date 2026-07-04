import type { Metadata } from "next";
import "./globals.css";
import { SessionProvider } from "@/components/SessionProvider";
import { FeaturesProvider } from "@/components/FeaturesProvider";
import { ToastProvider } from "@/components/ToastProvider";
import { NavBar } from "@/components/NavBar";

export const metadata: Metadata = {
  title: "LMS",
  description: "멀티테넌트 LMS — RLS 격리 + RBAC",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko">
      <body>
        <SessionProvider>
          <FeaturesProvider>
            <ToastProvider>
              <NavBar />
              <main className="container">{children}</main>
            </ToastProvider>
          </FeaturesProvider>
        </SessionProvider>
      </body>
    </html>
  );
}
