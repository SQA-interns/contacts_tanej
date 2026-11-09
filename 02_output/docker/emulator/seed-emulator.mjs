// Creates the fixed test users in the Authentication Emulator and prints "KEY=uid" lines for
// scripts/seed.sh (which passes them to the SQL seed). Run inside the emulator container:
//   docker compose exec -T emulator node /srv/seed-emulator.mjs
// No real person: example.com addresses and one shared test password (SR-18).
const BASE = 'http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1';
const PROJECT = 'demo-contacts';
const KEY = 'fake-api-key';
export const PASSWORD = 'Geslo1234';

const USERS = [
  { key: 'ANA', email: 'ana.novak@example.com', name: 'Ana Novak' },
  { key: 'MARKO', email: 'marko@example.com', name: 'Marko Pirc' },
  { key: 'PETRA', email: 'petra@example.com', name: 'Petra Kos' },
  { key: 'BORIS', email: 'boris@example.com', name: 'Boris Zupan' },
];

async function call(path, body, headers = {}) {
  const response = await fetch(`${BASE}/${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...headers },
    body: JSON.stringify(body),
  });
  return { ok: response.ok, body: await response.json() };
}

for (const user of USERS) {
  let result = await call(`accounts:signUp?key=${KEY}`, {
    email: user.email,
    password: PASSWORD,
    displayName: user.name,
    returnSecureToken: true,
  });
  if (!result.ok) {
    // already created by an earlier run
    result = await call(`accounts:signInWithPassword?key=${KEY}`, {
      email: user.email,
      password: PASSWORD,
      returnSecureToken: true,
    });
  }
  const uid = result.body.localId;
  if (!uid) throw new Error(`No user id for ${user.email}: ${JSON.stringify(result.body)}`);
  // the emulator's owner token may set the e-mail as verified (BR-03)
  await call(`projects/${PROJECT}/accounts:update`, { localId: uid, emailVerified: true, displayName: user.name }, { Authorization: 'Bearer owner' });
  console.log(`${user.key}_UID=${uid}`);
}
