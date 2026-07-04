// 백엔드(Spring Boot) REST API 클라이언트.
// 모든 /api 호출에 JWT Bearer 토큰을 싣는다. 토큰의 tenant_id로 테넌트가,
// roles로 권한이 결정된다 (격리·권한은 백엔드 RLS/RBAC가 강제).

const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080";

// 업로드 영상은 백엔드(/media/..)가 제공한다. 상대 경로면 백엔드 오리진을 붙여 절대 URL로.
export const resolveMediaUrl = (url: string | null | undefined): string | null =>
  url ? (url.startsWith("/") ? `${API_BASE}${url}` : url) : null;

export interface Course {
  id: string;
  tenantId: string;
  title: string;
  description: string | null;
  categoryCode: string | null;
  level: number | null;
  published: boolean;
  tuitionFee: number;
  createdAt: string;
}

export interface Lesson {
  id: string;
  courseId: string;
  title: string;
  content: string | null;
  videoUrl: string | null;
  orderNo: number;
  createdAt: string;
}

export interface LessonProgress {
  lessonId: string;
  courseId: string;
  lastPositionSeconds: number;
  completed: boolean;
}

// 레슨에 video_url이 없을 때 학습창이 재생할 샘플 영상 (저작권 free, Google 공개 샘플)
export const SAMPLE_VIDEO =
  "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4";

export interface Enrollment {
  id: string;
  courseId: string;
  studentId: string;
  status: "ACTIVE" | "COMPLETED";
  progress: number;
  enrolledAt: string;
  updatedAt: string;
}

export class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message);
  }
}

// 토큰 만료/무효(401)를 전역에서 처리하기 위한 훅. SessionProvider가 등록한다.
let onUnauthorized: (() => void) | null = null;
export function setUnauthorizedHandler(fn: (() => void) | null) {
  onUnauthorized = fn;
}

async function request<T>(path: string, token: string | null, init?: RequestInit): Promise<T> {
  const headers: Record<string, string> = { ...(init?.headers as Record<string, string>) };
  if (token) headers["Authorization"] = `Bearer ${token}`;
  if (init?.body) headers["Content-Type"] = "application/json";

  const res = await fetch(`${API_BASE}${path}`, { ...init, headers });
  if (!res.ok) {
    // 토큰을 보냈는데 401이면 만료/무효 → 세션 정리(자동 로그아웃)
    if (res.status === 401 && token) {
      onUnauthorized?.();
    }
    let msg = `${res.status} ${res.statusText}`;
    if (res.status === 403) msg = "권한이 없습니다 (역할 부족이거나 비활성 기능)";
    // 토큰이 있으면 만료/무효(세션 정리), 없으면 로그인 시도 실패(자격 증명 오류)
    else if (res.status === 401) msg = token ? "세션이 만료되었습니다. 다시 로그인해 주세요" : "이메일 또는 비밀번호가 올바르지 않습니다";
    else if (res.status === 404) msg = "찾을 수 없습니다 (다른 테넌트이거나 없음)";
    else if (res.status === 409) msg = "이미 수강 중입니다";
    throw new ApiError(res.status, msg);
  }
  if (res.status === 204) return undefined as T;
  const text = await res.text();
  return text ? (JSON.parse(text) as T) : (undefined as T);
}

// --- dev 토큰 발급 (로컬 전용) ---
export async function fetchDevToken(tenantId: string, subject: string, roles: string[]): Promise<string> {
  const qs = new URLSearchParams({ tenantId, subject, roles: roles.join(",") });
  const data = await request<{ token: string }>(`/dev/token?${qs.toString()}`, null);
  return data.token;
}

// --- 실제 회원가입/로그인 ---
export interface AuthResponse {
  token: string;
  subject: string;   // = email
  tenantId: string;
  orgCode: string;
  displayName: string | null;
  roles: string[];
}

export const registerAccount = (body: {
  orgCode: string;
  email: string;
  password: string;
  displayName?: string;
  role?: string;
}) => request<AuthResponse>("/api/auth/register", null, { method: "POST", body: JSON.stringify(body) });

export const loginAccount = (body: { orgCode: string; email: string; password: string }) =>
  request<AuthResponse>("/api/auth/login", null, { method: "POST", body: JSON.stringify(body) });

// 비밀번호 재설정 (로컬: 요청 시 토큰을 응답으로 받음)
export const requestPasswordReset = (body: { orgCode: string; email: string }) =>
  request<{ token: string; message: string }>("/api/auth/password-reset/request", null, {
    method: "POST", body: JSON.stringify(body),
  });
export const confirmPasswordReset = (body: { orgCode: string; email: string; token: string; newPassword: string }) =>
  request<void>("/api/auth/password-reset/confirm", null, { method: "POST", body: JSON.stringify(body) });

