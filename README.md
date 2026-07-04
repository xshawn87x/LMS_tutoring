# LMS SaaS — Step 1: RLS 멀티테넌시 수직 슬라이스

모듈 단위 멀티테넌트 LMS SaaS의 첫 수직 슬라이스. **두 테넌트가 각자 과정(Course)을
가지고 있고, PostgreSQL RLS(Row Level Security)가 행 단위로 격리**해주는 것을 검증한다.

> 핵심: 애플리케이션 코드에는 `WHERE tenant_id = ?`가 **한 줄도 없다.** 격리는 전적으로
> DB(RLS)가 강제한다. 코드가 실수로 필터를 빠뜨려도 데이터가 새지 않는 것이 RLS의 가치.

> **프론트엔드**: Next.js 웹앱이 [frontend/](frontend/) 에 있다. 실행·화면 설명은 [frontend/README.md](frontend/README.md) 참고.

## 기술 스택

**백엔드**: Java 17 · Spring Boot 3.3 · Spring Web · Spring Security(OAuth2 Resource Server, JWT, RBAC) ·
Spring Data JPA / Hibernate · Flyway · PostgreSQL 16 (RLS) · Docker Compose · Lombok · Gradle
**프론트엔드**: Next.js 14 (App Router) · React 18 · TypeScript

## 동작 원리

```
요청 → JWT 검증(Resource Server) → tenant_id 클레임 추출 → TenantContext(ThreadLocal)
     → 커넥션 획득 시 SET app.current_tenant = '<uuid>' → RLS 정책이 행 자동 필터링
```

- **DB 롤 분리**: `lms_owner`(테이블 소유자, Flyway 마이그레이션 담당) vs `lms_app`(앱 런타임,
  RLS 적용 대상). RLS는 비소유자 롤에만 적용되므로 앱은 반드시 `lms_app`으로 접속한다.
  `FORCE ROW LEVEL SECURITY`로 소유자 우회까지 차단한다.
- **세션 변수 주입**: [TenantAwareDataSource](src/main/java/com/lms/tenant/TenantAwareDataSource.java)가
  커넥션을 내줄 때마다 `set_config('app.current_tenant', ...)`를 실행한다. 미인증 요청은
  sentinel(0 UUID)로 설정해 **fail-closed**(아무 행도 안 보임).
- **RLS 정책**: [V2__enable_rls.sql](src/main/resources/db/migration/V2__enable_rls.sql)

## 사전 준비 (1회)

이 환경에는 Docker와 Git이 아직 없다. 아래를 설치한다.

1. **Docker Desktop** — https://www.docker.com/products/docker-desktop/  → 설치 후 `docker --version`
2. **Git** (선택) — https://git-scm.com/download/win  → 설치 후 `git --version`
3. Java 17 — 이미 설치됨 ✅
4. Gradle — 설치 불필요. 포함된 Wrapper(`./gradlew`)가 자동으로 받는다.

## 이 PC 전용: 빌드 전 환경변수 (Avast + Docker Desktop)

이 머신은 **Avast가 HTTPS를 가로채** Java TLS를 막고, **Docker Desktop이 비표준 named pipe**를 쓴다.
그래서 Gradle 명령 전에 아래 두 환경변수를 한 번 설정해 둔다 (PowerShell 세션마다):

```powershell
$env:GRADLE_OPTS = "-Djavax.net.ssl.trustStore=.certs/cacerts -Djavax.net.ssl.trustStorePassword=changeit"
$env:DOCKER_HOST = "npipe:////./pipe/dockerDesktopLinuxEngine"   # Testcontainers가 Docker를 찾게 함
```
> 일반 PC(Avast 없음 + 기본 Docker 소켓)에서는 둘 다 불필요. 자세한 배경은 `gradle.properties` 주석 참고.

## 실행

```powershell
# 1) PostgreSQL 기동
docker compose up -d

# 2) 애플리케이션 실행 (Flyway가 스키마+RLS+시드를 자동 적용)
.\gradlew.bat bootRun
```

## 검증

### 자동 (통합 테스트 — Testcontainers, Docker 필요)

```powershell
.\gradlew.bat test
```

