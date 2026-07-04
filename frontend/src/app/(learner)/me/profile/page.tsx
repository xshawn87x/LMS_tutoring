"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useSession } from "@/components/SessionProvider";
import { useFeatures } from "@/components/FeaturesProvider";
import { useToast } from "@/components/ToastProvider";
import {
  getProfile,
  InterestCategory,
  LEVEL_LABELS,
  listInterestCategories,
  saveProfile,
} from "@/lib/api";

export default function ProfilePage() {
  const { session, hydrated } = useSession();
  const { isEnabled, loaded: featuresLoaded } = useFeatures();
  const { showToast } = useToast();

  const [categories, setCategories] = useState<InterestCategory[]>([]);
  const [interests, setInterests] = useState<string[]>([]);
  const [levels, setLevels] = useState<Record<string, number>>({});
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    if (!session) return;
    try {
      const [cats, profile] = await Promise.all([
        listInterestCategories(session.token),
        getProfile(session.token),
      ]);
      setCategories(cats);
      setInterests(profile.interests);
      setLevels(Object.fromEntries(profile.skills.map((s) => [s.categoryCode, s.level])));
    } catch (e) {
      setError(e instanceof Error ? e.message : "불러오기 실패");
    }
  }, [session]);

  useEffect(() => {
    load();
  }, [load]);

  if (!hydrated || !session) return <p className="notice">불러오는 중… (로그인이 필요하면 <Link href="/login">여기</Link>)</p>;

  if (featuresLoaded && !isEnabled("DIAGNOSIS")) {
    return <div><h1>내 프로필</h1><p className="notice">이 기관에서는 역량 진단 기능이 비활성화되어 있습니다.</p></div>;
  }

  const toggleInterest = (code: string) => {
    setInterests((prev) => {
      if (prev.includes(code)) {
        setLevels((lv) => { const { [code]: _, ...rest } = lv; return rest; });
        return prev.filter((c) => c !== code);
      }
      setLevels((lv) => ({ ...lv, [code]: 0 }));
      return [...prev, code];
    });
  };

  const onSave = async () => {
    setBusy(true);
    setError(null);
    try {
      await saveProfile(session.token, {
        interests,
        skills: interests.map((code) => ({ categoryCode: code, level: levels[code] ?? 0 })),
      });
      showToast("프로필이 저장되었습니다");
    } catch (e) {
      setError(e instanceof Error ? e.message : "저장 실패");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div>
      <h1>내 프로필 — 관심분야 · 역량</h1>
      <p className="muted">바꾸면 <Link href="/">홈</Link>의 맞춤 추천에 바로 반영됩니다.</p>
      {error && <p className="error">{error}</p>}

      <h2>관심분야</h2>
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
          <h2>분야별 수준</h2>
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
        <button onClick={onSave} disabled={busy}>{busy ? "저장 중…" : "저장"}</button>
      </div>
    </div>
  );
}
