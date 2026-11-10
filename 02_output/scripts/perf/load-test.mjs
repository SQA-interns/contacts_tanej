// Timing and load test (T-N01): NFR-01 (list, search, filter under 1 s, 95th percentile, 10,000
// contacts, 20 parallel users), NFR-02 (open a contact under 500 ms, 95th percentile), NFR-04 (100
// users, no error responses), DoD-P03, EX-009-08, EX-010-07.
//
// Needs the stack up (docker compose --env-file ../.env up --build -d, from 02_output/). The script
//   1. creates 100 test accounts in the Authentication Emulator (perf-user-001 .. 100, fixed ids),
//   2. loads scripts/perf/seed-10000.sql into the db container,
//   3. runs 20 virtual users for DURATION_S seconds (search, filter, sort, open contact),
//   4. lets all 100 users call the API once (capacity),
//   5. writes the result to logs/perf-load-test.log and exits 1 if a limit is missed.
// Usage: node scripts/perf/load-test.mjs [--duration 60] [--skip-seed]
// Environment: API_URL (http://localhost:8080), EMULATOR_URL (http://localhost:9099).
import { spawnSync } from 'node:child_process';
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const API_URL = process.env.API_URL ?? 'http://localhost:8080';
const EMULATOR_URL = process.env.EMULATOR_URL ?? 'http://localhost:9099';
const IDENTITY = `${EMULATOR_URL}/identitytoolkit.googleapis.com/v1`;
const PROJECT = 'demo-contacts';
const PASSWORD = 'Geslo1234';
const USERS = 100;
const PARALLEL = 20;
const BIG_GROUP = 'a0000000-0000-4000-8000-000000000000';
const SMALL_GROUPS = [1, 2, 3, 4].map((g) => `a0000000-0000-4000-8000-00000000000${g}`);
// Each virtual user pauses between rounds so that it stays under the rate limit of 100 requests per
// minute and user (SR-09), as a person working with the list would.
const THINK_MS = 1600;
const LIMIT_MS = { search: 1000, open: 500 }; // NFR-01, NFR-02 (95th percentile)

const here = dirname(fileURLToPath(import.meta.url));
const root = join(here, '..', '..');
const argv = process.argv.slice(2);
const durationS = Number(argv[argv.indexOf('--duration') + 1]) || 60;
const skipSeed = argv.includes('--skip-seed');
const emailOf = (n) => `perf${String(n).padStart(3, '0')}@example.com`;
const uidOf = (n) => `perf-user-${String(n).padStart(3, '0')}`;

const lines = [];
const log = (text) => {
  console.log(text);
  lines.push(text);
};

