import { LearnerLayout } from "@/components/LearnerLayout";

// 학습자 포털(학생·학부모) — 좌측 사이드바 + Coursera 라이트 테마.
// 라우트 그룹 레이아웃이라 이 그룹의 페이지는 SSR부터 학습자 크롬으로 렌더된다(플래시 없음).
export default function LearnerGroupLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="learner">
      <LearnerLayout>{children}</LearnerLayout>
    </div>
  );
}
