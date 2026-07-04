"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useSession } from "@/components/SessionProvider";
import { useFeatures } from "@/components/FeaturesProvider";
import {
  InterestCategory,
  LEVEL_LABELS,
  listInterestCategories,
  saveProfile,
} from "@/lib/api";

export default function OnboardingPage() {
  const { session, hydrated } = useSession();
  const { isEnabled, loaded: featuresLoaded } = useFeatures();
  const router = useRouter();

  const [categories, setCategories] = useState<InterestCategory[]>([]);
  const [interests, setInterests] = useState<string[]>([]);
  const [levels, setLevels] = useState<Record<string, number>>({});
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (hydrated && !session) router.replace("/login");
  }, [hydrated, session, router]);

  const load = useCallback(async () => {
    if (!session) return;
    try {
      setCategories(await listInterestCategories(session.token));
    } catch (e) {
      setError(e instanceof Error ? e.message : "불러오기 실패");
    }
  }, [session]);

  useEffect(() => {
    load();
  }, [load]);

  if (!hydrated || !session) return <p className="notice">불러오는 중…</p>;

  if (featuresLoaded && !isEnabled("DIAGNOSIS")) {
    return (
      <div>
        <h1>온보딩</h1>
        <p className="notice">이 기관에서는 역량 진단 기능이 비활성화되어 있습니다.</p>
      </div>
    );
  }

  const toggleInterest = (code: string) => {
    setInterests((prev) => {
      if (prev.includes(code)) {
        const next = prev.filter((c) => c !== code);
        setLevels((lv) => { const { [code]: _, ...rest } = lv; return rest; });
        return next;
      }
      setLevels((lv) => ({ ...lv, [code]: 0 }));
      return [...prev, code];
    });
  };

  const onSubmit = async () => {
    if (interests.length === 0) {
      setError("관심분야를 하나 이상 선택하세요");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await saveProfile(session.token, {
        interests,
        skills: interests.map((code) => ({ categoryCode: code, level: levels[code] ?? 0 })),
      });
      router.replace("/");
    } catch (e) {
      setError(e instanceof Error ? e.message : "저장 실패");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div>
      <h1>시작하기 — 나를 알려주세요</h1>
      <p className="muted">
        관심분야와 현재 수준을 선택하면 맞춤 과정을 추천해 드립니다. 나중에 “내 프로필”에서 언제든 바꿀 수 있어요.
      </p>
      {error && <p className="error">{error}</p>}

      <h2>1. 관심분야 선택</h2>
      <div className="row">
        {categories.map((c) => (
          <button
            key={c.code}
            className={interests.includes(c.code) ? "success" : "ghost"}
            onClick={() => toggleInterest(c.code)}
          >
            {c.name}
          </button>
        ))}
      </div>

      {interests.length > 0 && (
        <>
          <h2>2. 분야별 현재 수준</h2>
          {interests.map((code) => {
            const cat = categories.find((c) => c.code === code);
            return (
              <div className="card" key={code}>
                <label>{cat?.name ?? code}</label>
                <div className="row">
                  {LEVEL_LABELS.map((lab, lv) => (
                    <button
                      key={lv}
                      className={(levels[code] ?? 0) === lv ? "success" : "ghost"}
                      onClick={() => setLevels((prev) => ({ ...prev, [code]: lv }))}
                    >
                      {lab}
                    </button>
                  ))}
                </div>
              </div>
            );
          })}
        </>
      )}

      <div style={{ marginTop: 20 }}>
        <button onClick={onSubmit} disabled={busy}>{busy ? "저장 중…" : "완료하고 추천 받기"}</button>
      </div>
    </div>
  );
}
