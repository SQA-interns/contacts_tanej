#!/usr/bin/env bash
# Restart test (T-N03, NFR-05): write data, restart the containers, read it back, call /health.
# Needs the stack up (docker compose --env-file ../.env up --build -d, from 02_output/).
#   ./scripts/restart-test.sh
# Creates one group and one contact with a tag and an attribute for the account
# ana.novak@example.com (seed users, scripts/seed.sh; the account is created here if missing),
# restarts backend and database (stop and start, volumes kept), waits for /health 200, then checks
# that the group and the contact are still there with the same values. Log: logs/restart-test.log.
# Exit 0 on pass, 1 on a failed check.
set -euo pipefail
export MSYS_NO_PATHCONV=1
cd "$(dirname "$0")/.."

API_URL="${API_URL:-http://localhost:8080}"
EMULATOR_URL="${EMULATOR_URL:-http://localhost:9099}"
IDENTITY="$EMULATOR_URL/identitytoolkit.googleapis.com/v1"
EMAIL="restart-test@example.com"
PASSWORD="Geslo1234"
compose=(docker compose --env-file ../.env)
mkdir -p logs
LOG=logs/restart-test.log
: > "$LOG"
say() { echo "$*" | tee -a "$LOG"; }
fail() { say "FAIL $*"; exit 1; }
json() { node -e "const v=JSON.parse(require('fs').readFileSync(0,'utf8'));const p=process.argv[1].split('.');let x=v;for(const k of p)x=x?.[k];process.stdout.write(x===undefined?'':String(x))" "$1"; }

say "Restart test $(date -u +%FT%TZ)"

token() {
  local r
  r="$(curl -s -X POST "$IDENTITY/accounts:signInWithPassword?key=fake-api-key" \
    -H 'Content-Type: application/json' \
    -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\",\"returnSecureToken\":true}")"
  echo "$r" | json idToken
}

TOKEN="$(token)"
if [ -z "$TOKEN" ]; then
  r="$(curl -s -X POST "$IDENTITY/accounts:signUp?key=fake-api-key" -H 'Content-Type: application/json' \
    -H 'Authorization: Bearer owner' \
    -d "{\"localId\":\"restart-test-user\",\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\",\"displayName\":\"Restart Test\",\"returnSecureToken\":true}")"
  local_id="$(echo "$r" | json localId)"
  [ -n "$local_id" ] || fail "no account: $r"
  curl -s -X POST "$IDENTITY/projects/demo-contacts/accounts:update" -H 'Content-Type: application/json' \
    -H 'Authorization: Bearer owner' \
    -d "{\"localId\":\"$local_id\",\"emailVerified\":true,\"displayName\":\"Restart Test\"}" > /dev/null
  TOKEN="$(token)"
fi
[ -n "$TOKEN" ] || fail "no token"

api() { curl -s -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' "$@"; }

STAMP="$(date +%s)"
say "Writing a group and a contact ..."
GROUP="$(api -X POST "$API_URL/groups" -d "{\"name\":\"Restart $STAMP\"}")"
GROUP_ID="$(echo "$GROUP" | json id)"
[ -n "$GROUP_ID" ] || fail "group not created: $GROUP"
# Git Bash on Windows does not pass non-ASCII command-line arguments to curl as UTF-8, so the body
# is written to a file by node (S with caron as Š in the source, UTF-8 on disk) and sent as is.
BODY_FILE=logs/restart-body.json  # relative: node on Windows cannot open a Git Bash /tmp path
trap 'rm -f "$BODY_FILE"' EXIT
node -e '
const body = {firstName: "Špela", lastName: "Restart" + process.argv[1],
  email: "restart" + process.argv[1] + "@example.com", tags: ["restart"],
  attributes: [{key: "stage", value: "before"}]};
require("fs").writeFileSync(process.argv[2], JSON.stringify(body), "utf8");
' "$STAMP" "$BODY_FILE"
CONTACT="$(api -X POST "$API_URL/groups/$GROUP_ID/contacts" --data-binary "@$BODY_FILE")"
CONTACT_ID="$(echo "$CONTACT" | json id)"
[ -n "$CONTACT_ID" ] || fail "contact not created: $CONTACT"
say "  group $GROUP_ID, contact $CONTACT_ID"

say "Restarting backend and database ..."
"${compose[@]}" restart db backend >> "$LOG" 2>&1

say "Waiting for /health ..."
code=000
for _ in $(seq 1 60); do
  code="$(curl -s -o /dev/null -w '%{http_code}' "$API_URL/health" || true)"
  [ "$code" = "200" ] && break
  sleep 2
done
[ "$code" = "200" ] || fail "/health answers $code after the restart"
say "PASS /health 200"

# the emulator keeps no state across its own restart; it was not restarted here, so the token is valid
READ="$(api "$API_URL/groups/$GROUP_ID/contacts/$CONTACT_ID")"
[ "$(echo "$READ" | json lastName)" = "Restart$STAMP" ] || fail "contact not read back: $READ"
[ "$(echo "$READ" | json firstName)" = "Špela" ] || fail "first name changed: $READ"
[ "$(echo "$READ" | json email)" = "restart$STAMP@example.com" ] || fail "e-mail changed: $READ"
echo "$READ" | grep -q 'restart' || fail "tag lost: $READ"
echo "$READ" | grep -q 'before' || fail "attribute lost: $READ"
say "PASS contact read back with its values, tag and attribute"
api "$API_URL/groups" | grep -q "$GROUP_ID" || fail "group not in the list"
say "PASS group in the list"
say "PASS NFR-05 data intact after the restart"
