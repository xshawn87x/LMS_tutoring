# syntax=docker/dockerfile:1

### 1) 의존성 설치
FROM node:20-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

### 2) 빌드 — Next standalone 산출물 생성
FROM node:20-alpine AS build
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
# NEXT_PUBLIC_* 는 빌드 시점에 번들에 박힘. 브라우저에서 백엔드로 직접 호출하므로
# 기본값(http://localhost:8080)이 그대로 호스트에 매핑되어 동작한다. 필요 시 build arg로 덮어쓴다.
ARG NEXT_PUBLIC_API_BASE=http://localhost:8080
ENV NEXT_PUBLIC_API_BASE=$NEXT_PUBLIC_API_BASE
RUN npm run build

### 3) 런타임 — standalone 서버만 실어 실행
FROM node:20-alpine AS runtime
WORKDIR /app
ENV NODE_ENV=production
ENV PORT=3000
ENV HOSTNAME=0.0.0.0

RUN addgroup -S nodejs && adduser -S nextjs -G nodejs

COPY --from=build --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=build --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs
EXPOSE 3000
CMD ["node", "server.js"]
