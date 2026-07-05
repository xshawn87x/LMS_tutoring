"use client";

import Link from "next/link";
import { useSession } from "@/components/SessionProvider";
import { homePathForRoles } from "@/lib/portal";

const FEATURES = [
  { icon: "📈", title: "성적 추이·석차", desc: "과목별 성적 그래프와 반 내 석차·백분위를 자동 집계합니다." },
  { icon: "📊", title: "학부모 리포트", desc: "성적·출결·과제·진도를 한 장으로 요약해 학부모에게 자동 발송합니다." },
  { icon: "🧩", title: "성적 기반 반편성", desc: "평균 성적으로 레벨반을 추천·자동 배치합니다." },
  { icon: "🗓", title: "출결·자동 알림", desc: "결석 처리 즉시 학부모에게 인앱·이메일로 알립니다." },
  { icon: "🎥", title: "온라인 학습·수강관리", desc: "영상 학습·이어듣기·진도 추적과 수료 처리까지 한 번에." },
  { icon: "💳", title: "요금제·정산", desc: "학원별 요금제·자격·청구를 플랫폼에서 관리하고 정산합니다." },
];

export default function LandingPage() {
  const { session } = useSession();
  const primaryHref = session ? homePathForRoles(session.roles) : "/login";
  const primaryLabel = session ? "대시보드로 가기" : "무료로 시작하기";

  return (
    <div className="learner landing">
      {/* 상단 내비 */}
      <header className="lp-nav">
        <div className="lp-wrap lp-nav-inner">
          <span className="lp-logo">🎓 LMS<span className="lp-logo-accent"> for 학원</span></span>
          <nav className="lp-nav-links">
            <a href="#features">기능</a>
            <a href="#segment">보습·입시</a>
            <Link href="/platform" className="muted">플랫폼 콘솔</Link>
            <Link className="button" href={primaryHref}>{session ? "대시보드" : "로그인"}</Link>
          </nav>
        </div>
      </header>

      {/* 히어로 */}
      <section className="lp-hero">
        <div className="lp-wrap">
          <span className="lp-eyebrow">입시·보습학원 특화 LMS</span>
          <h1>성적부터 학부모 리포트까지,<br />학원 운영을 하나로</h1>
          <p>시험 성적 추이·석차, 출결 자동 알림, 성적 기반 반편성, 학부모 리포트 발송을 한 플랫폼에서. 멀티테넌트 SaaS로 여러 학원을 안전하게 운영합니다.</p>
          <div className="lp-cta">
            <Link className="button" href={primaryHref}>{primaryLabel}</Link>
            <Link className="button ghost" href="/login">학원 개설</Link>
          </div>
          <p className="lp-trust">신용카드 없이 몇 분이면 시작 · 학생·성적·학부모 소통을 한 곳에서</p>
        </div>
      </section>

      {/* 기능 */}
      <section className="lp-section" id="features">
        <div className="lp-wrap">
          <h2 className="lp-h2">학원 운영에 필요한 모든 것</h2>
          <p className="lp-sub">흩어진 도구 없이, 한 화면에서 성적·출결·리포트·수강을 관리하세요.</p>
          <div className="lp-features">
            {FEATURES.map((f) => (
              <div className="lp-feature" key={f.title}>
                <span className="lp-feature-ic">{f.icon}</span>
                <h3>{f.title}</h3>
                <p>{f.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* 세그먼트 피치 */}
      <section className="lp-section lp-segment" id="segment">
        <div className="lp-wrap lp-segment-grid">
          <div>
            <span className="lp-eyebrow">왜 보습·입시학원인가</span>
            <h2 className="lp-h2" style={{ marginTop: 8 }}>“모든 학원”이 아니라 한 세그먼트를 깊게</h2>
            <p className="lp-sub" style={{ maxWidth: 520 }}>
              모의고사 성적 추이, 반 내 석차, 결석 즉시 알림, 성적 기반 반편성 — 보습·입시학원이 매일 쓰는 흐름에 맞춰 설계했습니다.
            </p>
            <ul className="lp-check">
              <li>과목별 성적 추이 그래프 + 석차·백분위</li>
              <li>학부모 정기 리포트 자동 발송(인앱·이메일)</li>
              <li>평균 성적 기반 레벨반 자동 편성</li>
              <li>출결·상담·과제·자료실 통합</li>
            </ul>
          </div>
          <div className="lp-card-preview">
            <div className="lp-pv-row"><b>3월 모의고사</b><span className="chip accent">상위 12%</span></div>
            <div className="lp-pv-bar"><span style={{ width: "88%" }} /></div>
            <div className="lp-pv-row"><span className="muted">수학</span><b>92점</b></div>
            <div className="lp-pv-row"><span className="muted">영어</span><b>81점</b></div>
            <div className="lp-pv-row" style={{ marginTop: 8 }}><span className="muted">학부모 리포트</span><span className="chip free">발송됨</span></div>
          </div>
        </div>
      </section>

      {/* 하단 CTA */}
      <section className="lp-final">
        <div className="lp-wrap" style={{ textAlign: "center" }}>
          <h2 className="lp-h2" style={{ color: "#fff" }}>지금 학원을 무료로 시작하세요</h2>
          <p style={{ color: "rgba(255,255,255,0.85)", marginTop: 8 }}>몇 분이면 기관을 개설하고 학생·성적을 관리할 수 있습니다.</p>
          <div className="lp-cta" style={{ justifyContent: "center", marginTop: 18 }}>
            <Link className="button" href="/login" style={{ background: "#fff", color: "var(--accent)" }}>학원 개설하기</Link>
            <Link className="button ghost" href={primaryHref} style={{ borderColor: "rgba(255,255,255,0.5)", color: "#fff" }}>{session ? "대시보드" : "로그인"}</Link>
          </div>
        </div>
      </section>

      <footer className="lp-footer">
        <div className="lp-wrap">🎓 LMS for 학원 · 멀티테넌트 SaaS · © 2026</div>
      </footer>
    </div>
  );
}
