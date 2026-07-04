"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState, ReactNode } from "react";
import { FeatureView, listFeatures } from "@/lib/api";
import { useSession } from "./SessionProvider";

interface FeaturesContextValue {
  features: FeatureView[];
  loaded: boolean;
  isEnabled: (name: string) => boolean;
  reload: () => Promise<void>;
}

const FeaturesContext = createContext<FeaturesContextValue | undefined>(undefined);

/**
 * 현재 테넌트의 기능 플래그를 로드해 UI 게이팅에 쓴다.
 * 세션(테넌트)이 바뀌면 다시 불러온다. 관리자가 토글하면 reload()로 갱신.
 */
export function FeaturesProvider({ children }: { children: ReactNode }) {
  const { session } = useSession();
  const [features, setFeatures] = useState<FeatureView[]>([]);
  const [loaded, setLoaded] = useState(false);

  const reload = useCallback(async () => {
    if (!session) {
      setFeatures([]);
      setLoaded(false);
      return;
    }
    try {
      setFeatures(await listFeatures(session.token));
      setLoaded(true);
    } catch {
      setFeatures([]);
      setLoaded(true); // 실패해도 로딩은 끝난 것으로 처리(게이팅이 영원히 막히지 않게)
    }
  }, [session]);

  useEffect(() => {
    setLoaded(false);
    reload();
  }, [reload]);

  const isEnabled = useCallback(
    (name: string) => features.some((f) => f.name === name && f.enabled),
    [features],
  );

  const value = useMemo(
    () => ({ features, loaded, isEnabled, reload }),
    [features, loaded, isEnabled, reload],
  );
  return <FeaturesContext.Provider value={value}>{children}</FeaturesContext.Provider>;
}

export function useFeatures(): FeaturesContextValue {
  const ctx = useContext(FeaturesContext);
  if (!ctx) throw new Error("useFeatures must be used within FeaturesProvider");
  return ctx;
}
