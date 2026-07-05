# 배포 가이드 (운영)

이 문서는 LMS SaaS를 **실제 서버(클라우드)에 배포**하는 절차입니다. 로컬 개발은 `docker-compose.yml`(dev 프로파일)을, 운영은 `docker-compose.prod.yml`(prod 프로파일)을 씁니다.

---

## 1. 준비물
- Ubuntu 22.04+ 서버 1대 (AWS EC2 / Oracle Cloud 프리티어 / 어디든). RAM 2GB+ 권장.
- Docker + Docker Compose 설치
- 도메인 1개 + DNS 관리 권한 (예: `your-domain.com`, `api.your-domain.com`)

## 2. 도메인 DNS
두 A 레코드를 서버 공인 IP로 향하게 합니다.
```
your-domain.com       A   <서버IP>
api.your-domain.com   A   <서버IP>
```

## 3. 배포
```bash
git clone https://github.com/xshawn87x/LMS_tutoring.git /opt/lms && cd /opt/lms
cp .env.example .env
# .env 편집: LMS_JWT_SECRET(openssl rand -base64 48), 플랫폼 관리자 비번,
#            POSTGRES_OWNER_PASSWORD, APP_DOMAIN/API_DOMAIN, NEXT_PUBLIC_API_BASE
docker compose -f docker-compose.prod.yml up -d --build
```
- **Caddy가 Let's Encrypt 인증서를 자동 발급** → `https://your-domain.com` 접속.
- 시크릿이 약하면 백엔드가 기동을 거부합니다(`ProductionReadinessGuard`). 로그 확인: `docker compose -f docker-compose.prod.yml logs backend`.

## 4. 운영 프로파일이 하는 것 (prod)
- **dev 토큰 발급기(/dev/token) 비활성화** (`@Profile("dev")`) — 임의 토큰 발급 차단
- **Swagger/OpenAPI 비공개**, actuator는 health/info만 노출
- **기동 시 시크릿 검사**: `LMS_JWT_SECRET`·`LMS_PLATFORM_ADMIN_PASSWORD`가 기본/약하면 기동 중단
- 리버스 프록시(Caddy) 뒤 `X-Forwarded-*` 신뢰

## 5. 백업 / 복구
```bash
./scripts/backup.sh                 # ./backups/lms_<시각>.sql.gz 생성, 14일 보관
crontab -e                          # 매일 새벽 3시 자동 백업
#   0 3 * * *  cd /opt/lms && ./scripts/backup.sh >> /var/log/lms-backup.log 2>&1
./scripts/restore.sh ./backups/lms_YYYYMMDD_HHMMSS.sql.gz   # 복구
```

## 6. 업그레이드 / 롤백
```bash
git pull && docker compose -f docker-compose.prod.yml up -d --build   # 마이그레이션(Flyway)은 자동 적용
# 롤백: 이전 커밋 체크아웃 후 재빌드. DB 스키마는 앞으로만(Flyway) — 파괴적 변경 전 백업 필수.
```

---

## 7. DB 롤 비밀번호 교체 (선택, 권장)
앱 접속 롤 `lms_app`의 비밀번호는 마이그레이션 기본값(`lms_app_pw`)입니다. DB 포트를 외부에 열지 않으면(현 구성) 노출면이 낮지만, 교체하려면:
```bash
docker exec -it lms-postgres psql -U lms_owner -d lms -c "ALTER ROLE lms_app PASSWORD '새비번';"
# .env 의 LMS_APP_DB_PASSWORD 를 같은 값으로 맞추고 backend 재기동
docker compose -f docker-compose.prod.yml up -d backend
```

## 8. 이메일(SMTP) 실발송 — 학부모 리포트/결석 알림
`.env`에 SMTP 값을 채우면 `EmailMessageSender`가 실제로 발송합니다(미설정 시 SIMULATED).
```
SPRING_MAIL_HOST=smtp.gmail.com   # 또는 SES 등
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=...           # Gmail은 앱 비밀번호 사용
SPRING_MAIL_PASSWORD=...
APP_MAIL_FROM=no-reply@your-domain.com
```

## 9. 결제(PG) 실연동 — 토스페이먼츠 (국내 학원 권장)
현재는 `MockPaymentProvider`(즉시 성공). 실 연동 절차:
1. 토스페이먼츠 가입 → 시크릿 키 발급.
2. 프론트: 학원비 결제 화면에 **토스 결제위젯** 붙여 `paymentKey`·`orderId`·`amount` 획득.
3. 백엔드: `PaymentProvider`(또는 학원비 `StudentPaymentService`)에 `TossPaymentProvider` 구현 추가 —
   `POST https://api.tosspayments.com/v1/payments/confirm` 를 **시크릿 키(Basic 인증)** 로 호출해 승인.
4. `.env`에 `APP_PAYMENT_PROVIDER=toss`, `APP_PAYMENT_TOSS_SECRET_KEY=...` → `MockPaymentProvider` 자동 비활성화(`@ConditionalOnProperty`).
   BillingService/청구 로직은 그대로 재사용됩니다.
> 아임포트(포트원)도 동일 패턴 — 결제 승인/웹훅 검증만 프로바이더 구현으로 교체.

## 10. SMS / 카카오 알림톡 (선택)
현재 `SmsMessageSender`·`KakaoMessageSender`는 스텁(SIMULATED). NHN Cloud SMS / 카카오 비즈메시지 키를 받아
해당 `MessageSender` 구현을 실제 API 호출로 교체하면 결석·리포트 알림이 문자/알림톡으로 나갑니다(인앱은 이미 실동작).

## 11. 인증 하드닝 (권장 다음 단계)
- 현재 JWT는 **HS256(대칭키, `LMS_JWT_SECRET`)** — 강한 시크릿이면 운영 가능.
- 더 강화하려면 **RSA(비대칭)** 또는 외부 IdP(Cognito/Auth0) 도입: `TokenService`를 RSA 서명으로,
  `SecurityConfig`의 `JwtDecoder`를 공개키/JWKS로 교체. (키 회전·다중 서비스 검증에 유리)

## 12. CI (이미 구성됨)
`.github/workflows/ci.yml` — push/PR 시 백엔드 테스트(Testcontainers)+빌드, 프론트 빌드 자동 실행.
배포 자동화(CD)는 이 워크플로에 `docker compose ... up -d` SSH 스텝을 추가하면 됩니다.
