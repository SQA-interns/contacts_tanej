// Runtime configuration (NFR-12, ADR-06). In a container this file is generated at start from the
// environment variables FIREBASE_PROJECT_ID, FIREBASE_WEB_API_KEY, FIREBASE_AUTH_DOMAIN and
// FIREBASE_AUTH_EMULATOR_URL (docker/frontend-entrypoint.sh). These values are for local
// development against the Firebase Authentication Emulator only; they hold no secret.
window.__APP_CONFIG__ = {
  firebaseProjectId: 'demo-contacts',
  firebaseApiKey: 'fake-api-key',
  firebaseAuthDomain: 'localhost',
  firebaseAuthEmulatorUrl: 'http://localhost:9099',
  apiBaseUrl: '/api',
  // demo deployments only (DEMO_TOOLS): "Fill with test data"
  demoTools: false,
};
