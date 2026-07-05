#!/usr/bin/env bash
# DB 복구 — 백업 파일(.sql.gz)을 실행 중인 postgres 컨테이너에 복원한다.
# 사용:  ./scripts/restore.sh ./backups/lms_YYYYMMDD_HHMMSS.sql.gz
# 주의: 기존 데이터에 덮어써질 수 있으니 운영에선 유지보수 창에서 신중히 실행하세요.
set -euo pipefail

FILE="${1:?복원할 백업 파일 경로를 지정하세요 (예: ./backups/lms_....sql.gz)}"
CONTAINER="${LMS_PG_CONTAINER:-lms-postgres}"

[ -f "$FILE" ] || { echo "파일이 없습니다: $FILE"; exit 1; }
echo "[restore] ${FILE} → ${CONTAINER}"
read -r -p "정말 복원하시겠습니까? (yes 입력) " ans
[ "$ans" = "yes" ] || { echo "취소됨"; exit 1; }

gunzip -c "$FILE" | docker exec -i "$CONTAINER" psql -U lms_owner -d lms
echo "[restore] 완료"
