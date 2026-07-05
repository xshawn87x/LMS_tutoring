import { LearnerAreaChrome } from "@/components/LearnerAreaChrome";

// 학습자 그룹 페이지 — 역할에 따라 크롬을 나눈다(강사·관리자=다크 운영, 학생·학부모=라이트 사이드바).
export default function LearnerGroupLayout({ children }: { children: React.ReactNode }) {
  return <LearnerAreaChrome>{children}</LearnerAreaChrome>;
}
