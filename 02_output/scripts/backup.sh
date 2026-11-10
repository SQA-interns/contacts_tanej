#!/usr/bin/env bash
# Backup of the database (T-N04, NFR-06): one compressed pg_dump file, 14 days of retention.
#   ./scripts/backup.sh [backup-dir]            (from 02_output/, stack up; default dir: backups/)
# Env: COMPOSE_FILE (default docker-compose.yml; use docker-compose.prod.yml in production),
#      ENV_FILE (default ../.env), RETENTION_DAYS (default 14).
# Daily run (cron, host time): 15 2 * * * cd /opt/contacts && ./scripts/backup.sh /var/backups/contacts
# Writes contacts-YYYYMMDDTHHMMSSZ.dump, deletes dumps older than RETENTION_DAYS, prints the file name.
# What it saves: the whole database "contacts" (all tables and the Flyway history). Nothing else needs
# a backup: the images are rebuilt from the sources, and the accounts live in Firebase Authentication.
set -euo pipefail
export MSYS_NO_PATHCONV=1
cd "$(dirname "$0")/.."

COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.yml}"
ENV_FILE="${ENV_FILE:-../.env}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"
DIR="${1:-backups}"
compose=(docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE")

mkdir -p "$DIR"
FILE="$DIR/contacts-$(date -u +%Y%m%dT%H%M%SZ).dump"
if ! "${compose[@]}" exec -T db pg_dump -U contacts -d contacts --format=custom --compress=6 > "$FILE.part"; then
  rm -f "$FILE.part"
  echo "FAIL pg_dump" >&2
  exit 1
fi
mv "$FILE.part" "$FILE"
[ -s "$FILE" ] || { echo "FAIL empty backup $FILE" >&2; exit 1; }

find "$DIR" -maxdepth 1 -name 'contacts-*.dump' -mtime +"$RETENTION_DAYS" -delete
echo "$FILE"
