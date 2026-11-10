#!/usr/bin/env bash
# Restore test (T-N04, NFR-06): restore a backup into a clean Compose stack and compare the data.
#   ./scripts/restore.sh [backup-file]      (from 02_output/, source stack up; default: newest in backups/)
# Takes a fresh backup when no file is given and backups/ is empty. Starts a second, empty database as
# the Compose project "contact-groups-restore" (own volume, no ports), restores the dump into it, then
# compares row count and an md5 checksum of every table with the source database. Removes the
# restore stack afterwards. Log: logs/backup-restore-test.log. Exit 0 on identical data, 1 otherwise.
# Real restore in production: stop the backend, `pg_restore --clean --if-exists -U contacts -d contacts`
# into the db service (`docker compose exec -T db pg_restore ... < file`), start the backend.
set -euo pipefail
export MSYS_NO_PATHCONV=1
cd "$(dirname "$0")/.."

COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.yml}"
ENV_FILE="${ENV_FILE:-../.env}"
src=(docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE")
dst=(docker compose -p contact-groups-restore -f "$COMPOSE_FILE" --env-file "$ENV_FILE")
mkdir -p logs
LOG=logs/backup-restore-test.log
: > "$LOG"
say() { echo "$*" | tee -a "$LOG"; }
cleanup() { "${dst[@]}" down -v >/dev/null 2>&1 || true; }
trap cleanup EXIT
fail() { say "FAIL $*"; exit 1; }

say "Restore test $(date -u +%FT%TZ)"
FILE="${1:-}"
if [ -z "$FILE" ]; then
  FILE="$(ls -1t backups/contacts-*.dump 2>/dev/null | head -n 1 || true)"
  [ -n "$FILE" ] || FILE="$(./scripts/backup.sh)"
fi
[ -s "$FILE" ] || fail "backup file $FILE not found"
say "Backup file: $FILE"

say "Starting a clean database (project contact-groups-restore) ..."
cleanup
"${dst[@]}" up -d --wait db >>"$LOG" 2>&1 || fail "the restore database did not start"
"${dst[@]}" exec -T db psql -q -U contacts -d contacts -tA \
  -c "select count(*) from pg_tables where schemaname='public'" | grep -qx 0 || fail "the restore database is not empty"

say "Restoring ..."
"${dst[@]}" exec -T db pg_restore -U contacts -d contacts --no-owner < "$FILE" >>"$LOG" 2>&1 \
  || fail "pg_restore reported errors (see log)"

# One line per table: name|row count|md5 of all rows in a fixed order.
SUMMARY_SQL=$(cat <<'SQL'
do $$ declare r record; c bigint; h text; begin
  create temp table summary(t text, n bigint, h text) on commit drop;
  for r in select tablename from pg_tables where schemaname = 'public' order by tablename loop
    execute format('select count(*), coalesce(md5(string_agg(x::text, ''|'' order by x::text)), '''') from %I x', r.tablename) into c, h;
    insert into summary values (r.tablename, c, h);
  end loop;
  raise notice '%', (select string_agg(t || '|' || n || '|' || h, E'\n' order by t) from summary);
end $$;
SQL
)
summary() { "$@" exec -T db psql -q -U contacts -d contacts -c "$SUMMARY_SQL" 2>&1 | sed -e 's/^NOTICE:  //' -e 's/^psql:.*NOTICE:  //'; }
A="$(summary "${src[@]}")"
B="$(summary "${dst[@]}")"
say "Source database:"; say "$A"
say "Restored database:"; say "$B"
[ -n "$A" ] || fail "no tables found in the source database"
if [ "$A" = "$B" ]; then
  TABLES="$(echo "$A" | wc -l | tr -d ' ')"
  say "PASS NFR-06 $TABLES tables with identical row counts and checksums"
else
  fail "the restored data differs from the source"
fi
