#!/usr/bin/env bash
# DB 백업 — 실행 중인 postgres 컨테이너에서 pg_dump 로 압축 백업을 만든다.
# 사용:  ./scripts/backup.sh [출력디렉터리]   (기본: ./backups)
# 크론 예:  0 3 * * *  cd /opt/lms && ./scripts/backup.sh >> /var/log/lms-backup.log 2>&1
set -euo pipefail

OUT_DIR="${1:-./backups}"
CONTAINER="${LMS_PG_CONTAINER:-lms-postgres}"
STAMP="$(date +%Y%m%d_%H%M%S)"
FILE="${OUT_DIR}/lms_${STAMP}.sql.gz"
KEEP_DAYS="${LMS_BACKUP_KEEP_DAYS:-14}"

mkdir -p "$OUT_DIR"
echo "[backup] ${CONTAINER} → ${FILE}"
docker exec "$CONTAINER" pg_dump -U lms_owner -d lms --no-owner | gzip > "$FILE"
echo "[backup] 완료: $(du -h "$FILE" | cut -f1)"

# 오래된 백업 정리
find "$OUT_DIR" -name 'lms_*.sql.gz' -type f -mtime +"$KEEP_DAYS" -print -delete || true
echo "[backup] ${KEEP_DAYS}일 지난 백업 정리 완료"
