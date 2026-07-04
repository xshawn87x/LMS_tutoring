-- ============================================================
-- V16: 기관 상태(status) — 연체/정지 게이팅
-- ============================================================
-- ACTIVE(정상) / PAST_DUE(연체: 지난달 인보이스 미결제) / SUSPENDED(수동 정지).
-- ACTIVE가 아니면 기능 모듈 이용이 차단된다(FeatureService에서 강제).
-- 값 전환: 자동(청구 마감 시 연체 판정/결제 시 해제) + 수동(슈퍼관리자 정지/해제).

ALTER TABLE tenant ADD COLUMN status VARCHAR(12) NOT NULL DEFAULT 'ACTIVE';
-- tenant는 V13에서 lms_app에 UPDATE 권한을 이미 부여함(요금제 변경). status도 그 권한으로 갱신.
