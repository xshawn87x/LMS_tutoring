# syntax=docker/dockerfile:1

### 1) 빌드 스테이지 — Gradle로 실행 가능한 bootJar 생성
# 컨테이너(Linux)에는 Avast SSL 가로채기가 없으므로 로컬 트러스트스토어(gradle.properties)는 불필요.
FROM gradle:8.10.2-jdk17 AS build
WORKDIR /app

# 의존성 캐시 최적화: 빌드 스크립트를 먼저 복사
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# 소스 복사 후 bootJar 생성 (테스트는 CI에서 별도 수행 → 이미지 빌드 시 스킵)
COPY src ./src
RUN gradle bootJar --no-daemon -x test

### 2) 런타임 스테이지 — 가벼운 JRE 이미지에 jar만 실어 실행
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

# 헬스체크용 curl + 비루트 사용자
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -r lms && useradd -r -g lms lms

COPY --from=build /app/build/libs/*.jar app.jar

# 업로드 저장 디렉터리 (compose에서 볼륨 마운트)
RUN mkdir -p /app/uploads && chown -R lms:lms /app
USER lms

EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=6 \
    CMD curl -fsS http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
