#!/usr/bin/env bash
# One command that loads the local seed data (phase 2): the test users in the Authentication
# Emulator and the matching rows in PostgreSQL. Start the stack first:
#   docker compose --env-file ../.env up --build -d      (from 02_output/)
#   ./scripts/seed.sh
# Test users: ana.novak@example.com, marko@example.com, petra@example.com, boris@example.com,
# all with the password Geslo1234 and a verified e-mail. No real person, no real account (SR-18).
set -euo pipefail
# Git Bash on Windows rewrites /srv/... paths; this keeps them as they are
export MSYS_NO_PATHCONV=1
cd "$(dirname "$0")/.."

compose=(docker compose --env-file ../.env)

echo "Creating the test users in the emulator ..."
uids="$("${compose[@]}" exec -T emulator node /srv/seed-emulator.mjs)"

args=()
while IFS='=' read -r key value; do
  [ -n "$key" ] || continue
  name="$(printf '%s' "${key%_UID}" | tr '[:upper:]' '[:lower:]')_uid"
  args+=(-v "${name}=${value}")
done <<< "$uids"

echo "Loading the seed rows ..."
"${compose[@]}" exec -T db psql -q -U contacts -d contacts "${args[@]}" < scripts/seed/seed.sql
echo "Seed loaded."