[TenantIsolationTest](src/test/java/com/lms/tenant/TenantIsolationTest.java)가
Testcontainers(2.0.5)로 깨끗한 PostgreSQL을 띄워 다음을 단언한다 (4 tests, 전부 통과 확인됨):
- 테넌트 A 컨텍스트 → A의 과정 2건만
- 테넌트 B 컨텍스트 → B의 과정 1건만
- A가 만든 과정은 B에게 보이지 않음 (RLS가 행을 숨김)
- 테넌트 컨텍스트 없음 → 0건 (fail-closed)

### 수동 (end-to-end)

`bootRun` 상태에서 (dev 프로파일이 기본 → `/dev/token` 활성화):

```powershell
# 두 테넌트의 토큰 발급
$tokenA = (Invoke-RestMethod "http://localhost:8080/dev/token?tenantId=11111111-1111-1111-1111-111111111111").token
$tokenB = (Invoke-RestMethod "http://localhost:8080/dev/token?tenantId=22222222-2222-2222-2222-222222222222").token

# 격리 확인 — A는 2건, B는 1건
Invoke-RestMethod "http://localhost:8080/api/courses" -Headers @{ Authorization = "Bearer $tokenA" }
Invoke-RestMethod "http://localhost:8080/api/courses" -Headers @{ Authorization = "Bearer $tokenB" }

# 생성은 INSTRUCTOR/ADMIN 권한 필요 → 역할 있는 토큰으로 발급
$instrA = (Invoke-RestMethod "http://localhost:8080/dev/token?tenantId=11111111-1111-1111-1111-111111111111&roles=INSTRUCTOR").token
Invoke-RestMethod "http://localhost:8080/api/courses" -Method Post -Headers @{ Authorization = "Bearer $instrA" } `
    -ContentType "application/json" -Body '{"title":"새 과정","description":"A가 생성"}'