async function identity(path, body, headers = {}) {
  const response = await fetch(`${IDENTITY}/${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...headers },
    body: JSON.stringify(body),
  });
  return { ok: response.ok, body: await response.json() };
}

async function signIn(n) {
  const body = { email: emailOf(n), password: PASSWORD, returnSecureToken: true };
  const result = await identity('accounts:signInWithPassword?key=fake-api-key', body);
  return result.ok ? result.body.idToken : null;
}

async function ensureAccounts() {
  for (let n = 1; n <= USERS; n++) {
    if (await signIn(n)) continue;
    const created = await identity('accounts:signUp?key=fake-api-key', {
      localId: uidOf(n),
      email: emailOf(n),
      password: PASSWORD,
      displayName: `Perf User ${n}`,
      returnSecureToken: true,
    }, { Authorization: 'Bearer owner' });
    if (!created.ok) throw new Error(`No account ${emailOf(n)}: ${JSON.stringify(created.body)}`);
    await identity(
      `projects/${PROJECT}/accounts:update`,
      { localId: created.body.localId, emailVerified: true, displayName: `Perf User ${n}` },
      { Authorization: 'Bearer owner' },
    );
  }
}

function loadSeed() {
  const sql = readFileSync(join(here, 'seed-10000.sql'));
  const run = spawnSync(
    'docker',
    ['compose', '--env-file', '../.env', 'exec', '-T', 'db', 'psql', '-q', '-U', 'contacts', '-d', 'contacts'],
    { cwd: root, input: sql, encoding: 'utf8', env: { ...process.env, MSYS_NO_PATHCONV: '1' } },
  );
  if (run.status !== 0) throw new Error(`Seed failed: ${run.stderr}`);
}

async function call(token, method, path, body) {
  const started = performance.now();
  let status = 0;
  try {
    const response = await fetch(`${API_URL}${path}`, {
      method,
      headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
    status = response.status;
    await response.arrayBuffer();
  } catch {
    status = 0;
  }
  return { ms: performance.now() - started, status };
}

const search = (token, group, body) => call(token, 'POST', `/groups/${group}/contacts/search`, body);

/** The searches of the test: list, text, filters of every type, sort, a later page. */
const SEARCHES = [
  { name: 'list', body: {} },
  { name: 'list page 200', body: { page: 200 } },
  { name: 'list sorted by company', body: { sort: { field: 'company', direction: 'DESC' } } },
  { name: 'text (EX-009-08)', body: { q: 'kovacic' } },
  { name: 'text with accents', body: { q: 'Šuster Maja' } },
  { name: 'text e-mail part', body: { q: 'c0.77' } },
  { name: 'tag filter', body: { conditions: [{ type: 'TAGS_ALL', tags: ['udelezenec2026'] }] } },
  {
    name: 'three conditions (EX-010-07)',
    body: {
      q: 'novak',
      conditions: [
        { type: 'TAGS_ANY', tags: ['udelezenec2025', 'vip'] },
        { type: 'ATTRIBUTE_EQUALS', key: 'kompetenca', value: 'java' },
        { type: 'COUNTRY', country: 'SI' },
      ],
    },
  },
  { name: 'attribute exists', body: { conditions: [{ type: 'ATTRIBUTE_EXISTS', key: 'kompetenca' }] } },
  { name: 'tag not', body: { conditions: [{ type: 'TAG_NOT', tag: 'neObvescaj' }] } },
  { name: 'company', body: { conditions: [{ type: 'COMPANY', company: 'Acme' }] } },
];

const samples = { search: new Map(), open: [] };
const errors = [];

function record(kind, name, result) {
  if (result.status < 200 || result.status >= 300) errors.push(`${kind} ${name}: HTTP ${result.status}`);
  if (kind === 'open') samples.open.push(result.ms);
  else (samples.search.get(name) ?? samples.search.set(name, []).get(name)).push(result.ms);
}

async function virtualUser(n, deadline) {
  const token = await signIn(n);
  if (!token) throw new Error(`No token for user ${n}`);
  let round = n;
  while (performance.now() < deadline) {
    const s = SEARCHES[round % SEARCHES.length];
    const answer = await search(token, BIG_GROUP, s.body);
    record('search', s.name, answer);
    // open a contact of the big group (NFR-02): any id of the seed
    const index = 1 + ((round * 7919 + n * 104729) % 10000);
    const id = `f0000000-0000-4000-8000-${String(index).padStart(12, '0')}`;
    record('open', 'open contact', await call(token, 'GET', `/groups/${BIG_GROUP}/contacts/${id}`));
    round += 1;
    await new Promise((resolve) => setTimeout(resolve, THINK_MS));
  }
}

async function capacity() {
  // NFR-04: 100 registered users, 20 at the same time, no error response
  const failures = [];
  for (let start = 1; start <= USERS; start += PARALLEL) {
    const batch = [];
    for (let n = start; n < start + PARALLEL && n <= USERS; n++) {
      batch.push(
        (async () => {
          const token = await signIn(n);
          const groups = await call(token, 'GET', '/groups');
          const big = await search(token, BIG_GROUP, {});
          const small = await search(token, SMALL_GROUPS[n % SMALL_GROUPS.length], {});
          for (const r of [groups, big, small]) if (r.status !== 200) failures.push(`user ${n}: HTTP ${r.status}`);
        })(),
      );
    }
    await Promise.all(batch);
  }
  return failures;
}

function percentile(values, p) {
  const sorted = [...values].sort((a, b) => a - b);
  return sorted[Math.min(sorted.length - 1, Math.ceil((p / 100) * sorted.length) - 1)];
}

const fmt = (ms) => `${ms.toFixed(0)} ms`;

async function main() {
  log(`Load test ${new Date().toISOString()}  api=${API_URL}  users=${USERS}  parallel=${PARALLEL}  duration=${durationS}s`);
  log('Creating the accounts ...');
  await ensureAccounts();
  if (!skipSeed) {
    log('Loading the seed (10,000 contacts) ...');
    loadSeed();
  }
  const deadline = performance.now() + durationS * 1000;
  log(`Running ${PARALLEL} virtual users for ${durationS} s ...`);
  await Promise.all(Array.from({ length: PARALLEL }, (_, i) => virtualUser(i + 1, deadline)));

  log('Capacity run, 100 users ...');
  const capacityFailures = await capacity();

  const all = [...samples.search.values()].flat();
  let failed = false;
  log('');
  log('| Request | n | p50 | p95 | max |');
  log('|---|---|---|---|---|');
  for (const [name, values] of samples.search) {
    log(`| ${name} | ${values.length} | ${fmt(percentile(values, 50))} | ${fmt(percentile(values, 95))} | ${fmt(Math.max(...values))} |`);
  }
  log(`| open contact | ${samples.open.length} | ${fmt(percentile(samples.open, 50))} | ${fmt(percentile(samples.open, 95))} | ${fmt(Math.max(...samples.open))} |`);
  const p95Search = percentile(all, 95);
  const p95Open = percentile(samples.open, 95);
  log('');
  for (const [label, value, limit] of [
    ['NFR-01 list, search, filter p95 (all searches together)', p95Search, LIMIT_MS.search],
    ['NFR-02 open a contact p95', p95Open, LIMIT_MS.open],
  ]) {
    const ok = value < limit;
    failed ||= !ok;
    log(`${ok ? 'PASS' : 'FAIL'} ${label}: ${fmt(value)} (limit ${limit} ms)`);
  }
  for (const [name, values] of samples.search) {
    if (percentile(values, 95) >= LIMIT_MS.search) {
      failed = true;
      log(`FAIL ${name}: p95 ${fmt(percentile(values, 95))} (limit ${LIMIT_MS.search} ms)`);
    }
  }
  const total = errors.length + capacityFailures.length;
  failed ||= total > 0;
  log(`${total === 0 ? 'PASS' : 'FAIL'} NFR-04 no error responses: ${total} of ${all.length + samples.open.length + USERS * 3} requests`);
  for (const e of [...errors, ...capacityFailures].slice(0, 20)) log(`  ${e}`);

  mkdirSync(join(root, 'logs'), { recursive: true });
  writeFileSync(join(root, 'logs', 'perf-load-test.log'), `${lines.join('\n')}\n`);
  process.exit(failed ? 1 : 0);
}

main().catch((error) => {
  console.error(error);
  process.exit(2);
});