// --- 학원 관리자: 회원/강사 관리 ---
export interface Member {
  id: string;
  email: string;
  displayName: string | null;
  roles: string[];
  createdAt: string;
}
export const listMembers = (token: string) => request<Member[]>("/api/admin/members", token);
export const createMember = (token: string, body: { email: string; password: string; displayName?: string; roles: string[] }) =>
  request<Member>("/api/admin/members", token, { method: "POST", body: JSON.stringify(body) });
export const updateMemberRoles = (token: string, id: string, roles: string[]) =>
  request<Member>(`/api/admin/members/${id}/roles`, token, { method: "PUT", body: JSON.stringify({ roles }) });
export const resetMemberPassword = (token: string, id: string, newPassword: string) =>
  request<void>(`/api/admin/members/${id}/reset-password`, token, { method: "POST", body: JSON.stringify({ newPassword }) });
export const deleteMember = (token: string, id: string) =>
  request<void>(`/api/admin/members/${id}`, token, { method: "DELETE" });

// --- Q&A 학습질문 ---
export interface QuestionSummary {
  id: string; courseId: string; author: string; title: string;
  resolved: boolean; answerCount: number; createdAt: string;
}
export interface QnaAnswer { id: string; author: string; body: string; createdAt: string; }
export interface QuestionThread {
  id: string; courseId: string; author: string; title: string; body: string | null;
  resolved: boolean; createdAt: string; answers: QnaAnswer[];
}
export const listQuestions = (token: string, courseId: string) =>
  request<QuestionSummary[]>(`/api/courses/${courseId}/questions`, token);
export const getQuestionThread = (token: string, questionId: string) =>
  request<QuestionThread>(`/api/questions/${questionId}`, token);
export const askQuestion = (token: string, courseId: string, body: { title: string; body?: string }) =>
  request<QuestionThread>(`/api/courses/${courseId}/questions`, token, { method: "POST", body: JSON.stringify(body) });
export const answerQuestion = (token: string, questionId: string, body: string) =>
  request<QnaAnswer>(`/api/questions/${questionId}/answers`, token, { method: "POST", body: JSON.stringify({ body }) });
export const resolveQuestion = (token: string, questionId: string, resolved: boolean) =>
  request<QuestionThread>(`/api/questions/${questionId}/resolve?resolved=${resolved}`, token, { method: "POST" });
export const deleteQnaQuestion = (token: string, questionId: string) =>
  request<void>(`/api/questions/${questionId}`, token, { method: "DELETE" });

// --- 과제 ---
export interface Assignment {
  id: string; courseId: string; title: string; description: string | null;
  dueAt: string | null; maxScore: number; createdAt: string;
}
export interface AssignmentSubmission {
  id: string; assignmentId: string; student: string; textAnswer: string | null; fileUrl: string | null;
  submittedAt: string; score: number | null; feedback: string | null; gradedAt: string | null;
}
export const listAssignments = (token: string, courseId: string) =>
  request<Assignment[]>(`/api/courses/${courseId}/assignments`, token);
export const createAssignment = (token: string, courseId: string, body: { title: string; description?: string; dueAt?: string | null; maxScore: number }) =>
  request<Assignment>(`/api/courses/${courseId}/assignments`, token, { method: "POST", body: JSON.stringify(body) });
export const submitAssignment = (token: string, id: string, body: { textAnswer?: string; fileUrl?: string }) =>
  request<AssignmentSubmission>(`/api/assignments/${id}/submissions`, token, { method: "POST", body: JSON.stringify(body) });
export const myAssignmentSubmission = (token: string, id: string) =>
  request<AssignmentSubmission>(`/api/assignments/${id}/my-submission`, token);
export const listAssignmentSubmissions = (token: string, id: string) =>
  request<AssignmentSubmission[]>(`/api/assignments/${id}/submissions`, token);
export const gradeSubmission = (token: string, submissionId: string, body: { score: number; feedback?: string }) =>
  request<AssignmentSubmission>(`/api/submissions/${submissionId}/grade`, token, { method: "POST", body: JSON.stringify(body) });

// --- 계정 관리 (로그인 사용자) ---
export const changePassword = (token: string, body: { currentPassword: string; newPassword: string }) =>
  request<void>("/api/me/password", token, { method: "POST", body: JSON.stringify(body) });

export const updateAccount = (token: string, body: { displayName: string }) =>
  request<{ email: string; displayName: string | null; roles: string[] }>(
    "/api/me/account", token, { method: "PUT", body: JSON.stringify(body) });

// --- 과정 ---
export const listCourses = (token: string) => request<Course[]>("/api/courses", token);
export const createCourse = (
  token: string,
  body: { title: string; description?: string; categoryCode?: string; level?: number },
) => request<Course>("/api/courses", token, { method: "POST", body: JSON.stringify(body) });
export const getCourse = (token: string, id: string) => request<Course>(`/api/courses/${id}`, token);
export const updateCourse = (
  token: string,
  id: string,
  body: { title: string; description?: string; categoryCode?: string; level?: number },
) => request<Course>(`/api/courses/${id}`, token, { method: "PUT", body: JSON.stringify(body) });
export const deleteCourse = (token: string, id: string) =>
  request<void>(`/api/courses/${id}`, token, { method: "DELETE" });

