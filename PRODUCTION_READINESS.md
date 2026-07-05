# 운영 준비도 점검 (Production Readiness)

실제 학원에 판매·배포하기 전 전반 상태를 감사한 결과입니다. 범례: ✅ 완료 · 🟡 부분/조건부 · 🔴 미비(출시 전 필요).

> 참고: "Git조차 없음" 리스크는 **이미 해결**되었습니다 — 원격 저장소(GitHub `LMS_tutoring`) + 커밋 이력 + GitHub Actions CI가 구성되어 있습니다.

---

## 영역별 상태

| 영역 | 상태 | 근거 / 비고 |
|---|---|---|
| **버전관리 · CI** | ✅ | Git(origin) + `.github/workflows/ci.yml`(백엔드 Testcontainers 테스트·빌드 + 프론트 빌드, push/PR 자동) |
| **컨테이너화** | ✅ | dev `docker-compose.yml` / 운영 `docker-compose.prod.yml`, 멀티스테이지 Dockerfile(백엔드·프론트) |
| **테넌트 격리** | ✅ | PostgreSQL **RLS**(FORCE) 전 테이블, 코드에 `WHERE tenant_id` 없이 DB가 강제. 격리 테스트 존재 |
| **권한(RBAC)** | ✅ | `@PreAuthorize` + JWT roles. 역할별 포털 분리(학습자/운영/플랫폼) |
| **인증(JWT)** | 🟡 | HS256 대칭키(`LMS_JWT_SECRET` 환경변수). 강한 시크릿이면 운영 가능. **RSA/IdP는 권장 하드닝** |
| **dev 토큰 노출** | ✅ | `/dev/token` 발급기는 `@Profile("dev")` — 운영(prod)에서 미등록(404) |
| **기동 시 시크릿 검사** | ✅ | `ProductionReadinessGuard`(prod) — JWT/플랫폼 비번이 기본·약하면 **기동 차단**(단위테스트 포함) |
| **시크릿 관리** | 🟡 | `.env`(gitignore) 환경변수 주입. Vault/AWS SSM 등 시크릿 매니저는 미도입(소규모엔 `.env`로 충분) |
| **DB 마이그레이션** | ✅ | Flyway V1~V30, 앞으로만 적용. 스키마 검증(`ddl-auto: validate`) |
| **백업** | 🟡 | `scripts/backup.sh`·`restore.sh` 제공(pg_dump/gzip, 보관정리). **자동 크론은 서버에서 설정 필요** |
| **배포 · HTTPS · 도메인** | 🟡 | prod 컴포즈 + **Caddy 자동 HTTPS(Let's Encrypt)** 구성 제공. 실제 클라우드 배포·DNS는 미수행(계정 필요) |
| **결제(PG)** | 🔴 | `MockPaymentProvider`(즉시성공). **실 토스페이먼츠/아임포트 미연동** — 학원비 실수납하려면 필요(seam·문서 준비) |
| **알림** | 🟡 | 인앱 ✅ 실동작 · **이메일(SMTP) ✅ 설정 시 실발송** · SMS/카카오 알림톡 🔴 스텁(SIMULATED) |
| **관측성** | 🟡 | actuator health/info + 로그. Sentry/APM/메트릭 수집·대시보드는 미도입 |
| **테스트/품질** | ✅ | 백엔드 125개 테스트(Testcontainers) 전부 green + E2E 스크립트 다수 |
| **성능/확장** | 🟡 | 단일 인스턴스. 일부 집계 `findAll()`(현 규모 무해, 대량 시 쿼리 최적화·인덱스 필요). Redis/캐시 없음 |
| **영상 스토리지** | 🟡 | 로컬 파일(도커 볼륨). S3/CDN·HLS 스트리밍 미도입(트래픽 커지면 필요) |

---

## 출시 전 필수 (진짜 판매 전 반드시)
1. **강한 시크릿 주입** — `LMS_JWT_SECRET`(32자+ 무작위), 플랫폼 관리자 비번, DB owner 비번. → 가드가 자동 강제(약하면 기동 거부). ✅ 준비됨(값만 채우면 됨)
2. **도메인 + HTTPS 실배포** — `docker-compose.prod.yml`로 서버 배포, DNS 연결(Caddy 자동 인증서). 🟡 구성 제공, 실행 필요
3. **실 결제(PG) 연동** — 학원비를 실제로 받으려면 토스페이먼츠 연동 필수. 🔴 (DEPLOY.md §9 절차, 키 필요)
4. **자동 백업 크론** — `scripts/backup.sh`를 매일 크론 등록. 🟡

## 권장 (출시 직후~안정화)
- **SMS/카카오 알림톡** 실연동(결석·리포트 알림을 문자로) — NHN Cloud/카카오 비즈메시지
- **RSA JWT 또는 외부 IdP** — 키 회전·다중 서비스 검증
- **에러 트래킹(Sentry) + 모니터링** — 장애 조기 탐지
- **DB 자동 백업을 오브젝트 스토리지로** 오프사이트 보관

## 나중에 (규모 커질 때)
- 영상 **S3/Mux + HLS 스트리밍**, CDN
- Redis 캐시, 읽기 복제본, 수평 확장
- 집계 쿼리 최적화(리포트/애널리틱스의 `findAll()` → 인덱스·집계 쿼리)
- 비례정산(proration)·세금계산서, 웹훅 기반 결제 정산 자동화

---

## 한 줄 결론
**아키텍처·보안 기본기(RLS·RBAC·프로파일 분리·기동 가드·CI·컨테이너화)는 출시 수준.** 실제 판매까지 남은 핵심은 **① 실서버 배포(도메인·HTTPS) ② 실 결제(PG) 연동 ③ 강한 시크릿·자동 백업 운영** 세 가지이며, 앞의 둘은 외부 계정(클라우드·토스)이 있어야 마무리됩니다. 그 외 코드·구성은 이 저장소에서 모두 준비되어 있습니다.
