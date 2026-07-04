"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useToast } from "@/components/ToastProvider";
import {
  AddonPriceView,
  AuditView,
  MarketContentItem,
  PlatformPlan,
  PlatformTenant,
  PricingView,
  SettlementItem,
  TenantBillingView,
  formatMoney,
  platformMarketCreate,
  platformMarketDelete,
  platformMarketList,
  platformSettlements,
  platformChangePlan,
  platformClosePeriod,
  platformGetAudit,
  platformGetBilling,
  platformGetPricing,
  platformGrantAddon,
  platformIssueInvoice,
  platformListPlans,
  platformListTenants,
  platformLogin,
  platformPayInvoice,
  platformReactivateTenant,
  platformRevoke,
  platformSuspendTenant,
  platformUpdateAddonPrice,
  platformUpdatePlanPrice,
} from "@/lib/api";

// 플랫폼(SaaS 제공자) 슈퍼관리자 토큰은 기관 세션과 완전히 별개다.
const PLATFORM_TOKEN_KEY = "lms.platform.token";

type View = "overview" | "tenants" | "pricing" | "audit" | "market";

const STATUS_LABEL: Record<string, string> = { ACTIVE: "정상", PAST_DUE: "연체", SUSPENDED: "정지" };

export default function PlatformConsole() {
  const { showToast } = useToast();
  const [token, setToken] = useState<string | null>(null);
  const [hydrated, setHydrated] = useState(false);

  // 로그인 폼
  const [email, setEmail] = useState("root@platform.local");
  const [password, setPassword] = useState("");
  const [loginError, setLoginError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  // 데이터
  const [plans, setPlans] = useState<PlatformPlan[]>([]);
  const [tenants, setTenants] = useState<PlatformTenant[]>([]);
  const [pricing, setPricing] = useState<PricingView | null>(null);
  const [billing, setBilling] = useState<Record<string, TenantBillingView>>({});
  const [audit, setAudit] = useState<AuditView[]>([]);
  const [market, setMarket] = useState<MarketContentItem[]>([]);
  const [settlements, setSettlements] = useState<SettlementItem[]>([]);
  const [mkTitle, setMkTitle] = useState("");
  const [mkPrice, setMkPrice] = useState("100000");
  const [mkProvider, setMkProvider] = useState("본사콘텐츠");
  const [error, setError] = useState<string | null>(null);
  const [rowBusy, setRowBusy] = useState<string | null>(null);

  // 가격 편집 초안(문자열)
  const [planDraft, setPlanDraft] = useState<Record<string, string>>({});
  const [addonDraft, setAddonDraft] = useState<Record<string, { monthlyPrice: string; unitPrice: string; includedUnits: string }>>({});

  // 네비게이션
  const [view, setView] = useState<View>("overview");
  const [selectedId, setSelectedId] = useState<string | null>(null);

  useEffect(() => {
    const saved = typeof window !== "undefined" ? window.localStorage.getItem(PLATFORM_TOKEN_KEY) : null;
    if (saved) setToken(saved);
    setHydrated(true);
  }, []);

  const loadBilling = useCallback(async (tk: string, tenantId: string) => {
    const b = await platformGetBilling(tk, tenantId);
    setBilling((prev) => ({ ...prev, [tenantId]: b }));
  }, []);

  const reloadAudit = useCallback(async (tk: string) => {
    try { setAudit(await platformGetAudit(tk)); } catch { /* 무시 */ }
  }, []);

  const reloadMarket = useCallback(async (tk: string) => {
    try {
      setMarket(await platformMarketList(tk));
      setSettlements(await platformSettlements(tk));
    } catch { /* 무시 */ }
  }, []);

  const load = useCallback(async (tk: string) => {
    setError(null);
    try {
      const [p, t, pr] = await Promise.all([
        platformListPlans(tk),
        platformListTenants(tk),
        platformGetPricing(tk),
      ]);
      setPlans(p);
      setTenants(t);
      setPricing(pr);
      setSelectedId((cur) => cur ?? (t[0]?.id ?? null));
      await Promise.all([...t.map((tenant) => loadBilling(tk, tenant.id)), reloadAudit(tk), reloadMarket(tk)]);
    } catch (e) {
      setError(e instanceof Error ? e.message : "불러오기 실패");
      setToken(null);
      window.localStorage.removeItem(PLATFORM_TOKEN_KEY);
    }
  }, [loadBilling]);

  useEffect(() => {
    if (token) load(token);
  }, [token, load]);

  // 가격 카탈로그 → 편집 초안 동기화
  useEffect(() => {
    if (!pricing) return;
    setPlanDraft(Object.fromEntries(pricing.plans.map((p) => [p.name, String(p.monthlyPrice)])));
    setAddonDraft(Object.fromEntries(pricing.addons.map((a) => [a.feature, {
      monthlyPrice: String(a.monthlyPrice), unitPrice: String(a.unitPrice), includedUnits: String(a.includedUnits),
    }])));
  }, [pricing]);

  const onLogin = async () => {
    setLoginError(null);
    setBusy(true);
    try {
      const res = await platformLogin(email, password);
      window.localStorage.setItem(PLATFORM_TOKEN_KEY, res.token);
      setToken(res.token);
      showToast("플랫폼 콘솔에 로그인했습니다");
    } catch (e) {
      setLoginError(e instanceof Error ? e.message : "로그인 실패");
    } finally {
      setBusy(false);
    }
  };

  const logout = () => {
    setToken(null);
    window.localStorage.removeItem(PLATFORM_TOKEN_KEY);
    setPassword("");
    setBilling({});
    setView("overview");
    setSelectedId(null);
  };

  const replaceTenant = (updated: PlatformTenant) =>
    setTenants((prev) => prev.map((t) => (t.id === updated.id ? updated : t)));

  const addonPrice = (feature: string): AddonPriceView | undefined =>
    pricing?.addons.find((a) => a.feature === feature);

  const priceLabel = (a: AddonPriceView): string =>
    a.pricingType === "FLAT"
      ? `월 ${formatMoney(a.monthlyPrice, a.currency)}`
      : `${formatMoney(a.unitPrice, a.currency)}/${a.unitLabel ?? "건"} · 월 ${a.includedUnits} 무료`;

  const planPrice = (name: string) => pricing?.plans.find((p) => p.name === name);
  const planPriceLabel = (name: string) => {
    const pp = planPrice(name);
    if (!pp) return "";
    return pp.monthlyPrice === 0 ? "무료" : `월 ${formatMoney(pp.monthlyPrice, pp.currency)}`;
  };

  const onChangePlan = async (tenant: PlatformTenant, plan: string) => {
    if (!token || plan === tenant.plan) return;
    setRowBusy(`${tenant.id}:plan`);
    try {
      replaceTenant(await platformChangePlan(token, tenant.id, plan));
      await loadBilling(token, tenant.id);
      showToast(`${tenant.orgCode} → ${plan} 요금제로 변경`);
    } catch (e) {
      setError(e instanceof Error ? e.message : "요금제 변경 실패");
    } finally {
      setRowBusy(null);
    }
  };

  const onToggleEntitlement = async (tenant: PlatformTenant, feature: string, entitled: boolean) => {
    if (!token) return;
    setRowBusy(`${tenant.id}:${feature}`);
    try {
      const updated = entitled
        ? await platformRevoke(token, tenant.id, feature)
        : await platformGrantAddon(token, tenant.id, feature);
      replaceTenant(updated);
      await loadBilling(token, tenant.id);
      showToast(entitled ? `${feature} 자격 회수` : `${feature} 애드온 부여`);
    } catch (e) {
      setError(e instanceof Error ? e.message : "자격 변경 실패");
    } finally {
      setRowBusy(null);
    }
  };

  const onIssueInvoice = async (tenant: PlatformTenant) => {
    if (!token) return;
    setRowBusy(`${tenant.id}:invoice`);
    try {
      await platformIssueInvoice(token, tenant.id);
      await loadBilling(token, tenant.id);
      showToast(`${tenant.orgCode} 인보이스 발행`);
    } catch (e) {
      setError(e instanceof Error ? e.message : "인보이스 발행 실패");
    } finally {
      setRowBusy(null);
    }
  };

  const onPayInvoice = async (tenant: PlatformTenant, invoiceId: string) => {
    if (!token) return;
    setRowBusy(`inv:${invoiceId}`);
    try {
      await platformPayInvoice(token, invoiceId);
      await loadBilling(token, tenant.id);
      showToast("결제 완료 (모의)");
    } catch (e) {
      setError(e instanceof Error ? e.message : "결제 실패");
    } finally {
      setRowBusy(null);
    }
  };

  // ACTIVE면 정지, 그 외(정지/연체)면 이용 재개(관리자 수동 override)
  const onToggleStatus = async (t: PlatformTenant) => {
    if (!token) return;
    setRowBusy(`${t.id}:status`);
    try {
      const updated = t.status === "ACTIVE"
        ? await platformSuspendTenant(token, t.id)
        : await platformReactivateTenant(token, t.id);
      replaceTenant(updated);
      await Promise.all([loadBilling(token, t.id), reloadAudit(token)]);
      showToast(updated.status === "SUSPENDED" ? `${t.orgCode} 정지됨` : `${t.orgCode} 이용 재개`);
    } catch (e) {
      setError(e instanceof Error ? e.message : "상태 변경 실패");
    } finally { setRowBusy(null); }
  };

  const onClosePeriod = async (period?: string) => {
    if (!token) return;
    setRowBusy("close-period");
    try {
      const issued = await platformClosePeriod(token, period);
      await Promise.all([...tenants.map((t) => loadBilling(token, t.id)), platformListTenants(token).then(setTenants), reloadAudit(token)]);
      showToast(`청구 마감 완료 · 인보이스 ${issued.length}건 발행`);
    } catch (e) {
      setError(e instanceof Error ? e.message : "청구 마감 실패");
    } finally { setRowBusy(null); }
  };

  const onEditPlanPrice = async (plan: string, monthlyPrice: number) => {
    if (!token) return;
    setRowBusy(`price:plan:${plan}`);
    try {
      await platformUpdatePlanPrice(token, plan, monthlyPrice);
      await Promise.all([platformGetPricing(token).then(setPricing), reloadAudit(token), ...tenants.map((t) => loadBilling(token, t.id))]);
      showToast(`${plan} 요금 수정됨`);
    } catch (e) {
      setError(e instanceof Error ? e.message : "가격 수정 실패");
    } finally { setRowBusy(null); }
  };

  const onEditAddonPrice = async (feature: string, body: { monthlyPrice: number; unitPrice: number; includedUnits: number }) => {
    if (!token) return;
    setRowBusy(`price:addon:${feature}`);
    try {
      await platformUpdateAddonPrice(token, feature, body);
      await Promise.all([platformGetPricing(token).then(setPricing), reloadAudit(token), ...tenants.map((t) => loadBilling(token, t.id))]);
      showToast(`${feature} 가격 수정됨`);
    } catch (e) {
      setError(e instanceof Error ? e.message : "가격 수정 실패");
    } finally { setRowBusy(null); }
  };

  // 집계 KPI
  const stats = useMemo(() => {
    const tenantCount = tenants.length;
    const paid = tenants.filter((t) => t.plan !== "FREE").length;
    let mrr = 0;
    let unpaid = 0;
    for (const t of tenants) {
      const b = billing[t.id];
      if (!b) continue;
      mrr += b.statement.total;
      unpaid += b.invoices.filter((i) => i.status === "ISSUED").reduce((s, i) => s + i.total, 0);
    }
    const dist: Record<string, number> = {};
    for (const p of plans) dist[p.name] = 0;
    for (const t of tenants) dist[t.plan] = (dist[t.plan] ?? 0) + 1;
    return { tenantCount, paid, mrr, unpaid, dist };
  }, [tenants, billing, plans]);

  if (!hydrated) return null;

  // ===== 로그인 =====
  if (!token) {
    return (
      <div className="pf-login">
        <h1>플랫폼 콘솔 <span className="badge">SUPER ADMIN</span></h1>
        <p className="muted">
          SaaS 제공자(플랫폼) 전용. 기관 경계를 넘어 각 기관의 <b>요금제·기능 자격·청구</b>를 관리합니다.
        </p>
        <div className="card">
          <h3>슈퍼관리자 로그인</h3>
          <label>이메일</label>
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="root@platform.local" />
          <label>비밀번호</label>
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="••••••••"
            onKeyDown={(e) => e.key === "Enter" && onLogin()} />
          {loginError && <p className="error">{loginError}</p>}
          <div style={{ marginTop: 16 }}>
            <button onClick={onLogin} disabled={busy}>{busy ? "확인 중…" : "로그인"}</button>
          </div>
          <p className="muted" style={{ marginTop: 12, marginBottom: 0 }}>
            로컬 기본 계정: <code>root@platform.local</code> / <code>platform-admin-pw</code>
          </p>
        </div>
      </div>
    );
  }

  const selected = tenants.find((t) => t.id === selectedId) ?? null;

  const navItems: { key: View; icon: string; label: string }[] = [
    { key: "overview", icon: "📊", label: "개요" },
    { key: "tenants", icon: "🏢", label: "테넌트 관리" },
    { key: "pricing", icon: "🏷️", label: "요금제 · 가격" },
    { key: "market", icon: "🛒", label: "콘텐츠 마켓" },
    { key: "audit", icon: "🧾", label: "감사 로그" },
  ];

  const onCreateContent = async () => {
    if (!token || !mkTitle.trim()) return;
    setRowBusy("mk-create");
    try {
      await platformMarketCreate(token, { title: mkTitle, price: Number(mkPrice) || 0, provider: mkProvider });
      setMkTitle(""); await reloadMarket(token); showToast("콘텐츠를 등록했습니다");
    } catch (e) { setError(e instanceof Error ? e.message : "등록 실패"); } finally { setRowBusy(null); }
  };
  const onDeleteContent = async (id: string) => {
    if (!token) return;
    try { await platformMarketDelete(token, id); await reloadMarket(token); showToast("삭제했습니다"); }
    catch (e) { setError(e instanceof Error ? e.message : "삭제 실패"); }
  };

  const statusPill = (status: string) => {
    const cls = status === "ACTIVE" ? "paid" : "issued";
    return <span className={`pf-pill ${cls}`}>{STATUS_LABEL[status] ?? status}</span>;
  };

  return (
    <div className="pf-shell">
      <div className="pf-wrap">
        <div className="pf-grid">
          {/* ===== 사이드바 ===== */}
          <aside className="pf-sidebar">
            <div className="pf-brand">⚙️ 플랫폼 콘솔 <span className="badge">SUPER</span></div>
            <nav className="pf-nav">
              {navItems.map((n) => (
                <button
                  key={n.key}
                  className={`pf-nav-item ${view === n.key ? "active" : ""}`}
                  onClick={() => setView(n.key)}
                >
                  <span className="ico">{n.icon}</span> {n.label}
                </button>
              ))}
              <div className="pf-nav-sep" />
              <div className="muted" style={{ padding: "2px 12px 6px", fontSize: 11, fontWeight: 700 }}>기관 바로가기</div>
              {tenants.map((t) => (
                <button
                  key={t.id}
                  className={`pf-nav-item ${view === "tenants" && selectedId === t.id ? "active" : ""}`}
                  onClick={() => { setView("tenants"); setSelectedId(t.id); }}
                >
                  <span className="ico">•</span> {t.orgCode}
                  <span className={`pf-plan-tag pf-plan-${t.plan}`} style={{ marginLeft: "auto" }}>{t.plan}</span>
                </button>
              ))}
              <div className="pf-nav-foot">
                <button className="pf-nav-item" onClick={logout}><span className="ico">↩</span> 로그아웃</button>
              </div>
            </nav>
          </aside>

          {/* ===== 콘텐츠 ===== */}
          <section className="pf-content">
            {error && <p className="error">{error}</p>}

            {view === "overview" && renderOverview()}
            {view === "tenants" && renderTenants()}
            {view === "pricing" && renderPricing()}
            {view === "audit" && renderAudit()}
            {view === "market" && renderMarket()}
          </section>
        </div>
      </div>
    </div>
  );

  // ===================== 뷰 렌더러 =====================

  function renderOverview() {
    return (
      <>
        <div className="pf-header">
          <div>
            <h1>개요</h1>
            <div className="pf-sub">모든 기관의 요금제·청구 현황 한눈에 보기</div>
          </div>
        </div>

        <div className="pf-kpis">
          <div className="pf-kpi">
            <div className="k-label">전체 기관</div>
            <div className="k-value">{stats.tenantCount}</div>
            <div className="k-foot">유료 {stats.paid} · 무료 {stats.tenantCount - stats.paid}</div>
          </div>
          <div className="pf-kpi">
            <div className="k-label">이번 달 예상 매출</div>
            <div className="k-value pf-money">{formatMoney(stats.mrr)}</div>
            <div className="k-foot">요금제 + 애드온 + 사용량</div>
          </div>
          <div className="pf-kpi">
            <div className="k-label">미결제 인보이스</div>
            <div className="k-value pf-money">{formatMoney(stats.unpaid)}</div>
            <div className="k-foot">발행됐으나 미결제</div>
          </div>
          <div className="pf-kpi">
            <div className="k-label">요금제 등급</div>
            <div className="k-value">{plans.length}</div>
            <div className="k-foot">{plans.map((p) => p.name).join(" · ")}</div>
          </div>
        </div>

        <div className="pf-panel">
          <h3>요금제 분포</h3>
          <div className="pf-dist">
            {plans.map((p) => {
              const n = stats.dist[p.name] ?? 0;
              const pct = stats.tenantCount ? Math.round((n / stats.tenantCount) * 100) : 0;
              const color = p.name === "PRO" ? "var(--accent-2)" : p.name === "STANDARD" ? "var(--accent)" : "var(--muted)";
              return (
                <div className="pf-dist-row" key={p.name}>
                  <span><span className={`pf-plan-tag pf-plan-${p.name}`}>{p.name}</span></span>
                  <span className="pf-dist-track"><span className="pf-dist-fill" style={{ width: `${pct}%`, background: color }} /></span>
                  <span className="muted" style={{ textAlign: "right" }}>{n}곳</span>
                </div>
              );
            })}
          </div>
        </div>

        <div className="pf-panel">
          <h3>운영 · 청구 마감</h3>
          <p className="muted" style={{ marginTop: 0 }}>
            모든 기관에 대해 해당 월의 인보이스를 일괄 발행하고 연체를 판정합니다.
            운영에선 매월 1일 새벽 스케줄러가 지난달을 자동 마감합니다.
          </p>
          <div className="row" style={{ alignItems: "center", gap: 10 }}>
            <button disabled={rowBusy === "close-period"} onClick={() => onClosePeriod()}>
              {rowBusy === "close-period" ? "마감 중…" : "이번 달 청구 마감 실행"}
            </button>
            <span className="muted" style={{ fontSize: 12 }}>수동 트리거(데모)</span>
          </div>
        </div>

        <div className="pf-panel">
          <h3>기관 요약</h3>
          <table className="grid">
            <thead>
              <tr><th>기관</th><th>요금제</th><th>상태</th><th style={{ textAlign: "right" }}>이번 달 예상</th><th style={{ textAlign: "right" }}>미결제</th><th></th></tr>
            </thead>
            <tbody>
              {tenants.map((t) => {
                const b = billing[t.id];
                const unpaid = b ? b.invoices.filter((i) => i.status === "ISSUED").reduce((s, i) => s + i.total, 0) : 0;
                return (
                  <tr key={t.id}>
                    <td><b>{t.name}</b> <span className="badge tenant">{t.orgCode}</span></td>
                    <td><span className={`pf-plan-tag pf-plan-${t.plan}`}>{t.plan}</span></td>
                    <td>{statusPill(t.status)}</td>
                    <td className="pf-money" style={{ textAlign: "right" }}>{b ? formatMoney(b.statement.total) : "—"}</td>
                    <td className="pf-money" style={{ textAlign: "right" }}>{unpaid ? formatMoney(unpaid) : "—"}</td>
                    <td style={{ textAlign: "right" }}>
                      <button className="ghost" onClick={() => { setView("tenants"); setSelectedId(t.id); }}>관리</button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </>
    );
  }

  function renderTenants() {
    return (
      <>
        <div className="pf-header">
          <div>
            <h1>테넌트 관리</h1>
            <div className="pf-sub">기관을 선택해 이용 서비스·자격·청구를 관리합니다</div>
          </div>
        </div>

        <div className="pf-td">
          {/* 좌: 기관 목록 */}
          <div className="pf-tenant-list">
            {tenants.map((t) => {
              const b = billing[t.id];
              return (
                <button
                  key={t.id}
                  className={`pf-tenant-card ${selectedId === t.id ? "active" : ""}`}
                  onClick={() => setSelectedId(t.id)}
                >
                  <div className="t-name">{t.name}</div>
                  <div className="t-meta">
                    <span className={`pf-plan-tag pf-plan-${t.plan}`}>{t.plan}</span>
                    {t.status !== "ACTIVE" && statusPill(t.status)}
                    <span style={{ marginLeft: "auto" }} className="pf-money">{b ? formatMoney(b.statement.total) : ""}</span>
                  </div>
                </button>
              );
            })}
          </div>

          {/* 우: 선택된 기관 상세 */}
          <div>{selected ? renderTenantDetail(selected) : <div className="pf-panel muted">왼쪽에서 기관을 선택하세요.</div>}</div>
        </div>
      </>
    );
  }

  function renderTenantDetail(t: PlatformTenant) {
    const b = billing[t.id];
    return (
      <>
        <div className="pf-panel">
          <div className="pf-detail-head">
            <h3>{t.name} <span className="badge tenant">{t.orgCode}</span> {statusPill(t.status)}</h3>
            <div className="row" style={{ width: "auto", alignItems: "center", gap: 8 }}>
              <label style={{ margin: 0 }}>요금제</label>
              <select
                value={t.plan}
                disabled={rowBusy === `${t.id}:plan`}
                onChange={(e) => onChangePlan(t, e.target.value)}
                style={{ width: "auto" }}
              >
                {plans.map((p) => (
                  <option key={p.name} value={p.name}>{p.displayName} — {planPriceLabel(p.name)}</option>
                ))}
              </select>
              <button
                className={t.status === "ACTIVE" ? "ghost" : "success"}
                disabled={rowBusy === `${t.id}:status`}
                onClick={() => onToggleStatus(t)}
              >
                {rowBusy === `${t.id}:status` ? "…" : t.status === "ACTIVE" ? "정지" : "이용 재개"}
              </button>
            </div>
          </div>
          {t.status !== "ACTIVE" && (
            <p className="error" style={{ marginTop: 0 }}>
              {t.status === "PAST_DUE" ? "연체 상태 — 미납 인보이스 결제 시 자동 해제됩니다." : "정지 상태 — 기능 이용이 차단됩니다."}
            </p>
          )}

          <h4 style={{ margin: "14px 0 8px", fontSize: 14, color: "var(--muted)" }}>이용 서비스 (기능 자격)</h4>
          <table className="grid">
            <thead>
              <tr><th>기능</th><th>자격</th><th>가격</th><th style={{ textAlign: "right" }}>작업</th></tr>
            </thead>
            <tbody>
              {t.features.map((f) => {
                const key = `${t.id}:${f.feature}`;
                const ap = addonPrice(f.feature);
                return (
                  <tr key={f.feature}>
                    <td>
                      {f.displayName} <span className="badge">{f.feature}</span>
                      {f.source && <span className="badge">{f.source}</span>}
                    </td>
                    <td>{f.entitled ? "✅ 있음" : "— 없음"}</td>
                    <td className="muted">{ap ? priceLabel(ap) : "요금제 전용"}</td>
                    <td style={{ textAlign: "right" }}>
                      <button
                        className={f.entitled ? "ghost" : "success"}
                        disabled={rowBusy === key}
                        onClick={() => onToggleEntitlement(t, f.feature, f.entitled)}
                      >
                        {rowBusy === key ? "…" : f.entitled ? "자격 회수" : "애드온 부여"}
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>

        {b && (
          <div className="pf-panel">
            <div className="pf-bill-total">
              <h3 style={{ margin: 0 }}>이번 달 예상 청구 <span className="muted" style={{ fontWeight: 400 }}>({b.statement.period})</span></h3>
              <span className="amt pf-money">{formatMoney(b.statement.total, b.statement.currency)}</span>
            </div>
            <table className="grid" style={{ marginTop: 10 }}>
              <tbody>
                {b.statement.lines.map((l, i) => (
                  <tr key={i}>
                    <td>
                      <span className="badge">{l.kind}</span> {l.label}
                      {l.detail && <div className="muted" style={{ fontSize: 12, marginTop: 2 }}>{l.detail}</div>}
                    </td>
                    <td className="pf-money" style={{ textAlign: "right" }}>{formatMoney(l.amount, b.statement.currency)}</td>
                  </tr>
                ))}
              </tbody>
            </table>

            <div className="row" style={{ marginTop: 12, alignItems: "center", gap: 10 }}>
              <button disabled={rowBusy === `${t.id}:invoice`} onClick={() => onIssueInvoice(t)}>
                {rowBusy === `${t.id}:invoice` ? "발행 중…" : "이번 달 인보이스 발행"}
              </button>
              <span className="muted" style={{ fontSize: 12 }}>발행 시 현재 명세가 스냅샷으로 저장됩니다</span>
            </div>

            {b.invoices.length > 0 && (
              <table className="grid" style={{ marginTop: 12 }}>
                <thead>
                  <tr><th>청구월</th><th>금액</th><th>상태</th><th style={{ textAlign: "right" }}>작업</th></tr>
                </thead>
                <tbody>
                  {b.invoices.map((inv) => (
                    <tr key={inv.id}>
                      <td>{inv.period}</td>
                      <td className="pf-money">{formatMoney(inv.total, inv.currency)}</td>
                      <td>
                        {inv.status === "PAID"
                          ? <span className="pf-pill paid">결제완료</span>
                          : <span className="pf-pill issued">미결제</span>}
                      </td>
                      <td style={{ textAlign: "right" }}>
                        {inv.status === "ISSUED" ? (
                          <button className="success" disabled={rowBusy === `inv:${inv.id}`} onClick={() => onPayInvoice(t, inv.id)}>
                            {rowBusy === `inv:${inv.id}` ? "…" : "결제(모의)"}
                          </button>
                        ) : (
                          <span className="muted" style={{ fontSize: 12 }}>{inv.paymentRef}</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        )}
      </>
    );
  }

  function renderPricing() {
    return (
      <>
        <div className="pf-header">
          <div>
            <h1>요금제 · 가격</h1>
            <div className="pf-sub">요금제 등급과 애드온 가격 카탈로그</div>
          </div>
        </div>

        <div className="pf-plans">
          {plans.map((p) => (
            <div className={`pf-plan-card ${p.name === "PRO" ? "hi" : ""}`} key={p.name}>
              <div><b>{p.displayName}</b> <span className={`pf-plan-tag pf-plan-${p.name}`}>{p.name}</span></div>
              <div className="row" style={{ alignItems: "center", gap: 6, margin: "10px 0 2px" }}>
                <input type="number" min={0} style={{ width: 120 }}
                  value={planDraft[p.name] ?? ""}
                  onChange={(e) => setPlanDraft((d) => ({ ...d, [p.name]: e.target.value }))} />
                <span className="muted">원/월</span>
                <button className="ghost" disabled={rowBusy === `price:plan:${p.name}`}
                  onClick={() => onEditPlanPrice(p.name, Number(planDraft[p.name] || 0))}>저장</button>
              </div>
              <ul>{p.features.map((f) => <li key={f}>{f}</li>)}</ul>
            </div>
          ))}
        </div>

        <div className="pf-panel">
          <h3>애드온 가격</h3>
          <table className="grid">
            <thead>
              <tr><th>기능</th><th>과금 방식</th><th>가격</th><th style={{ textAlign: "right" }}>수정</th></tr>
            </thead>
            <tbody>
              {(pricing?.addons ?? []).map((a) => {
                const d = addonDraft[a.feature] ?? { monthlyPrice: "0", unitPrice: "0", includedUnits: "0" };
                const setD = (patch: Partial<typeof d>) => setAddonDraft((prev) => ({ ...prev, [a.feature]: { ...d, ...patch } }));
                return (
                  <tr key={a.feature}>
                    <td>{a.displayName} <span className="badge">{a.feature}</span></td>
                    <td>{a.pricingType === "FLAT" ? "정액(월)" : "사용량"}</td>
                    <td>
                      {a.pricingType === "FLAT" ? (
                        <span className="row" style={{ gap: 4 }}>
                          <input type="number" min={0} style={{ width: 100 }} value={d.monthlyPrice}
                            onChange={(e) => setD({ monthlyPrice: e.target.value })} /> <span className="muted">원/월</span>
                        </span>
                      ) : (
                        <span className="row" style={{ gap: 4, flexWrap: "wrap" }}>
                          <input type="number" min={0} style={{ width: 80 }} value={d.unitPrice}
                            onChange={(e) => setD({ unitPrice: e.target.value })} /> <span className="muted">원/{a.unitLabel}</span>
                          <input type="number" min={0} style={{ width: 70 }} value={d.includedUnits}
                            onChange={(e) => setD({ includedUnits: e.target.value })} /> <span className="muted">무료</span>
                        </span>
                      )}
                    </td>
                    <td style={{ textAlign: "right" }}>
                      <button className="ghost" disabled={rowBusy === `price:addon:${a.feature}`}
                        onClick={() => onEditAddonPrice(a.feature, {
                          monthlyPrice: Number(d.monthlyPrice || 0),
                          unitPrice: Number(d.unitPrice || 0),
                          includedUnits: Number(d.includedUnits || 0),
                        })}>저장</button>
                    </td>
                  </tr>
                );
              })}
              {(!pricing || pricing.addons.length === 0) && (
                <tr><td colSpan={4} className="muted">등록된 애드온 가격이 없습니다.</td></tr>
              )}
            </tbody>
          </table>
          <p className="muted" style={{ marginBottom: 0 }}>
            코어 학습 기능은 미등록(요금제 티어로만 차등). 가격 변경은 감사 로그에 기록됩니다.
          </p>
        </div>
      </>
    );
  }

  function renderMarket() {
    const totalRevenue = settlements.reduce((s, x) => s + x.revenue, 0);
    return (
      <>
        <div className="pf-header">
          <div>
            <h1>콘텐츠 마켓</h1>
            <div className="pf-sub">판매 콘텐츠 등록 + 학원별 구매 정산</div>
          </div>
        </div>

        <div className="pf-panel">
          <h3>콘텐츠 등록</h3>
          <div className="row" style={{ gap: 8, alignItems: "flex-end", flexWrap: "wrap" }}>
            <div style={{ minWidth: 200 }}><label>제목</label><input value={mkTitle} onChange={(e) => setMkTitle(e.target.value)} placeholder="고1 수학 강의팩" /></div>
            <div style={{ minWidth: 120 }}><label>가격(원)</label><input type="number" value={mkPrice} onChange={(e) => setMkPrice(e.target.value)} /></div>
            <div style={{ minWidth: 140 }}><label>제공자</label><input value={mkProvider} onChange={(e) => setMkProvider(e.target.value)} /></div>
            <button disabled={rowBusy === "mk-create"} onClick={onCreateContent}>등록</button>
          </div>
        </div>

        <div className="pf-panel">
          <h3>판매 콘텐츠 ({market.length})</h3>
          <table className="grid">
            <thead><tr><th>제목</th><th>제공자</th><th style={{ textAlign: "right" }}>가격</th><th>노출</th><th></th></tr></thead>
            <tbody>
              {market.map((c) => (
                <tr key={c.id}>
                  <td>{c.title}</td><td className="muted">{c.provider ?? "-"}</td>
                  <td className="pf-money" style={{ textAlign: "right" }}>{formatMoney(c.price)}</td>
                  <td>{c.published ? "공개" : "비공개"}</td>
                  <td style={{ textAlign: "right" }}><button className="ghost" onClick={() => onDeleteContent(c.id)}>삭제</button></td>
                </tr>
              ))}
              {market.length === 0 && <tr><td colSpan={5} className="muted">등록된 콘텐츠가 없습니다.</td></tr>}
            </tbody>
          </table>
        </div>

        <div className="pf-panel">
          <div className="pf-bill-total">
            <h3 style={{ margin: 0 }}>정산 (전 학원)</h3>
            <span className="amt pf-money">{formatMoney(totalRevenue)}</span>
          </div>
          <table className="grid" style={{ marginTop: 8 }}>
            <thead><tr><th>콘텐츠</th><th>제공자</th><th style={{ textAlign: "right" }}>판매수</th><th style={{ textAlign: "right" }}>매출</th></tr></thead>
            <tbody>
              {settlements.map((s) => (
                <tr key={s.contentId}>
                  <td>{s.title}</td><td className="muted">{s.provider ?? "-"}</td>
                  <td style={{ textAlign: "right" }}>{s.purchaseCount}</td>
                  <td className="pf-money" style={{ textAlign: "right" }}>{formatMoney(s.revenue)}</td>
                </tr>
              ))}
              {settlements.length === 0 && <tr><td colSpan={4} className="muted">구매 내역이 없습니다.</td></tr>}
            </tbody>
          </table>
        </div>
      </>
    );
  }

  function renderAudit() {
    return (
      <>
        <div className="pf-header">
          <div>
            <h1>감사 로그</h1>
            <div className="pf-sub">플랫폼의 요금제·자격·가격·청구 변경 이력 (최근 100건)</div>
          </div>
        </div>
        <div className="pf-panel">
          <table className="grid">
            <thead>
              <tr><th>시각</th><th>행위자</th><th>동작</th><th>대상</th><th>내용</th></tr>
            </thead>
            <tbody>
              {audit.map((a) => (
                <tr key={a.id}>
                  <td className="muted" style={{ whiteSpace: "nowrap" }}>{new Date(a.createdAt).toLocaleString("ko-KR")}</td>
                  <td>{a.actor}</td>
                  <td><span className="badge">{a.action}</span></td>
                  <td className="muted">{a.targetType}{a.targetId ? ` · ${a.targetId.slice(0, 8)}` : ""}</td>
                  <td>{a.detail}</td>
                </tr>
              ))}
              {audit.length === 0 && <tr><td colSpan={5} className="muted">기록이 없습니다.</td></tr>}
            </tbody>
          </table>
        </div>
      </>
    );
  }
}