// --- 레슨 ---
export const listLessons = (token: string, courseId: string) =>
  request<Lesson[]>(`/api/courses/${courseId}/lessons`, token);
export const addLesson = (
  token: string,
  courseId: string,
  body: { title: string; content?: string; videoUrl?: string; orderNo: number },
) => request<Lesson>(`/api/courses/${courseId}/lessons`, token, { method: "POST", body: JSON.stringify(body) });
export const updateLesson = (
  token: string,
  courseId: string,
  lessonId: string,
  body: { title: string; content?: string; videoUrl?: string; orderNo: number },
) => request<Lesson>(`/api/courses/${courseId}/lessons/${lessonId}`, token, { method: "PUT", body: JSON.stringify(body) });
export const deleteLesson = (token: string, courseId: string, lessonId: string) =>
  request<void>(`/api/courses/${courseId}/lessons/${lessonId}`, token, { method: "DELETE" });

// 동영상 업로드 (multipart) → { url, filename }. url을 레슨 videoUrl로 저장.
export async function uploadVideo(token: string, file: File): Promise<{ url: string; filename: string }> {
  const form = new FormData();
  form.append("file", file);
  const res = await fetch(`${API_BASE}/api/uploads/video`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` }, // Content-Type은 브라우저가 boundary와 함께 자동 설정
    body: form,
  });
  if (!res.ok) {
    if (res.status === 401) onUnauthorized?.();
    throw new ApiError(res.status, res.status === 413 ? "파일이 너무 큽니다" : "업로드 실패");
  }
  return res.json();
}

// 범용 파일 업로드 (과제 첨부·자료실) → { url, filename }
export async function uploadFile(token: string, file: File): Promise<{ url: string; filename: string }> {
  const form = new FormData();
  form.append("file", file);
  const res = await fetch(`${API_BASE}/api/uploads/file`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
    body: form,
  });
  if (!res.ok) {
    if (res.status === 401) onUnauthorized?.();
    throw new ApiError(res.status, res.status === 413 ? "파일이 너무 큽니다" : "업로드 실패");
  }
  return res.json();
}

// --- 학습창 진도(이어듣기 + 완료) ---
export const getLessonProgress = (token: string, courseId: string) =>
  request<LessonProgress[]>(`/api/courses/${courseId}/lesson-progress`, token);

export const saveLessonProgress = (
  token: string,
  lessonId: string,
  body: { lastPositionSeconds: number; completed: boolean },
) => request<LessonProgress>(`/api/lessons/${lessonId}/progress`, token, {
  method: "PUT",
  body: JSON.stringify(body),
});

// --- 수강신청 ---
export const enroll = (token: string, courseId: string) =>
  request<Enrollment>(`/api/courses/${courseId}/enrollments`, token, { method: "POST" });
export const myEnrollments = (token: string) => request<Enrollment[]>("/api/enrollments/me", token);
export const updateProgress = (token: string, enrollmentId: string, progress: number) =>
  request<Enrollment>(`/api/enrollments/${enrollmentId}/progress`, token, {
    method: "PATCH",
    body: JSON.stringify({ progress }),
  });
export const cancelEnrollment = (token: string, enrollmentId: string) =>
  request<void>(`/api/enrollments/${enrollmentId}`, token, { method: "DELETE" });

// --- 퀴즈 ---
export interface Quiz {
  id: string;
  courseId: string;
  title: string;
}

export interface QuizQuestion {
  id: string;
  body: string;
  choices: string[];
  orderNo: number;
  // 주의: 정답(correctIndex)은 서버가 보내지 않는다.
}

export interface QuizDetail {
  id: string;
  title: string;
  questions: QuizQuestion[];
}

export interface SubmissionResult {
  submissionId: string;
  score: number;
  total: number;
  correctness: boolean[];
}

export interface SubmissionSummary {
  id: string;
  quizId: string;
  score: number;
  total: number;
  submittedAt: string;
}

export const listQuizzes = (token: string, courseId: string) =>
  request<Quiz[]>(`/api/courses/${courseId}/quizzes`, token);

export const createQuiz = (token: string, courseId: string, title: string) =>
  request<Quiz>(`/api/courses/${courseId}/quizzes`, token, {
    method: "POST",
    body: JSON.stringify({ title }),
  });

export const getQuiz = (token: string, quizId: string) =>
  request<QuizDetail>(`/api/quizzes/${quizId}`, token);

export const addQuestion = (
  token: string,
  quizId: string,
  body: { body: string; choices: string[]; correctIndex: number; orderNo: number },
) => request<QuizQuestion>(`/api/quizzes/${quizId}/questions`, token, { method: "POST", body: JSON.stringify(body) });

export const updateQuestion = (
  token: string,
  quizId: string,
  questionId: string,
  body: { body: string; choices: string[]; correctIndex: number; orderNo: number },
) => request<QuizQuestion>(`/api/quizzes/${quizId}/questions/${questionId}`, token, { method: "PUT", body: JSON.stringify(body) });

export const deleteQuestion = (token: string, quizId: string, questionId: string) =>
  request<void>(`/api/quizzes/${quizId}/questions/${questionId}`, token, { method: "DELETE" });

export const deleteQuiz = (token: string, quizId: string) =>
  request<void>(`/api/quizzes/${quizId}`, token, { method: "DELETE" });

export const submitQuiz = (token: string, quizId: string, answers: number[]) =>
  request<SubmissionResult>(`/api/quizzes/${quizId}/submissions`, token, {
    method: "POST",
    body: JSON.stringify({ answers }),
  });

export const myQuizSubmissions = (token: string) =>
  request<SubmissionSummary[]>("/api/quiz-submissions/me", token);

// --- 기능 플래그 (기관별 모듈 활성화) ---
export interface FeatureView {
  name: string;        // LESSONS | ENROLLMENTS | QUIZZES | CERTIFICATES ...
  displayName: string;
  enabled: boolean;    // 유효 활성 = 자격 있음 AND 기관이 켬
  entitled: boolean;   // 요금제(자격)에 포함 — false면 기관이 켤 수 없음(🔒)
  implemented: boolean;
}

export const listFeatures = (token: string) =>
  request<FeatureView[]>("/api/features", token);

export const toggleFeature = (token: string, name: string, enabled: boolean) =>
  request<FeatureView[]>(`/api/features/${name}`, token, {
    method: "PUT",
    body: JSON.stringify({ enabled }),
  });

// --- 플랫폼(SaaS 제공자) 슈퍼관리자: 요금제·자격 관리 ---
// 테넌트 세션과 별개인 슈퍼관리자 토큰을 쓴다.
export interface PlatformEntitlement {
  feature: string;
  displayName: string;
  entitled: boolean;
  source: "PLAN" | "ADDON" | null;  // 자격 있을 때만
  implemented: boolean;
}

export interface PlatformTenant {
  id: string;
  orgCode: string;
  name: string;
  plan: string;        // FREE | STANDARD | PRO
  status: "ACTIVE" | "PAST_DUE" | "SUSPENDED";
  features: PlatformEntitlement[];
}

export interface PlatformPlan {
  name: string;
  displayName: string;
  features: string[];
}

export const platformLogin = (email: string, password: string) =>
  request<{ token: string; email: string }>("/api/platform/login", null, {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });

export const platformListPlans = (token: string) =>
  request<PlatformPlan[]>("/api/platform/plans", token);

export const platformListTenants = (token: string) =>
  request<PlatformTenant[]>("/api/platform/tenants", token);

export const platformChangePlan = (token: string, tenantId: string, plan: string) =>
  request<PlatformTenant>(`/api/platform/tenants/${tenantId}/plan`, token, {
    method: "PUT",
    body: JSON.stringify({ plan }),
  });

export const platformGrantAddon = (token: string, tenantId: string, feature: string) =>
  request<PlatformTenant>(`/api/platform/tenants/${tenantId}/entitlements/${feature}`, token, {
    method: "POST",
  });

export const platformRevoke = (token: string, tenantId: string, feature: string) =>
  request<PlatformTenant>(`/api/platform/tenants/${tenantId}/entitlements/${feature}`, token, {
    method: "DELETE",
  });

// --- 플랫폼 가격/청구 ---
export interface PlanPriceView {
  name: string;
  displayName: string;
  monthlyPrice: number;
  currency: string;
}

export interface AddonPriceView {
  feature: string;
  displayName: string;
  pricingType: "FLAT" | "USAGE";
  monthlyPrice: number;
  unitPrice: number;
  includedUnits: number;
  unitLabel: string | null;
  currency: string;
}

export interface PricingView {
  plans: PlanPriceView[];
  addons: AddonPriceView[];
}

export interface InvoiceLineView {
  kind: "PLAN" | "ADDON" | "USAGE";
  label: string;
  amount: number;
  detail: string | null;
}

export interface StatementView {
  period: string;
  currency: string;
  total: number;
  lines: InvoiceLineView[];
}

export interface InvoiceView {
  id: string;
  tenantId: string;
  period: string;
  currency: string;
  total: number;
  status: "ISSUED" | "PAID";
  lines: InvoiceLineView[];
  paymentRef: string | null;
  issuedAt: string;
  paidAt: string | null;
}

export interface TenantBillingView {
  statement: StatementView;
  invoices: InvoiceView[];
}

export const platformGetPricing = (token: string) =>
  request<PricingView>("/api/platform/pricing", token);

export const platformGetBilling = (token: string, tenantId: string) =>
  request<TenantBillingView>(`/api/platform/tenants/${tenantId}/billing`, token);

export const platformIssueInvoice = (token: string, tenantId: string, period?: string) =>
  request<InvoiceView>(`/api/platform/tenants/${tenantId}/invoices`, token, {
    method: "POST",
    body: JSON.stringify({ period: period ?? null }),
  });

export const platformPayInvoice = (token: string, invoiceId: string) =>
  request<InvoiceView>(`/api/platform/invoices/${invoiceId}/pay`, token, { method: "POST" });

// 기관 상태(정지/해제) + 청구 마감
export const platformSuspendTenant = (token: string, tenantId: string) =>
  request<PlatformTenant>(`/api/platform/tenants/${tenantId}/suspend`, token, { method: "POST" });

export const platformReactivateTenant = (token: string, tenantId: string) =>
  request<PlatformTenant>(`/api/platform/tenants/${tenantId}/reactivate`, token, { method: "POST" });

export const platformClosePeriod = (token: string, period?: string) =>
  request<InvoiceView[]>("/api/platform/billing/close-period", token, {
    method: "POST",
    body: JSON.stringify({ period: period ?? null }),
  });

// 가격 편집
export const platformUpdatePlanPrice = (token: string, plan: string, monthlyPrice: number) =>
  request<PlanPriceView>(`/api/platform/pricing/plans/${plan}`, token, {
    method: "PUT",
    body: JSON.stringify({ monthlyPrice }),
  });

export const platformUpdateAddonPrice = (
  token: string,
  feature: string,
  body: { monthlyPrice: number; unitPrice: number; includedUnits: number },
) => request<AddonPriceView>(`/api/platform/pricing/addons/${feature}`, token, {
  method: "PUT",
  body: JSON.stringify(body),
});

// 감사 로그
export interface AuditView {
  id: string;
  actor: string;
  action: string;
  targetType: string | null;
  targetId: string | null;
  detail: string | null;
  createdAt: string;
}

export const platformGetAudit = (token: string) =>
  request<AuditView[]>("/api/platform/audit", token);

// 금액 표시(원). currency는 현재 KRW 고정. 값이 없으면 0으로 안전 처리.
export const formatMoney = (amount: number | null | undefined, currency = "KRW") => {
  const n = typeof amount === "number" && Number.isFinite(amount) ? amount : 0;
  return currency === "KRW" ? `${n.toLocaleString("ko-KR")}원` : `${n.toLocaleString()} ${currency}`;
};

// --- 학습자 프로필 / 추천 ---
export interface InterestCategory {
  code: string;
  name: string;
}

export interface SkillView {
  categoryCode: string;
  level: number; // 0 입문 ~ 3 고급
}

export interface LearnerProfile {
  onboarded: boolean;
  interests: string[];
  skills: SkillView[];
}

export interface Recommendation {
  courseId: string;
  title: string;
  categoryCode: string | null;
  level: number | null;
  score: number;
  reason: string;
}

export const listInterestCategories = (token: string) =>
  request<InterestCategory[]>("/api/interest-categories", token);

export const getProfile = (token: string) => request<LearnerProfile>("/api/me/profile", token);

export const saveProfile = (token: string, body: { interests: string[]; skills: SkillView[] }) =>
  request<LearnerProfile>("/api/me/profile", token, { method: "PUT", body: JSON.stringify(body) });

export const getRecommendations = (token: string) =>
  request<Recommendation[]>("/api/recommendations", token);

// --- AI 콘텐츠 분석(큐레이션) ---
export interface ContentInsight {
  tags: string[];
  difficulty: number | null;
  summary: string | null;
  estMinutes: number | null;
  generatedBy: string; // HEURISTIC | CLAUDE
  updatedAt: string | null;
}

export const getInsight = (token: string, courseId: string) =>
  request<ContentInsight | null>(`/api/courses/${courseId}/insight`, token);

export const analyzeInsight = (token: string, courseId: string) =>
  request<ContentInsight>(`/api/courses/${courseId}/insight`, token, { method: "POST" });

// --- 수료증 ---
export interface Certificate {
  id: string;
  courseId: string;
  courseTitle: string | null;
  certificateNo: string;
  issuedAt: string;
}

export const getMyCertificates = (token: string) =>
  request<Certificate[]>("/api/me/certificates", token);

export const getCourseCertificate = (token: string, courseId: string) =>
  request<Certificate | null>(`/api/courses/${courseId}/certificate`, token);

// --- 강사 대시보드 ---
export interface CourseStats {
  courseId: string;
  title: string;
  categoryCode: string | null;
  level: number | null;
  enrollmentCount: number;
  avgProgress: number;
  completedCount: number;
  quizCount: number;
  avgQuizScore: number | null;
  certificateCount: number;
}

export const getInstructorCourses = (token: string) =>
  request<CourseStats[]>("/api/instructor/courses", token);

export interface StudentProgress {
  studentId: string;
  progress: number;
  status: "ACTIVE" | "COMPLETED";
  completed: boolean;
  quizzesTaken: number;
  quizzesTotal: number;
  avgQuizScore: number | null;
  certified: boolean;
}

export const getCourseStudents = (token: string, courseId: string) =>
  request<StudentProgress[]>(`/api/instructor/courses/${courseId}/students`, token);

// --- 공지사항 ---
export interface Notice {
  id: string;
  scope: "ACADEMY" | "COURSE";
  courseId: string | null;
  title: string;
  body: string | null;
  author: string | null;
  pinned: boolean;
  createdAt: string;
  updatedAt: string | null;
}

export const listAcademyNotices = (token: string) =>
  request<Notice[]>("/api/notices", token);
export const createAcademyNotice = (token: string, body: { title: string; body?: string; pinned: boolean }) =>
  request<Notice>("/api/notices", token, { method: "POST", body: JSON.stringify(body) });
export const listCourseNotices = (token: string, courseId: string) =>
  request<Notice[]>(`/api/courses/${courseId}/notices`, token);
export const createCourseNotice = (token: string, courseId: string, body: { title: string; body?: string; pinned: boolean }) =>
  request<Notice>(`/api/courses/${courseId}/notices`, token, { method: "POST", body: JSON.stringify(body) });
export const updateNotice = (token: string, id: string, body: { title: string; body?: string; pinned: boolean }) =>
  request<Notice>(`/api/notices/${id}`, token, { method: "PUT", body: JSON.stringify(body) });
export const deleteNotice = (token: string, id: string) =>
  request<void>(`/api/notices/${id}`, token, { method: "DELETE" });

// --- 학부모: 자녀 학습현황 + (관리자) 연결 관리 ---
export interface GuardianLink {
  id: string; parentSubject: string; studentSubject: string; createdAt: string;
}
export interface ChildInfo { subject: string; displayName: string | null; }
export const myChildren = (token: string) => request<ChildInfo[]>("/api/me/children", token);
export const childEnrollments = (token: string, studentSubject: string) =>
  request<Enrollment[]>(`/api/me/children/${encodeURIComponent(studentSubject)}/enrollments`, token);
export const listGuardianLinks = (token: string) => request<GuardianLink[]>("/api/admin/guardians", token);
export const createGuardianLink = (token: string, parentSubject: string, studentSubject: string) =>
  request<GuardianLink>("/api/admin/guardians", token, { method: "POST", body: JSON.stringify({ parentSubject, studentSubject }) });
export const deleteGuardianLink = (token: string, id: string) =>
  request<void>(`/api/admin/guardians/${id}`, token, { method: "DELETE" });

// ===== Tier1: 반/기수 · 출석 · 자료실 · 강의노출 =====
export interface StudentGroup {
  id: string; name: string; term: string | null; startDate: string | null; endDate: string | null;
  memberCount: number; createdAt: string;
}
export interface GroupMember { id: string; studentSubject: string; createdAt: string; }
export const listGroups = (token: string) => request<StudentGroup[]>("/api/groups", token);
export const createGroup = (token: string, body: { name: string; term?: string; startDate?: string | null; endDate?: string | null }) =>
  request<StudentGroup>("/api/groups", token, { method: "POST", body: JSON.stringify(body) });
export const deleteGroup = (token: string, id: string) => request<void>(`/api/groups/${id}`, token, { method: "DELETE" });
export const listGroupMembers = (token: string, id: string) => request<GroupMember[]>(`/api/groups/${id}/members`, token);
export const addGroupMember = (token: string, id: string, studentSubject: string) =>
  request<GroupMember>(`/api/groups/${id}/members`, token, { method: "POST", body: JSON.stringify({ studentSubject }) });
export const removeGroupMember = (token: string, id: string, studentSubject: string) =>
  request<void>(`/api/groups/${id}/members/${encodeURIComponent(studentSubject)}`, token, { method: "DELETE" });

export interface AttendanceRecord {
  id: string; groupId: string; studentSubject: string; attDate: string; status: string; note: string | null;
}
export const markAttendance = (token: string, groupId: string, date: string, entries: { studentSubject: string; status: string; note?: string }[]) =>
  request<AttendanceRecord[]>(`/api/groups/${groupId}/attendance`, token, { method: "POST", body: JSON.stringify({ date, entries }) });
export const groupAttendance = (token: string, groupId: string, date: string) =>
  request<AttendanceRecord[]>(`/api/groups/${groupId}/attendance?date=${date}`, token);
export const myAttendance = (token: string) => request<AttendanceRecord[]>("/api/me/attendance", token);

export interface Material { id: string; courseId: string; title: string; fileUrl: string; uploadedBy: string | null; createdAt: string; }
export const listMaterials = (token: string, courseId: string) => request<Material[]>(`/api/courses/${courseId}/materials`, token);
export const addMaterial = (token: string, courseId: string, body: { title: string; fileUrl: string }) =>
  request<Material>(`/api/courses/${courseId}/materials`, token, { method: "POST", body: JSON.stringify(body) });
export const deleteMaterial = (token: string, id: string) => request<void>(`/api/materials/${id}`, token, { method: "DELETE" });

export const setCoursePublished = (token: string, id: string, published: boolean) =>
  request<Course>(`/api/courses/${id}/publish?published=${published}`, token, { method: "PUT" });
export const setCourseTuition = (token: string, id: string, fee: number) =>
  request<Course>(`/api/courses/${id}/tuition?fee=${fee}`, token, { method: "PUT" });

// ===== Tier2: 상담 · 알림 =====
export interface CounselingRecordItem { id: string; studentSubject: string; counselor: string; content: string; createdAt: string; }
export interface Appointment { id: string; studentSubject: string; requestedBy: string; preferredAt: string | null; status: string; memo: string | null; createdAt: string; }
export const addCounselingRecord = (token: string, studentSubject: string, content: string) =>
  request<CounselingRecordItem>("/api/counseling/records", token, { method: "POST", body: JSON.stringify({ studentSubject, content }) });
export const counselingRecords = (token: string, student: string) =>
  request<CounselingRecordItem[]>(`/api/counseling/records?student=${encodeURIComponent(student)}`, token);
export const myCounseling = (token: string) => request<CounselingRecordItem[]>("/api/me/counseling", token);
export const requestAppointment = (token: string, body: { studentSubject?: string; preferredAt?: string | null; memo?: string }) =>
  request<Appointment>("/api/counseling/appointments", token, { method: "POST", body: JSON.stringify(body) });
export const allAppointments = (token: string) => request<Appointment[]>("/api/counseling/appointments", token);
export const myAppointments = (token: string) => request<Appointment[]>("/api/me/appointments", token);
export const setAppointmentStatus = (token: string, id: string, status: string) =>
  request<Appointment>(`/api/counseling/appointments/${id}/status?status=${status}`, token, { method: "PUT" });

export interface AppNotification { id: string; title: string; body: string | null; category: string | null; read: boolean; createdAt: string; }
export const myNotifications = (token: string) => request<AppNotification[]>("/api/me/notifications", token);
export const unreadCount = (token: string) => request<{ count: number }>("/api/me/notifications/unread-count", token);
export const readNotification = (token: string, id: string) => request<void>(`/api/me/notifications/${id}/read`, token, { method: "POST" });
export const readAllNotifications = (token: string) => request<void>("/api/me/notifications/read-all", token, { method: "POST" });
export const sendNotification = (token: string, body: { recipient: string; title: string; body?: string; channel?: string }) =>
  request<{ status: string }>("/api/admin/notifications/send", token, { method: "POST", body: JSON.stringify(body) });
export interface DeliveryLogItem { id: string; channel: string; recipient: string; title: string | null; status: string; createdAt: string; }
export const notificationLogs = (token: string) => request<DeliveryLogItem[]>("/api/admin/notifications/logs", token);

// ===== Tier3: 수강료 결제 · 콘텐츠 마켓 =====
export interface Payment { id: string; studentSubject: string; courseId: string; amount: number; status: string; method: string | null; paymentRef: string | null; createdAt: string; refundedAt: string | null; }
export const payTuition = (token: string, courseId: string) => request<Payment>(`/api/courses/${courseId}/pay`, token, { method: "POST" });
export const myPayments = (token: string) => request<Payment[]>("/api/me/payments", token);
export const allPayments = (token: string) => request<Payment[]>("/api/admin/payments", token);
export const refundPayment = (token: string, id: string) => request<Payment>(`/api/admin/payments/${id}/refund`, token, { method: "POST" });

export interface MarketContentItem { id: string; title: string; description: string | null; category: string | null; price: number; provider: string | null; published: boolean; createdAt: string; }
export interface ContentPurchaseItem { id: string; contentId: string; purchasedBy: string | null; amount: number; createdAt: string; }
export interface SettlementItem { contentId: string; title: string; provider: string | null; purchaseCount: number; revenue: number; }
// 학원(ADMIN)
export const browseMarket = (token: string) => request<MarketContentItem[]>("/api/market/content", token);
export const purchaseContent = (token: string, id: string) => request<ContentPurchaseItem>(`/api/market/content/${id}/purchase`, token, { method: "POST" });
export const myMarketPurchases = (token: string) => request<ContentPurchaseItem[]>("/api/market/purchases", token);
// 플랫폼(PLATFORM_ADMIN)
export const platformMarketList = (token: string) => request<MarketContentItem[]>("/api/platform/market/content", token);
export const platformMarketCreate = (token: string, body: { title: string; description?: string; category?: string; price: number; provider?: string }) =>
  request<MarketContentItem>("/api/platform/market/content", token, { method: "POST", body: JSON.stringify(body) });
export const platformMarketDelete = (token: string, id: string) => request<void>(`/api/platform/market/content/${id}`, token, { method: "DELETE" });
export const platformSettlements = (token: string) => request<SettlementItem[]>("/api/platform/market/settlements", token);

// ===== Tier4: 학원 환경설정 =====
export interface TenantSettingsView { tenantId: string | null; displayName: string | null; logoUrl: string | null; primaryColor: string | null; contact: string | null; terms: string | null; }
export const getSettings = (token: string) => request<TenantSettingsView>("/api/settings", token);
export const updateSettings = (token: string, body: Omit<TenantSettingsView, "tenantId">) =>
  request<TenantSettingsView>("/api/settings", token, { method: "PUT", body: JSON.stringify(body) });

// ===== 입시/보습학원 특화: 시험 · 성적 · 학부모 리포트 =====
export interface Exam { id: string; title: string; subject: string | null; examDate: string; maxScore: number; groupId: string | null; }
export interface ExamScore { id: string; examId: string; studentSubject: string; score: number; comment: string | null; }
export interface StudentScore {
  examId: string; title: string; subject: string | null; examDate: string;
  score: number; maxScore: number; percent: number; comment: string | null;
}
export interface ReportAttendance { present: number; absent: number; late: number; excused: number; total: number; attendanceRate: number; }
export interface AttendanceEntry { date: string; status: string; note: string | null; }
export interface ReportAssignment { submitted: number; graded: number; avgScore: number | null; }
export interface ReportCourse { enrolled: number; completed: number; avgProgress: number; }
export interface StudentReport {
  studentSubject: string; studentName: string | null;
  scores: StudentScore[]; scoreAvgPercent: number | null; latestPercent: number | null;
  attendance: ReportAttendance; recentAttendance: AttendanceEntry[];
  assignments: ReportAssignment; courses: ReportCourse; generatedAt: string;
}
export interface ScoreEntryInput { studentSubject: string; score: number; comment?: string }

// 성적 기반 반편성
export interface PlacementBand { minPercent: number; groupId: string; }
export interface PlacementRecommendation { studentSubject: string; studentName: string | null; avgPercent: number; examCount: number; groupId: string; groupName: string | null; }
export interface PlacementApplyResult { assigned: number; studentsPlaced: number; recommendations: PlacementRecommendation[]; }
export const recommendPlacement = (token: string, bands: PlacementBand[]) =>
  request<PlacementRecommendation[]>("/api/placement/recommend", token, { method: "POST", body: JSON.stringify({ bands }) });
export const applyPlacement = (token: string, bands: PlacementBand[]) =>
  request<PlacementApplyResult>("/api/placement/apply", token, { method: "POST", body: JSON.stringify({ bands }) });

// 시험/성적 관리 (INSTRUCTOR/ADMIN)
export const listExams = (token: string) => request<Exam[]>("/api/exams", token);
export const createExam = (token: string, body: { title: string; subject?: string; examDate: string; maxScore?: number; groupId?: string | null }) =>
  request<Exam>("/api/exams", token, { method: "POST", body: JSON.stringify(body) });
export const updateExam = (token: string, id: string, body: { title: string; subject?: string; examDate: string; maxScore?: number; groupId?: string | null }) =>
  request<Exam>(`/api/exams/${id}`, token, { method: "PUT", body: JSON.stringify(body) });
export const deleteExam = (token: string, id: string) => request<void>(`/api/exams/${id}`, token, { method: "DELETE" });
export const examScores = (token: string, id: string) => request<ExamScore[]>(`/api/exams/${id}/scores`, token);
export const recordScores = (token: string, id: string, entries: ScoreEntryInput[]) =>
  request<ExamScore[]>(`/api/exams/${id}/scores`, token, { method: "POST", body: JSON.stringify({ entries }) });
// 내 성적 (학생)
export const myScores = (token: string) => request<StudentScore[]>("/api/me/scores", token);
// 학부모 리포트 (강사/관리자 조회·발송)
export const studentReport = (token: string, student: string) =>
  request<StudentReport>(`/api/students/${encodeURIComponent(student)}/report`, token);
export const sendReport = (token: string, student: string) =>
  request<{ sent: number; parents: string[]; emailStatus: string }>(`/api/students/${encodeURIComponent(student)}/report/send`, token, { method: "POST" });
// 자녀 성적·리포트 (학부모)
export const childScores = (token: string, student: string) =>
  request<StudentScore[]>(`/api/me/children/${encodeURIComponent(student)}/scores`, token);
export const childReport = (token: string, student: string) =>
  request<StudentReport>(`/api/me/children/${encodeURIComponent(student)}/report`, token);

// 난이도 라벨 헬퍼 (UI 공용)
export const LEVEL_LABELS = ["입문", "초급", "중급", "고급"];
export const levelLabel = (level: number | null | undefined) =>
  level == null ? "" : LEVEL_LABELS[level] ?? String(level);
