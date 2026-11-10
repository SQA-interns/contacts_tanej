#!/usr/bin/env bash
# Clean-host test (T-N10, NFR-12): the delivered Compose set-up, built from a clean copy of the committed
# sources with only the `.env`, starts and E2E-01 passes.
#   ./scripts/clean-host-test.sh          (from 02_output/; the normal stack must be stopped first:
#                                          docker compose --env-file ../.env down      (keeps its data))
# Steps: git archive HEAD into a temporary folder (no node_modules, no build output, no data), copy only
# ../.env into it, `docker compose up --build -d --wait` as the project "contact-groups-clean" (its own images
# cache layers, but its volumes are new), run e2e/e2e-01-first-use.spec.ts in Chromium against it, remove
# the project with its volumes. Log: logs/clean-host-test.log. Exit 0 on pass.
# Needs: docker, git, node with the frontend's node_modules (for Playwright). Uses ports 5173, 8080, 9099.
set -euo pipefail
export MSYS_NO_PATHCONV=1
cd "$(dirname "$0")/.."
ROOT="$(cd .. && pwd)"

mkdir -p logs
LOG="$PWD/logs/clean-host-test.log"
: > "$LOG"
say() { echo "$*" | tee -a "$LOG"; }
fail() { say "FAIL $*"; exit 1; }

[ -f ../.env ] || fail "../.env is missing (copy secrets.env.example and fill it in)"
for port in 5173 8080 9099; do
  if (exec 3<>"/dev/tcp/127.0.0.1/$port") 2>/dev/null; then
    fail "port $port is in use: stop the normal stack first (docker compose --env-file ../.env down)"
  fi
done

export COMPOSE_PROJECT_NAME=contact-groups-clean
CLEAN="$(mktemp -d "${TMPDIR:-/tmp}/contacts-clean-XXXXXX")"
cleanup() {
  (cd "$CLEAN/02_output" 2>/dev/null && docker compose --env-file ../.env down -v >>"$LOG" 2>&1) || true
  rm -rf "$CLEAN"
}
trap cleanup EXIT

say "Clean host test $(date -u +%FT%TZ), commit $(git rev-parse --short HEAD)"
say "Copying the committed sources to $CLEAN ..."
git -C "$ROOT" archive HEAD 02_output | tar -x -C "$CLEAN"
cp ../.env "$CLEAN/.env"
say "Files in the copy: $(find "$CLEAN" -type f | wc -l | tr -d ' '); .env copied, nothing else added"

say "docker compose up --build --wait (several minutes on a cold cache) ..."
(cd "$CLEAN/02_output" && docker compose --env-file ../.env up --build -d --wait) >>"$LOG" 2>&1 \
  || fail "the stack did not start (see log)"
say "PASS stack healthy: $(docker compose -p contact-groups-clean ps --format '{{.Service}}={{.Status}}' | tr '\n' ' ')"

say "Running E2E-01 in Chromium ..."
(cd frontend && PLAYWRIGHT_BASE_URL=http://localhost:5173 npx playwright test e2e/e2e-01-first-use.spec.ts \
  --project=chromium --reporter=list) >>"$LOG" 2>&1 || fail "E2E-01 failed (see log)"
say "PASS NFR-12 E2E-01 passes on the clean copy"
