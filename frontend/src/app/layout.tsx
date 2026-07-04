import type { Metadata } from "next";
import "./globals.css";
import { SessionProvider } from "@/components/SessionProvider";
import { FeaturesProvider } from "@/components/FeaturesProvider";
import { ToastProvider } from "@/components/ToastProvider";

export const metadata: Metadata = {
  title: "LMS",
  description: "멀티테넌트 LMS — RLS 격리 + RBAC",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko">
      <head>
        {/* Pretendard — 브라우저에서 로드(런타임), 실패 시 시스템 폰트로 폴백 */}
        <link
          rel="stylesheet"
          href="https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/variable/pretendardvariable.min.css"
        />
      </head>
      <body>
        <SessionProvider>
          <FeaturesProvider>
            <ToastProvider>
              {children}
            </ToastProvider>
          </FeaturesProvider>
        </SessionProvider>
      </body>
    </html>
  );
}
