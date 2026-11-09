// Dependency scan of the web app (SB-08, SR-19): `npm audit --omit=dev` must show no High or Critical
// advisory without a recorded decision. Allowed advisories are listed in audit-allowlist.json with the
// decision id. Usage: node scripts/audit.mjs
import { spawnSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { join } from 'node:path';

const allowed = JSON.parse(readFileSync(join(import.meta.dirname, 'audit-allowlist.json'), 'utf8'));
const allowedIds = new Set(allowed.map((entry) => entry.advisory));

const npm = process.platform === 'win32' ? 'npm.cmd' : 'npm';
const run = spawnSync(npm, ['audit', '--omit=dev', '--json'], {
  encoding: 'utf8',
  shell: process.platform === 'win32',
});
const report = JSON.parse(run.stdout);

const open = [];
for (const [name, vulnerability] of Object.entries(report.vulnerabilities ?? {})) {
  if (!['high', 'critical'].includes(vulnerability.severity)) continue;
  for (const via of vulnerability.via.filter((item) => typeof item === 'object')) {
    const id = String(via.url ?? '')
      .split('/')
      .pop();
    if (['high', 'critical'].includes(via.severity) && !allowedIds.has(id))
      open.push(`${name}: ${id} (${via.severity})`);
  }
}

if (open.length > 0) {
  console.error('Open High or Critical advisories without a decision:\n' + [...new Set(open)].join('\n'));
  process.exit(1);
}
console.log(
  `No open High or Critical advisory; ${allowedIds.size} allowed by decision (${allowed.map((a) => a.decision).join(', ')}).`,
);
