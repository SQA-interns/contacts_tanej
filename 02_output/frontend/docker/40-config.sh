#!/bin/sh
# Writes /config.js from the environment when the container starts (NFR-12, ADR-07).
# A missing required variable stops the container.
set -eu

: "${FIREBASE_PROJECT_ID:?FIREBASE_PROJECT_ID is required}"
: "${FIREBASE_WEB_API_KEY:?FIREBASE_WEB_API_KEY is required}"
: "${FIREBASE_AUTH_DOMAIN:?FIREBASE_AUTH_DOMAIN is required}"
FIREBASE_AUTH_EMULATOR_URL="${FIREBASE_AUTH_EMULATOR_URL:-}"

for value in "$FIREBASE_PROJECT_ID" "$FIREBASE_WEB_API_KEY" "$FIREBASE_AUTH_DOMAIN" "$FIREBASE_AUTH_EMULATOR_URL"; do
  case "$value" in
    *'"'* | *'\'* | *'<'*) echo "40-config.sh: a configuration value holds a character that is not allowed" >&2; exit 1 ;;
  esac
done

cat > /usr/share/nginx/html/config.js <<EOF
window.__APP_CONFIG__ = {
  firebaseProjectId: "${FIREBASE_PROJECT_ID}",
  firebaseApiKey: "${FIREBASE_WEB_API_KEY}",
  firebaseAuthDomain: "${FIREBASE_AUTH_DOMAIN}",
  firebaseAuthEmulatorUrl: "${FIREBASE_AUTH_EMULATOR_URL}",
  apiBaseUrl: "/api",
};
EOF
