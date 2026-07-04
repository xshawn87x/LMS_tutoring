"use client";

import { createContext, useContext, useEffect, useMemo, useState, ReactNode } from "react";
import { AuthResponse, fetchDevToken, setUnauthorizedHandler } from "@/lib/api";

export interface Session {
  token: string;
  tenantId: string;
  subject: string;
  roles: string[];
  displayName?: string | null;
  orgCode?: string;
}

interface SessionContextValue {
  session: Session | null;
  loading: boolean;
  hydrated: boolean; // localStorage에서 세션 복원 완료 여부 (true 전엔 리다이렉트 판단 보류)
  login: (tenantId: string, subject: string, roles: string[]) => Promise<void>;
  setAuth: (auth: AuthResponse) => void;
  updateDisplayName: (displayName: string | null) => void;
  logout: () => void;
}

const SessionContext = createContext<SessionContextValue | undefined>(undefined);

const STORAGE_KEY = "lms.session";

export function SessionProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(null);
  const [loading, setLoading] = useState(false);
  const [hydrated, setHydrated] = useState(false);

  // 새로고침해도 세션 유지
  useEffect(() => {
    const raw = typeof window !== "undefined" ? window.localStorage.getItem(STORAGE_KEY) : null;
    if (raw) {
      try {
        setSession(JSON.parse(raw) as Session);
      } catch {
        window.localStorage.removeItem(STORAGE_KEY);
      }
    }
    setHydrated(true);
  }, []);

  const login = async (tenantId: string, subject: string, roles: string[]) => {
    setLoading(true);
    try {
      const token = await fetchDevToken(tenantId, subject, roles);
      const next: Session = { token, tenantId, subject, roles };
      setSession(next);
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
    } finally {
      setLoading(false);
    }
  };

  // 실제 로그인/회원가입 응답으로 세션을 만든다 (토큰은 백엔드가 발급).
  const setAuth = (auth: AuthResponse) => {
    const next: Session = {
      token: auth.token,
      tenantId: auth.tenantId,
      subject: auth.subject,
      roles: auth.roles,
      displayName: auth.displayName,
      orgCode: auth.orgCode,
    };
    setSession(next);
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
  };

  const updateDisplayName = (displayName: string | null) => {
    setSession((prev) => {
      if (!prev) return prev;
      const next = { ...prev, displayName };
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
      return next;
    });
  };

  const logout = () => {
    setSession(null);
    window.localStorage.removeItem(STORAGE_KEY);
  };

  // 401(토큰 만료) 발생 시 자동 로그아웃 → 페이지 가드가 /login으로 보냄
  useEffect(() => {
    setUnauthorizedHandler(() => {
      setSession(null);
      window.localStorage.removeItem(STORAGE_KEY);
    });
    return () => setUnauthorizedHandler(null);
  }, []);

  const value = useMemo(
    () => ({ session, loading, hydrated, login, setAuth, updateDisplayName, logout }),
    [session, loading, hydrated],
  );
  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>;
}

export function useSession(): SessionContextValue {
  const ctx = useContext(SessionContext);
  if (!ctx) throw new Error("useSession must be used within SessionProvider");
  return ctx;
}