```

**성공 기준**: 어떤 테넌트도 다른 테넌트의 행을 읽거나 쓸 수 없다 → RLS 척추 검증 완료.

## API 엔드포인트

> 모든 `/api/**`는 JWT 필요. 화면으로 보려면 [Swagger UI](http://localhost:8080/swagger-ui/index.html)에서
> `/dev/token`으로 토큰을 받아 **Authorize**에 넣고 호출.

| 메서드 | 경로 | 권한 | 설명 |
|--------|------|------|------|
| GET | `/api/courses` · `/api/courses/{id}` | 인증된 누구나 | 과정 목록/단건 (타 테넌트면 404) |
| POST | `/api/courses` | INSTRUCTOR, ADMIN | 과정 생성 |
| PUT | `/api/courses/{id}` | INSTRUCTOR, ADMIN | 과정 수정 |
| DELETE | `/api/courses/{id}` | INSTRUCTOR, ADMIN | 과정 삭제 (레슨·수강·퀴즈·수료증 CASCADE) |
| GET | `/api/courses/{courseId}/lessons` | 인증된 누구나 | 과정의 레슨 목록 |
| POST | `/api/courses/{courseId}/lessons` | INSTRUCTOR, ADMIN | 레슨 추가 |
| PUT | `/api/courses/{courseId}/lessons/{lessonId}` | INSTRUCTOR, ADMIN | 레슨 수정 (제목·내용·영상·순서) |
| DELETE | `/api/courses/{courseId}/lessons/{lessonId}` | INSTRUCTOR, ADMIN | 레슨 삭제 |
| POST | `/api/uploads/video` | INSTRUCTOR, ADMIN | 동영상 업로드(multipart) → `/media/{파일}` URL 반환 |
| GET | `/media/{파일}` | 공개 | 업로드 영상 제공 (HTTP Range 지원 → seek/이어듣기) |
| POST | `/api/courses/{courseId}/enrollments` | STUDENT, ADMIN | 수강신청 (학생 = JWT subject) |
| GET | `/api/enrollments/me` | 인증된 누구나 | 내 수강 목록 |
| PATCH | `/api/enrollments/{id}/progress` | STUDENT, ADMIN | 진도 수동 갱신 (100%면 자동 완료) |
| DELETE | `/api/enrollments/{id}` | STUDENT, ADMIN | 수강 취소 (내 수강 + 진도·수료증 초기화) |
| PUT | `/api/lessons/{lessonId}/progress` | STUDENT, ADMIN | 학습창 진도 저장(이어듣기 위치 + 완료) → 수강 진도 자동 재계산 |
| GET | `/api/courses/{courseId}/lesson-progress` | 인증된 누구나 | 내 레슨별 진도(이어듣기·완료 표시용) |
| POST | `/api/auth/register` | 공개 | 회원가입 (org_code+이메일+비번, bcrypt 저장) → JWT 발급 |
| POST | `/api/auth/login` | 공개 | 로그인 (org_code+이메일+비번) → JWT 발급 |
| GET/POST | `/api/courses/{courseId}/quizzes` | 조회=누구나 / 생성=INSTRUCTOR,ADMIN | 퀴즈 목록 / 생성 |
| GET | `/api/quizzes/{quizId}` | 인증된 누구나 | 응시용 퀴즈+문항 (정답 숨김) |
| POST | `/api/quizzes/{quizId}/questions` | INSTRUCTOR, ADMIN | 객관식 문항 추가 |
| POST | `/api/quizzes/{quizId}/submissions` | STUDENT, ADMIN | 답안 제출 → 즉시 채점 |
| GET | `/api/quiz-submissions/me` | 인증된 누구나 | 내 제출/점수 이력 |
| GET | `/api/features` | 인증된 누구나 | 이 기관의 기능 플래그 상태 (UI 게이팅용) |
| PUT | `/api/features/{feature}` | ADMIN | 기능 켜기/끄기 (기관별) |
| GET | `/api/interest-categories` | 인증된 누구나 | 관심분야 카탈로그 |
| GET/PUT | `/api/me/profile` | STUDENT, ADMIN | 내 관심분야·역량(자가 진단) 조회/저장 |
| GET | `/api/recommendations` | STUDENT, ADMIN | 맞춤 추천(관심분야·난이도·콘텐츠 태그 + 행동신호[인기도·수강이력], 이유 포함) |
| GET | `/api/courses/{id}/insight` | 인증된 누구나 | AI 콘텐츠 분석 결과(태그·난이도·요약·예상시간) |
| POST | `/api/courses/{id}/insight` | INSTRUCTOR, ADMIN | 콘텐츠 분석 실행 (AI_CURATION 필요) |
| GET | `/api/me/certificates` | STUDENT, ADMIN | 내 수료증 목록 (CERTIFICATES 필요) |
| GET | `/api/courses/{id}/certificate` | STUDENT, ADMIN | 이 과정 내 수료증 (없으면 null) |
| PUT | `/api/quizzes/{quizId}/questions/{questionId}` | INSTRUCTOR, ADMIN | 문항 수정 |
| DELETE | `/api/quizzes/{quizId}/questions/{questionId}` | INSTRUCTOR, ADMIN | 문항 삭제 |
| DELETE | `/api/quizzes/{quizId}` | INSTRUCTOR, ADMIN | 퀴즈 삭제 (문항·제출 CASCADE) |
| POST | `/api/me/password` | 인증된 누구나(실계정) | 비밀번호 변경 (현재 비번 확인) |
| PUT | `/api/me/account` | 인증된 누구나(실계정) | 표시 이름 수정 |
| GET | `/api/instructor/courses` | INSTRUCTOR, ADMIN | 강사 대시보드 — 과정별 수강·진도·완료·퀴즈·수료증 집계 |
| GET | `/api/instructor/courses/{courseId}/students` | INSTRUCTOR, ADMIN | 수강생별 진도·완료·퀴즈점수·수료 드릴다운 |
| GET | `/actuator/health` · `/actuator/metrics` | 공개 | 운영 관측성 (Spring Boot Actuator) |
| GET | `/dev/token?tenantId=&subject=&roles=` | 공개(dev) | 테스트 토큰 발급 |

### 기능 플래그 (기관별 모듈 선택 활성화)

모듈러 모놀리식에서 기관(테넌트)마다 사용할 모듈을 켜고 끈다. 플래그는 `tenant_feature` 테이블에
테넌트별로 저장(RLS 격리)되고, 행이 없으면 `Feature` enum의 기본값을 따른다.

- 토글 가능: `LESSONS` · `ENROLLMENTS` · `QUIZZES` · `DIAGNOSIS` · `RECOMMENDATIONS` (구현됨, 기본 ON),
  `AI_CURATION` · `CERTIFICATES` (구현됨, 기본 OFF — AI 분석 / 수료증)
- 끄면 해당 모듈의 서비스가 `FeatureDisabledException`(403)을 던져 **API가 그 기관에 한해 비활성화**되고,
  프론트는 `/api/features`를 읽어 **내비·과정 상세의 섹션을 숨긴다**.
- 관리 화면: 프론트 **`/admin/features`** (ADMIN 전용). 토글 즉시 그 기관의 전체 UI에 반영.
- 과정(Course)은 코어라 플래그가 없다.

레슨·수강도 course와 똑같이 RLS로 격리된다. 타 테넌트 과정에 레슨/수강을 붙이려는 시도는 404.

### AI 콘텐츠 분석·큐레이션 (학습자 보강 Phase 2)

과정/레슨 내용을 분석해 **태그·난이도·요약·예상시간**(`content_insight`, RLS)을 만들고, 그 태그를
추천 점수에 반영한다(관심분야 ↔ 콘텐츠 태그 일치 가산). `AI_CURATION` 플래그가 켜진 기관에서만 동작.

- 분석기는 인터페이스(`ContentAnalyzer`)로 추상화:
  - **`HeuristicContentAnalyzer`** (기본·무료·검증됨): 키워드 사전 + 선언 난이도/레슨 수로 산출.
  - **`ClaudeContentAnalyzer`** (`ANTHROPIC_API_KEY` 설정 시 자동 우선): 공식 `anthropic-java` SDK로
    `claude-opus-4-8` 호출 → 태그/요약 추출. **키가 없어 로컬 검증은 미실시**(휴리스틱이 활성 기본값).
- 사용자 흐름: (강사) 과정 상세에서 "분석 실행" → 인사이트 표시 / (학습자) 추천 이유에 "콘텐츠 태그 일치" 노출.

### 수료/수료증 (B1)

**수료 조건 = 진도 100% + 과정의 모든 퀴즈 60% 이상 통과** (퀴즈 없으면 진도 100%만). 충족 시
`course_completion`(수료증, RLS) 1건 발급 — 진도가 100%에 도달하는 순간(`EnrollmentService.updateProgress`)
자동 판정. `CERTIFICATES` 플래그가 켜진 기관에서만 동작. 프론트: 과정 상세에 수료 상태/수료증, **`/certificates`** 내 수료증 목록.

**수료증 출력**: 목록의 "수료증 보기·출력" → **`/certificates/{id}`** 인쇄용 수료증 양식(이중 테두리·수상자명·과정명·수료번호·발급일·발급기관). "🖨 인쇄 / PDF 저장" 버튼이 `window.print()`를 호출하고, `@media print` CSS가 내비/버튼/토스트를 숨겨 증서만 출력(브라우저 PDF 저장 가능). 수상자명은 세션 displayName.

### 콘텐츠 관리 (강사) + 동영상 업로드

- **과정/레슨 CRUD**: 과정 상세에서 강사가 과정 수정·삭제, 레슨 수정·삭제·**순서 변경(▲▼)**. 과정 삭제는 하위(레슨·수강·퀴즈·진도·수료증)를 FK CASCADE로 정리. 모든 쓰기는 `@PreAuthorize(INSTRUCTOR/ADMIN)` + RLS(타 테넌트 404).
- **동영상 업로드(무료, 로컬 스토리지)**: 레슨 추가/수정 폼에서 파일 업로드 → `POST /api/uploads/video`가 로컬(`app.upload.dir`, 기본 `uploads/`)에 UUID 파일명으로 저장하고 `/media/{파일}` URL 반환 → 레슨 videoUrl에 저장. 제공은 Spring `ResourceHttpRequestHandler`라 **HTTP Range(206) 지원** → 학습창에서 탐색·이어듣기 가능. 외부 URL도 그대로 사용 가능(없으면 샘플 영상). 프론트는 상대 `/media/..`를 백엔드 오리진으로 해석(`resolveMediaUrl`).

### 과정 탐색 (검색·필터·정렬)

과정 목록(**`/courses`**)에서 제목·설명 **검색**, 분야 **필터**, 제목/난이도 **정렬**(클라이언트, RLS로 이미 테넌트 범위).

### 강사 대시보드 — 수강생 진도 드릴다운

강사 대시보드(**`/instructor`**)의 과정별 집계에서 "수강생 진도 상세" → **`/instructor/{courseId}`** 표로
수강생별 진도·상태·응시 퀴즈 수·평균 최고점(%)·수료증 발급 여부를 확인. RLS로 테넌트 범위 격리.

### 퀴즈 관리 (강사) + 응시 이력

- 강사: 퀴즈 상세에서 문항 **수정·삭제**, 퀴즈 **삭제**(문항·제출 CASCADE). 학습창(**`/learn`**)에 과정 퀴즈 목록·응시 링크.
- 학생: 내 **최고 점수·응시 횟수** 표시, 재응시 가능.

### 계정 관리 (실 로그인 계정)

**`/account`**에서 표시 이름 수정, 비밀번호 변경(현재 비번 확인 + bcrypt 재해시). dev 토큰 세션은 실제 계정이 아니라 비활성(404 안내).

### 학습창 (동영상 + 자동 진도 + 이어듣기)

수강생이 영상을 보며 학습하는 화면. 프론트 **`/learn/{courseId}`**.

- **샘플 영상 구동**: 레슨에 `video_url`이 있으면 그 영상을, 없으면 공개 샘플 영상(Big Buck Bunny)으로 재생.
- **자동 진도 체크**: 재생 중 10초마다, 일시정지/레슨 전환/영상 종료 시 `PUT /api/lessons/{id}/progress`로 위치·완료를 저장.
  영상이 끝나면 그 레슨을 **완료** 처리하고 다음 레슨으로 자동 이동.
- **수강 진도 자동 재계산**: 서버가 `(완료 레슨 수 / 전체 레슨 수) × 100`으로 `enrollment.progress`를 다시 계산하고,
  100% 도달 시 수료 조건을 판정한다(퀴즈 통과 포함). 진도는 손으로 안 만지고 학습 행동에서 파생된다.
- **이어듣기**: 진입 시 `GET /api/courses/{id}/lesson-progress`로 마지막 위치를 받아 첫 미완료 레슨을 선택하고,
  영상 메타데이터 로드 시 저장된 초로 점프한다. 한 번 완료된 레슨은 위치만 갱신해도 미완료로 내려가지 않는다.
- 진도는 `lesson_progress`(RLS)에 학습자·테넌트별로 격리 저장.

### 실제 로그인/회원가입 (dev 토큰 대체)

`/dev/token`(비밀번호 없는 테스트 토큰)과 별개로, **기관(org_code) + 이메일 + 비밀번호** 기반 실제 인증을 제공한다.

- `tenant` 레지스트리(전역, RLS 없음)에서 `org_code`로 테넌트를 찾고 → TenantContext 세팅 → `app_user`(RLS) 작업.
  로그인 전 단계라 테넌트를 알 수 없으므로 org_code로 식별하는 구조.
- 비밀번호는 **bcrypt**(`BCryptPasswordEncoder`)로만 저장. 검증 성공 시 dev 토큰과 동일한 HS256 JWT를
  공유 `TokenService`로 발급(subject=이메일, tenant_id, roles).
- 같은 이메일이라도 **기관별로 독립 계정**(RLS + `unique(tenant_id,email)`). 자가 가입 역할은 STUDENT/INSTRUCTOR만(ADMIN 제외).
- 시드 기관: `acme`(테넌트 A) · `globex`(테넌트 B). 프론트 **`/login`**에 로그인/회원가입 탭 + 접이식 dev 빠른 로그인.

### 권한 (RBAC)

JWT의 `roles` 클레임(`["INSTRUCTOR"]` 등)을 `ROLE_*` 권한으로 변환하고, 쓰기 작업에 `@PreAuthorize`로 제약한다.

| 역할 | 권한 |
|------|------|
| `ADMIN` | 전부 |
| `INSTRUCTOR` | 과정·레슨 생성/관리 |
| `STUDENT` | 수강신청·진도 관리 (조회는 모두 가능) |

dev 토큰에 역할 지정: `GET /dev/token?tenantId=...&subject=alice&roles=INSTRUCTOR`
(여러 역할은 `roles=INSTRUCTOR,ADMIN`. 생략 시 기본 `STUDENT`)

## 시드 데이터

| 테넌트 | org_code | UUID | 과정(각 과정에 레슨·샘플영상 시드, V12) |
|--------|----------|------|------|
| A | acme | `1111…1111` | Spring 입문, JPA 기초, AI 에이전트를 활용한 LMS 구축, React로 시작하는 프론트엔드, 실전 SQL과 데이터 모델링, Docker로 배우는 컨테이너 기초 |
| B | globex | `2222…2222` | Python 입문, Pandas 데이터 분석 입문 |

## 프로젝트 구조

```
src/main/java/com/lms/
├── config/        SecurityConfig(JWT+RBAC+bcrypt), DataSourceConfig, OpenApiConfig(Swagger)
├── security/      Roles (역할·클레임 상수), TokenService (HS256 JWT 발급 단일 출처)
├── auth/          Tenant(레지스트리), AppUser, AuthService(회원가입/로그인), AccountService(비번·이름), 컨트롤러, dto/
├── tenant/        TenantContext, TenantFilter, TenantAwareDataSource
├── course/        Course(+update), CourseRepository(필터 없음!), Service(CRUD), Controller, dto/
├── lesson/        Lesson(+videoUrl/update), LessonProgress(이어듣기·완료), Repository, Service(CRUD), Controller, dto/
├── upload/        StorageConfig(/media 정적제공·Range), UploadController(동영상 업로드)
├── enrollment/    Enrollment, EnrollmentStatus, Repository, Service, Controller, dto/
├── quiz/          Quiz, Question(보기 JSON), QuizSubmission, 채점 Service, Controller, dto/
├── feature/       Feature(enum), TenantFeature, FeatureService(게이팅), Controller
├── learner/       InterestCategory, LearnerProfile/Interest/Skill, ProfileService, RecommendationService(규칙+태그)
├── curation/      ContentInsight, ContentAnalyzer(Heuristic/Claude), ContentInsightService (AI 콘텐츠 분석)
├── certificate/   CourseCompletion, CertificateService (수료 판정·수료증 발급)
├── dashboard/     DashboardService (강사용 과정별 집계)
├── error/         ApiException + 도메인 예외 + ApiError + GlobalExceptionHandler
└── dev/           DevTokenController (dev 프로파일 전용 토큰 발급기)
src/main/resources/db/migration/   V1~V9 + V10(레슨 영상·진도) + V11(기관·사용자) + V12(실제 과정/레슨 시드)
```

## 운영 (Ops)

- **관측성**: Spring Boot Actuator — `/actuator/health` · `/actuator/metrics` · `/actuator/info` (운영에선 접근 통제 강화 필요)
- **CI**: [.github/workflows/ci.yml](.github/workflows/ci.yml) — Git 저장소 초기화 후 GitHub에 푸시하면 백엔드 테스트(Testcontainers)+프론트 빌드 자동 실행. (로컬 Avast 트러스트스토어/`gradle.properties`는 gitignore되어 CI엔 불필요)
- **UX**: 전역 토스트(`ToastProvider`)로 생성/수정/저장 액션 성공 알림 통일

## 다음 단계 (이번 범위 밖 / 외부 비용·키 필요)

- **임베딩 기반 추천**(pgvector + Voyage/OpenAI 임베딩) — 외부 임베딩 API 비용 발생, 현재는 규칙+행동신호로 대체
- **결제**(포트원/토스), **S3/Mux 스토리지**, **Sentry**, **운영 RSA/IdP(JWKS) JWT**, **Redis 캐시** — 외부 서비스/키 필요
- **Spring Modulith 모듈 경계 강제** — 현재 모듈 간 직접 의존이 있어 도입 시 모듈 API 정의 리팩터 필요
- **Git 초기화 + 첫 커밋** (사용자 요청으로 보류 중)
