"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useSession } from "@/components/SessionProvider";
import { useToast } from "@/components/ToastProvider";
import {
  Course,
  createCourse,
  InterestCategory,
  LEVEL_LABELS,
  levelLabel,
  listCourses,
  listInterestCategories,
} from "@/lib/api";
import { courseEmoji, thumbStyle } from "@/lib/thumb";

export default function CoursesPage() {
  const { session } = useSession();
  const { showToast } = useToast();
  const [courses, setCourses] = useState<Course[]>([]);
  const [categories, setCategories] = useState<InterestCategory[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [categoryCode, setCategoryCode] = useState("");
  const [level, setLevel] = useState(0);
  const [busy, setBusy] = useState(false);

  // 탐색(검색·필터·정렬)
  const [query, setQuery] = useState("");
  const [filterCat, setFilterCat] = useState("");
  const [sortBy, setSortBy] = useState<"title" | "levelAsc" | "levelDesc">("title");

  const canCreate = !!session && (session.roles.includes("INSTRUCTOR") || session.roles.includes("ADMIN"));

  const categoryName = (code: string | null) =>
    code ? categories.find((c) => c.code === code)?.name ?? code : null;

  const load = useCallback(async () => {
    if (!session) return;
    setError(null);
    try {
      const [cs, cats] = await Promise.all([
        listCourses(session.token),
        listInterestCategories(session.token),
      ]);
      setCourses(cs);
      setCategories(cats);
    } catch (e) {
      setError(e instanceof Error ? e.message : "불러오기 실패");
    }
  }, [session]);

  useEffect(() => {
    load();
  }, [load]);

  if (!session) {
    return <p className="notice">세션이 없습니다. <Link href="/login">여기서 로그인</Link>하세요.</p>;
  }

  const onCreate = async () => {
    if (!title.trim()) return;
    setBusy(true);
    setError(null);
    try {
      await createCourse(session.token, {
        title,
        description: description || undefined,
        categoryCode: categoryCode || undefined,
        level,
      });
      setTitle("");
      setDescription("");
      setCategoryCode("");
      setLevel(0);
      showToast("과정이 생성되었습니다");
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : "생성 실패");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div>
      <h1>과정 탐색</h1>
      <p className="muted">우리 학원에서 열린 과정을 둘러보고 원하는 강의를 시작하세요.</p>

      {error && <p className="error">{error}</p>}

      {/* 탐색: 검색 + 분야 필터 + 정렬 */}
      {courses.length > 0 && (
        <div className="card">
          <div className="row">
            <input
              style={{ flex: 2, minWidth: 180 }}
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="🔍 과정명·설명 검색"
            />
            <select style={{ flex: 1, minWidth: 120 }} value={filterCat} onChange={(e) => setFilterCat(e.target.value)}>
              <option value="">전체 분야</option>
              {categories.map((c) => <option key={c.code} value={c.code}>{c.name}</option>)}
            </select>
            <select style={{ flex: 1, minWidth: 120 }} value={sortBy} onChange={(e) => setSortBy(e.target.value as typeof sortBy)}>
              <option value="title">제목순</option>
              <option value="levelAsc">난이도 낮은순</option>
              <option value="levelDesc">난이도 높은순</option>
            </select>
          </div>
        </div>
      )}

      {(() => {
        const q = query.trim().toLowerCase();
        const visible = courses
          .filter((c) => !filterCat || c.categoryCode === filterCat)
          .filter((c) => !q || c.title.toLowerCase().includes(q) || (c.description ?? "").toLowerCase().includes(q))
          .sort((a, b) => {
            if (sortBy === "title") return a.title.localeCompare(b.title, "ko");
            const la = a.level ?? 0, lb = b.level ?? 0;
            return sortBy === "levelAsc" ? la - lb : lb - la;
          });
        if (courses.length === 0) return <p className="notice">과정이 없습니다.</p>;
        if (visible.length === 0) return <p className="notice">조건에 맞는 과정이 없습니다.</p>;
        return (
          <>
            <p className="muted" style={{ marginBottom: 14 }}>{visible.length}개 과정</p>
            <div className="course-grid">
              {visible.map((c) => (
                <Link className="course-card" href={`/courses/${c.id}`} key={c.id}>
                  <div className="course-thumb" style={thumbStyle(c.title)}>{courseEmoji(c.categoryCode)}</div>
                  <div className="cc-body">
                    <span className="cc-title">{c.title}</span>
                    <span className="cc-desc">{c.description ?? "설명이 없습니다."}</span>
                    <div className="cc-meta">
                      {c.categoryCode && <span className="chip">{categoryName(c.categoryCode)}</span>}
                      {c.level != null && <span className="chip">{levelLabel(c.level)}</span>}
                      {c.tuitionFee > 0
                        ? <span className="chip paid">{c.tuitionFee.toLocaleString("ko-KR")}원</span>
                        : <span className="chip free">무료</span>}
                      {!c.published && <span className="chip">비공개</span>}
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          </>
        );
      })()}

      {canCreate ? (
        <>
          <h2>새 과정 만들기 <span className="badge role">INSTRUCTOR/ADMIN</span></h2>
          <div className="card">
            <label>제목</label>
            <input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="예: Spring 심화" />
            <label>설명</label>
            <input value={description} onChange={(e) => setDescription(e.target.value)} />
            <div className="row">
              <div style={{ flex: 1 }}>
                <label>분야 (추천 매칭에 사용)</label>
                <select value={categoryCode} onChange={(e) => setCategoryCode(e.target.value)}>
                  <option value="">— 미지정 —</option>
                  {categories.map((c) => <option key={c.code} value={c.code}>{c.name}</option>)}
                </select>
              </div>
              <div style={{ flex: 1 }}>
                <label>난이도</label>
                <select value={level} onChange={(e) => setLevel(Number(e.target.value))}>
                  {LEVEL_LABELS.map((lab, lv) => <option key={lv} value={lv}>{lab}</option>)}
                </select>
              </div>
            </div>
            <div style={{ marginTop: 14 }}>
              <button onClick={onCreate} disabled={busy}>{busy ? "생성 중…" : "과정 생성"}</button>
            </div>
          </div>
        </>
      ) : (
        <p className="muted">과정 생성은 INSTRUCTOR/ADMIN 역할만 가능합니다. (현재: {session.roles.join(", ")})</p>
      )}
    </div>
  );
}
