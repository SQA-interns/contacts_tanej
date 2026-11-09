// Phase 2 gate: "every R, AC, SR, SB and NFR maps to a section of the specification".
// Reads the definitions (first table cell) of R, AC, EX in the stories, BR in business-rules.md, SR in
// security-requirements.md, NFR in quality-requirements.md and SB in security-baseline.md, expands the
// ranges written in section 10 of docs/02_specification.md and reports every id that is not there.
// Usage: node scripts/check-traceability.mjs   (writes logs/traceability-check.log)
import { readdirSync, readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';

const root = join(import.meta.dirname, '..', '..');
const project = join(root, '01_input', '01_project');
const general = join(root, '01_input', '00_general');

const definitions = new Set();
function collect(file, pattern) {
  for (const line of readFileSync(file, 'utf8').split('\n')) {
    const match = pattern.exec(line);
    if (match) definitions.add(match[1]);
  }
}
const storyDir = join(project, '01_requirements', 'stories');
for (const name of readdirSync(storyDir).filter((n) => n.endsWith('.md'))) {
  collect(join(storyDir, name), /^\|\s*((?:R|AC|EX)-\d{3}-\d{2})\s*\|/);
}
collect(join(project, '01_requirements', 'business-rules.md'), /^\|\s*(BR-\d{2})\s*\|/);
collect(join(project, '02_design', 'security-requirements.md'), /^\|\s*(SR-\d{2})\s*\|/);
collect(join(project, '02_design', 'quality-requirements.md'), /^\|\s*(NFR-\d{2})\s*\|/);
collect(join(general, 'security', 'security-baseline.md'), /^\|\s*(SB-\d{2})\s*\|/);

const spec = readFileSync(join(root, '02_output', 'docs', '02_specification.md'), 'utf8');
const section = spec.slice(spec.indexOf('## 10. Traceability'));
const mapped = new Set();
for (const m of section.matchAll(/\b(R|AC|EX)-(\d{3})-(\d{2})(?:\.\.(\d{2}))?/g)) {
  const [, prefix, story, from, to] = m;
  for (let n = Number(from); n <= Number(to ?? from); n++) mapped.add(`${prefix}-${story}-${String(n).padStart(2, '0')}`);
}
for (const m of section.matchAll(/\b(BR|SR|SB|NFR)-(\d{2})(?:\.\.(\d{2}))?/g)) {
  const [, prefix, from, to] = m;
  for (let n = Number(from); n <= Number(to ?? from); n++) mapped.add(`${prefix}-${String(n).padStart(2, '0')}`);
}

const missing = [...definitions].filter((id) => !mapped.has(id)).sort();
const counts = {};
for (const id of definitions) counts[id.split('-')[0]] = (counts[id.split('-')[0]] ?? 0) + 1;
const summary = Object.entries(counts).map(([k, v]) => `${k} ${v}`).join(', ');
const log = [`Defined: ${definitions.size} (${summary})`, `Mapped in section 10: ${[...definitions].length - missing.length}`, missing.length ? `MISSING: ${missing.join(' ')}` : 'Every id maps to a section.'];
writeFileSync(join(root, '02_output', 'logs', 'traceability-check.log'), log.join('\n') + '\n');
console.log(log.join('\n'));
process.exit(missing.length ? 1 : 0);
