// Export timing (T-N02): NFR-03 (1,000 contacts with tags and attributes in under 10 s) and NFR-15
// (10,000 contacts complete without error and without running out of memory; time is only recorded).
//
// Needs the stack up and the seed of load-test.mjs (group "Perf 10000", 10,000 contacts, user
// perf001 is Owner). Run `node scripts/perf/load-test.mjs --duration 1` once if the seed is missing.
// Each export runs RUNS times; the file is read back as a ZIP and its rows are counted
// by the number of <row> elements of sheet1. The result goes to logs/perf-export-time.log; exit 1 if
// a limit is missed, an export fails, or the backend container restarted (out of memory).
// Usage: node scripts/perf/export-time.mjs [--runs 3]
// Environment: API_URL (http://localhost:8080), EMULATOR_URL (http://localhost:9099).
import { spawnSync } from 'node:child_process';
import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { inflateRawSync } from 'node:zlib';

const API_URL = process.env.API_URL ?? 'http://localhost:8080';
const EMULATOR_URL = process.env.EMULATOR_URL ?? 'http://localhost:9099';
const BIG_GROUP = 'a0000000-0000-4000-8000-000000000000';
const LIMIT_1000_MS = 10_000; // NFR-03
const root = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const argv = process.argv.slice(2);
const runs = Number(argv[argv.indexOf('--runs') + 1]) || 3;

const lines = [];
const log = (text) => {
  console.log(text);
  lines.push(text);
};

async function signIn() {
  const response = await fetch(
    `${EMULATOR_URL}/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=fake-api-key`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: 'perf001@example.com', password: 'Geslo1234', returnSecureToken: true }),
    },
  );
  const body = await response.json();
  if (!response.ok) throw new Error('No account perf001: run scripts/perf/load-test.mjs first');
  return body.idToken;
}

const contactId = (index) => `f0000000-0000-4000-8000-${String(index).padStart(12, '0')}`;

/** Counts the rows of the first sheet of an .xlsx (a ZIP) without a library, via the central directory. */
function countRows(zip) {
  const end = zip.lastIndexOf(Buffer.from([0x50, 0x4b, 0x05, 0x06]));
  let entry = zip.readUInt32LE(end + 16);
  for (let n = zip.readUInt16LE(end + 10); n > 0; n--) {
    const size = zip.readUInt32LE(entry + 20);
    const nameLength = zip.readUInt16LE(entry + 28);
    const extraLength = zip.readUInt16LE(entry + 30);
    const commentLength = zip.readUInt16LE(entry + 32);
    const local = zip.readUInt32LE(entry + 42);
    const name = zip.toString('utf8', entry + 46, entry + 46 + nameLength);
    if (name === 'xl/worksheets/sheet1.xml') {
      const start = local + 30 + zip.readUInt16LE(local + 26) + zip.readUInt16LE(local + 28);
      const xml = inflateRawSync(zip.subarray(start, start + size)).toString('utf8');
      return (xml.match(/<row /g) ?? []).length;
    }
    entry += 46 + nameLength + extraLength + commentLength;
  }
  return null;
}

function backendState() {
  const run = spawnSync(
    'docker',
    ['inspect', '-f', '{{.RestartCount}} {{.State.OOMKilled}} {{.State.StartedAt}}', 'contact-groups-backend-1'],
    { encoding: 'utf8' },
  );
  return run.status === 0 ? run.stdout.trim() : 'unknown';
}

async function runExport(token, name, body) {
  const times = [];
  let rows = null;
  let size = 0;
  for (let i = 0; i < runs; i++) {
    const started = performance.now();
    const response = await fetch(`${API_URL}/groups/${BIG_GROUP}/exports`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    });
    const bytes = Buffer.from(await response.arrayBuffer());
    times.push(performance.now() - started);
    if (response.status !== 200) throw new Error(`${name}: HTTP ${response.status} ${bytes.toString('utf8').slice(0, 200)}`);
    size = bytes.length;
    rows = countRows(bytes);
  }
  return { name, times, rows, size };
}

async function main() {
  log(`Export timing ${new Date().toISOString()}  api=${API_URL}  runs=${runs}`);
  const stateBefore = backendState();
  const token = await signIn();
  const ids1000 = Array.from({ length: 1000 }, (_, i) => contactId(i + 1));
  const results = [
    await runExport(token, '1,000 ticked contacts (NFR-03)', { language: 'en', contactIds: ids1000 }),
    await runExport(token, '10,000 contacts by filter (NFR-15)', { language: 'sl', filter: { q: null, conditions: [] } }),
  ];
  const stateAfter = backendState();

  let failed = false;
  log('');
  log('| Export | runs | times (ms) | max | file | rows in sheet |');
  log('|---|---|---|---|---|---|');
  for (const r of results) {
    const max = Math.max(...r.times);
    log(`| ${r.name} | ${runs} | ${r.times.map((t) => t.toFixed(0)).join(', ')} | ${max.toFixed(0)} ms | ${(r.size / 1024).toFixed(0)} KiB | ${r.rows ?? 'n/a'} |`);
  }
  log('');
  const max1000 = Math.max(...results[0].times);
  const ok1000 = max1000 < LIMIT_1000_MS;
  failed ||= !ok1000;
  log(`${ok1000 ? 'PASS' : 'FAIL'} NFR-03 1,000 contacts: slowest ${max1000.toFixed(0)} ms (limit ${LIMIT_1000_MS} ms)`);
  const lost = stateBefore !== stateAfter && stateBefore.split(' ')[0] !== stateAfter.split(' ')[0];
  failed ||= lost || stateAfter.includes(' true ');
  log(`${lost ? 'FAIL' : 'PASS'} NFR-15 10,000 contacts: no error, backend not restarted or OOM-killed (before: ${stateBefore}; after: ${stateAfter}); time recorded only: ${Math.max(...results[1].times).toFixed(0)} ms`);

  mkdirSync(join(root, 'logs'), { recursive: true });
  writeFileSync(join(root, 'logs', 'perf-export-time.log'), `${lines.join('\n')}\n`);
  process.exit(failed ? 1 : 0);
}

main().catch((error) => {
  console.error(error);
  process.exit(2);
});
