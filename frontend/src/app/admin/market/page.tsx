"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useSession } from "@/components/SessionProvider";
import { useToast } from "@/components/ToastProvider";
import {
  ContentPurchaseItem,
  MarketContentItem,
  browseMarket,
  formatMoney,
  myMarketPurchases,
  purchaseContent,
} from "@/lib/api";

export default function MarketPage() {
  const { session } = useSession();
  const { showToast } = useToast();
  const [content, setContent] = useState<MarketContentItem[]>([]);
  const [purchases, setPurchases] = useState<ContentPurchaseItem[]>([]);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(async () => {
    if (!session) return;
    try { setContent(await browseMarket(session.token)); setPurchases(await myMarketPurchases(session.token)); }
    catch (e) { setError(e instanceof Error ? e.message : "실패"); }
  }, [session]);
  useEffect(() => { reload(); }, [reload]);

  if (!session) return <p className="notice">세션이 없습니다. <Link href="/login">로그인</Link>하세요.</p>;
  if (!session.roles.includes("ADMIN")) return <div><h1>콘텐츠 마켓</h1><p className="error">ADMIN만 접근할 수 있습니다.</p></div>;

  const purchasedIds = new Set(purchases.map((p) => p.contentId));
  const onBuy = async (c: MarketContentItem) => {
    try { await purchaseContent(session.token, c.id); showToast(`${c.title} 구매 완료`); await reload(); }
    catch (e) { setError(e instanceof Error ? e.message : "구매 실패"); }
  };

  return (
    <div>
      <h1>콘텐츠 마켓 <span className="badge tenant">{session.orgCode ?? session.tenantId.slice(0, 4)}</span></h1>
      <p className="muted">본사·제휴 콘텐츠를 둘러보고 학원에서 사용할 콘텐츠를 구매합니다.</p>
      {error && <p className="error">{error}</p>}

      <h2>콘텐츠 둘러보기</h2>
      {content.length === 0 ? <p className="notice">판매 중인 콘텐츠가 없습니다.</p> : content.map((c) => (
        <div className="card" key={c.id}>
          <div className="row" style={{ justifyContent: "space-between" }}>
            <div>
              <b>{c.title}</b> {c.category && <span className="badge">{c.category}</span>}
              {c.provider && <span className="muted"> · {c.provider}</span>}
              {c.description && <p className="muted" style={{ margin: "4px 0 0" }}>{c.description}</p>}
            </div>
            <div style={{ textAlign: "right" }}>
              <div className="pf-money" style={{ fontWeight: 700 }}>{formatMoney(c.price)}</div>
              {purchasedIds.has(c.id)
                ? <span className="pf-pill paid">구매완료</span>
                : <button className="success" onClick={() => onBuy(c)}>구매</button>}
            </div>
          </div>
        </div>
      ))}

      <h2>내 콘텐츠 보관함 ({purchases.length})</h2>
      {purchases.length === 0 ? <p className="notice">구매한 콘텐츠가 없습니다.</p> : (
        <table className="grid">
          <thead><tr><th>콘텐츠</th><th style={{ textAlign: "right" }}>금액</th><th>구매일</th></tr></thead>
          <tbody>
            {purchases.map((p) => {
              const c = content.find((x) => x.id === p.contentId);
              return (
                <tr key={p.id}>
                  <td>{c?.title ?? p.contentId.slice(0, 8)}</td>
                  <td className="pf-money" style={{ textAlign: "right" }}>{formatMoney(p.amount)}</td>
                  <td className="muted">{new Date(p.createdAt).toLocaleDateString("ko-KR")}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      )}
    </div>
  );
}
