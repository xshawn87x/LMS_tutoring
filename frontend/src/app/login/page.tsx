"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useSession } from "@/components/SessionProvider";
import { useToast } from "@/components/ToastProvider";
import { confirmPasswordReset, loginAccount, registerAccount, requestPasswordReset } from "@/lib/api";
import { homePathForRoles } from "@/lib/portal";

// 시드된 기관 코드 (V11). 운영에선 가입 시 발급/안내된다.
const ORGS = [
  { code: "acme", label: "Acme 러닝 (acme)" },
  { code: "globex", label: "Globex 아카데미 (globex)" },
];
const SIGNUP_ROLES = [
  { value: "STUDENT", label: "학생 (수강)" },
  { value: "INSTRUCTOR", label: "강사 (과정 개설)" },
  { value: "PARENT", label: "학부모 (자녀 현황 조회)" },
];

// dev 빠른 로그인용
const TENANTS = [
  { label: "테넌트 A = acme", value: "11111111-1111-1111-1111-111111111111" },
  { label: "테넌트 B = globex", value: "22222222-2222-2222-2222-222222222222" },
];
const ALL_ROLES = ["STUDENT", "PARENT", "INSTRUCTOR", "ADMIN"];

export default function LoginPage() {
  const { session, login, setAuth, loading, logout } = useSession();
  const router = useRouter();
  const { showToast } = useToast();

  const [mode, setMode] = useState<"login" | "register" | "reset">("login");
  // 비밀번호 재설정
  const [resetStep, setResetStep] = useState<"request" | "confirm">("request");
  const [resetToken, setResetToken] = useState("");
  const [resetNewPw, setResetNewPw] = useState("");
  const [orgCode, setOrgCode] = useState(ORGS[0].code);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [role, setRole] = useState("STUDENT");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // dev 빠른 로그인 상태
  const [showDev, setShowDev] = useState(false);
  const [devTenant, setDevTenant] = useState(TENANTS[0].value);
  const [devSubject, setDevSubject] = useState("alice");
  const [devRoles, setDevRoles] = useState<string[]>(["STUDENT"]);

  const onSubmit = async () => {
    setError(null);
    if (!email.trim() || !password) {
      setError("이메일과 비밀번호를 입력하세요");
      return;
    }
    if (mode === "register" && password.length < 8) {
      setError("비밀번호는 8자 이상이어야 합니다");
      return;
    }
    setBusy(true);
    try {
      const auth =
        mode === "register"
          ? await registerAccount({ orgCode, email, password, displayName: displayName || undefined, role })
          : await loginAccount({ orgCode, email, password });
      setAuth(auth);
      showToast(mode === "register" ? "회원가입이 완료되었습니다 🎉" : `환영합니다, ${auth.displayName ?? auth.subject}님`);
      router.push(homePathForRoles(auth.roles));
    } catch (e) {
      setError(e instanceof Error ? e.message : "요청 실패 (백엔드 실행 여부 확인)");
    } finally {
      setBusy(false);
    }
  };

  const toggleDevRole = (r: string) =>
    setDevRoles((prev) => (prev.includes(r) ? prev.filter((x) => x !== r) : [...prev, r]));

  const onDevLogin = async () => {
    setError(null);
    if (devRoles.length === 0) {
      setError("역할을 하나 이상 선택하세요");
      return;
    }
    try {
      await login(devTenant, devSubject || "dev-user", devRoles);
      router.push(homePathForRoles(devRoles));
    } catch (e) {
      setError(e instanceof Error ? e.message : "dev 로그인 실패");
    }
  };

  const onRequestReset = async () => {
    setError(null);
    if (!email.trim()) { setError("이메일을 입력하세요"); return; }
    setBusy(true);
    try {
      const res = await requestPasswordReset({ orgCode, email });
      setResetToken(res.token);   // 로컬: 토큰 자동 채움(운영에선 이메일로 전달)
      setResetStep("confirm");
      showToast("재설정 토큰이 발급되었습니다");
    } catch (e) {
      setError(e instanceof Error ? e.message : "요청 실패");
    } finally { setBusy(false); }
  };

  const onConfirmReset = async () => {
    setError(null);
    if (resetNewPw.length < 8) { setError("새 비밀번호는 8자 이상이어야 합니다"); return; }
    setBusy(true);
    try {
      await confirmPasswordReset({ orgCode, email, token: resetToken, newPassword: resetNewPw });
      showToast("비밀번호가 변경되었습니다. 로그인하세요");
      setMode("login"); setResetStep("request"); setPassword(""); setResetNewPw(""); setResetToken("");
    } catch (e) {
      setError(e instanceof Error ? e.message : "확정 실패");
    } finally { setBusy(false); }
  };

  return (
    <div>
      <h1>{mode === "login" ? "로그인" : mode === "register" ? "회원가입" : "비밀번호 찾기"}</h1>
      <p className="muted">
        기관(테넌트)·이메일·비밀번호로 로그인합니다. 비밀번호는 bcrypt로 안전하게 저장되며,
        로그인 시 발급되는 JWT의 <b>테넌트</b>로 보이는 데이터가(RLS), <b>역할</b>로 가능한 작업이(RBAC) 결정됩니다.
      </p>

      {session && (
        <div className="card">
          <h3>이미 로그인됨</h3>
          <p className="muted">
            {session.displayName ?? session.subject} · {session.orgCode ?? session.tenantId.slice(0, 8)} · {session.roles.join(", ")}
          </p>
          <div className="row">
            <button onClick={() => router.push(homePathForRoles(session.roles))}>홈으로</button>
            <button className="ghost" onClick={logout}>로그아웃</button>
          </div>
        </div>
      )}

      <div className="card">
        <div className="row" style={{ marginBottom: 8 }}>
          <button className={mode === "login" ? "" : "ghost"} onClick={() => setMode("login")}>로그인</button>
          <button className={mode === "register" ? "" : "ghost"} onClick={() => setMode("register")}>회원가입</button>
          <button className={mode === "reset" ? "" : "ghost"} onClick={() => { setMode("reset"); setResetStep("request"); }}>비밀번호 찾기</button>
        </div>

        <label>기관</label>
        <select value={orgCode} onChange={(e) => setOrgCode(e.target.value)}>
          {ORGS.map((o) => (
            <option key={o.code} value={o.code}>{o.label}</option>
          ))}
        </select>

        <label>이메일</label>
        <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="you@example.com" />

        {mode !== "reset" && (
          <>
            <label>비밀번호 {mode === "register" && <span className="muted">(8자 이상)</span>}</label>
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="••••••••" />
          </>
        )}

        {mode === "register" && (
          <>
            <label>이름 (표시용)</label>
            <input value={displayName} onChange={(e) => setDisplayName(e.target.value)} placeholder="홍길동" />
            <label>역할</label>
            <select value={role} onChange={(e) => setRole(e.target.value)}>
              {SIGNUP_ROLES.map((r) => (
                <option key={r.value} value={r.value}>{r.label}</option>
              ))}
            </select>
          </>
        )}

        {mode === "reset" && resetStep === "confirm" && (
          <>
            <label>재설정 토큰 <span className="muted">(로컬: 자동 입력됨)</span></label>
            <input value={resetToken} onChange={(e) => setResetToken(e.target.value)} placeholder="토큰" />
            <label>새 비밀번호 <span className="muted">(8자 이상)</span></label>
            <input type="password" value={resetNewPw} onChange={(e) => setResetNewPw(e.target.value)} placeholder="••••••••" />
          </>
        )}

        {error && <p className="error">{error}</p>}
        <div style={{ marginTop: 16 }}>
          {mode === "reset" ? (
            resetStep === "request" ? (
              <button onClick={onRequestReset} disabled={busy}>{busy ? "요청 중…" : "재설정 토큰 받기"}</button>
            ) : (
              <button onClick={onConfirmReset} disabled={busy}>{busy ? "변경 중…" : "새 비밀번호로 변경"}</button>
            )
          ) : (
          <button onClick={onSubmit} disabled={busy}>
            {busy ? "처리 중…" : mode === "register" ? "가입하고 시작" : "로그인"}
          </button>
          )}
        </div>
        {mode === "reset" && (
          <p className="muted" style={{ marginTop: 10, marginBottom: 0, fontSize: 12 }}>
            로컬 환경이라 토큰이 화면에 바로 표시됩니다. 운영에선 이메일/문자로 전달됩니다.
          </p>
        )}
      </div>

      {/* 개발용 빠른 로그인 (dev 토큰) */}
      <div className="card">
        <button className="ghost" onClick={() => setShowDev((v) => !v)}>
          {showDev ? "▼" : "▶"} 개발용 빠른 로그인 (dev 토큰 · 비밀번호 없이)
        </button>
        {showDev && (
          <div style={{ marginTop: 12 }}>
            <p className="muted">테스트용. 임의 테넌트·역할로 즉시 세션을 만듭니다(ADMIN 포함).</p>
            <label>테넌트</label>
            <select value={devTenant} onChange={(e) => setDevTenant(e.target.value)}>
              {TENANTS.map((t) => (
                <option key={t.value} value={t.value}>{t.label}</option>
              ))}
            </select>
            <label>사용자(subject)</label>
            <input value={devSubject} onChange={(e) => setDevSubject(e.target.value)} placeholder="alice" />
            <label>역할 (복수 선택)</label>
            <div className="row">
              {ALL_ROLES.map((r) => (
                <label key={r} className="row" style={{ width: "auto", margin: 0 }}>
                  <input type="checkbox" style={{ width: "auto" }} checked={devRoles.includes(r)} onChange={() => toggleDevRole(r)} />
                  <span style={{ marginLeft: 6 }}>{r}</span>
                </label>
              ))}
            </div>
            <div style={{ marginTop: 12 }}>
              <button className="ghost" onClick={onDevLogin} disabled={loading}>
                {loading ? "발급 중…" : "dev 세션 시작"}
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
