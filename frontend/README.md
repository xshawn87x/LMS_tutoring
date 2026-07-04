# LMS Frontend (Next.js)

멀티테넌트 LMS의 웹 프론트엔드. 백엔드(Spring Boot) REST API를 호출하며,
**RLS 테넌트 격리**와 **RBAC 권한**이 화면에서 그대로 드러난다.

## 스택
Next.js 14 (App Router) · React 18 · TypeScript. 별도 UI 라이브러리 없이 `globals.css`로 스타일.

## 사전 준비
1. **Node.js LTS** (이 PC엔 winget으로 설치됨: v24) — `node --version`
2. 백엔드 실행 (`../` 에서 `docker compose up -d` + `.\gradlew.bat bootRun`)
3. **이 PC 전용**: Avast가 HTTPS를 가로채 npm 레지스트리 TLS를 막으므로, Avast CA를 PEM으로
   지정해야 한다 (cf. 루트의 `.certs/avast-root.pem`):
   ```powershell
   $env:NODE_EXTRA_CA_CERTS = "..\.certs\avast-root.pem"
   ```
   (Avast 없는 PC에서는 불필요)

## 실행
```powershell
cd frontend
$env:NODE_EXTRA_CA_CERTS = "..\.certs\avast-root.pem"   # 이 PC 전용
npm install
npm run dev          # http://localhost:3000
```
> 백엔드 주소는 `.env.local` 의 `NEXT_PUBLIC_API_BASE` (기본 http://localhost:8080).
> 백엔드는 `http://localhost:3000` 을 CORS로 허용하도록 설정돼 있다.

## 화면
| 경로 | 설명 |
|------|------|
| `/` | 세션 시작 — 테넌트·subject·역할 선택 → `/dev/token`으로 JWT 발급 (localStorage 저장) |
| `/courses` | 과정 목록 (현재 테넌트만, RLS). INSTRUCTOR/ADMIN은 과정 생성 폼 표시 |
| `/courses/[id]` | 레슨 목록 + (강사) 레슨 추가 + (학생) 수강신청 |
| `/my-learning` | 내 수강 목록 + 진도 조정 (100% → 자동 완료) |

## 격리·권한을 눈으로 확인하기
1. `/` 에서 **테넌트 A + INSTRUCTOR** 로 시작 → `/courses` 에 A의 과정 2개, 과정 생성·레슨 추가 가능.
2. 로그아웃 후 **테넌트 B** 로 시작 → B의 과정 1개만 보임 (A의 과정은 안 보임 = RLS).
3. **STUDENT** 로 시작 → 과정 생성/레슨 추가 버튼이 막히고, 시도 시 403 메시지. 대신 수강신청·진도 관리 가능.

## 구조
```
src/
├── app/
│   ├── layout.tsx          SessionProvider + NavBar
│   ├── page.tsx            세션 시작(로그인)
│   ├── courses/            목록 / [id] 상세
│   └── my-learning/        내 수강·진도
├── components/             SessionProvider(토큰 컨텍스트), NavBar
└── lib/api.ts              백엔드 API 클라이언트 + 타입
```

> 인증은 현재 백엔드의 dev 토큰(`/dev/token`)을 사용한다. 운영에서는 실제 로그인/IdP로 교체.
