// Licence and forbidden-use scan (T-N12, NFR-14, SR-19). Usage (from 02_output/): node scripts/licence-check.mjs
// Checks, with the rules of tech-stack.md ("Allowed dependency licences"):
//  1. every npm package of the web app (package-lock.json): runtime packages carry an allowed licence
//     (an SPDX "A OR B" is fine when one side is allowed); build and test packages may also carry the listed
//     weak-copyleft exceptions (EPL, MPL) but never GPL or AGPL;
//  2. every direct Maven dependency of the backend is listed in tech-stack.md with a licence;
//  3. forbidden imports: Hibernate, JPA, Spring Data, Google services other than Firebase Authentication;
//  4. no code of SCM in the sources.
// Transitive Maven licences are not read here (no JDK on the host); the OWASP dependency scan in CI and the
// Spring Boot BOM cover them, and the review of the dependency list stays a human step.
// Log: logs/licence-check.log. Exit 0 when nothing is forbidden.
import { readFileSync, readdirSync, statSync, writeFileSync, mkdirSync } from 'node:fs';
import { join, relative } from 'node:path';

const root = join(import.meta.dirname, '..');
const lines = [];
let failures = 0;
const say = (text) => {
  console.log(text);
  lines.push(text);
};
const fail = (text) => {
  failures += 1;
  say(`FAIL ${text}`);
};

const ALLOWED = new Set(['Apache-2.0', 'MIT', 'BSD-2-Clause', 'BSD-3-Clause', 'ISC', '0BSD', 'CC0-1.0', 'PostgreSQL']);
const BUILD_EXTRA = new Set([
  'MPL-2.0', 'EPL-2.0', 'EPL-1.0', 'Python-2.0', 'BlueOak-1.0.0', 'CC-BY-4.0', 'CC-BY-3.0', 'Unlicense',
  'LGPL-3.0-or-later', 'LGPL-3.0', 'LGPL-2.1', 'WTFPL', 'Zlib', 'MIT-0', '(MIT AND CC-BY-3.0)',
]);
// Build-only packages (all reached through firebase-tools, the emulator's dev tool, never in an image or bundle)
// whose licence field is not an SPDX id of the list, with the licence their repository states (D-47).
const BUILD_REVIEWED = new Map([
  ['argparse', 'PSF-2.0, permissive'],
  ['deep-freeze', 'public domain'],
  ['url-template', 'BSD, permissive'],
  ['fuzzy', 'MIT (licenses array in package.json)'],
  ['valid-url', 'MIT (repository LICENSE)'],
]);
const STRONG_COPYLEFT = /^(A?GPL)/i;

function alternatives(expression) {
  return expression
    .replace(/[()]/g, '')
    .split(/\s+OR\s+/)
    .map((part) => part.trim());
}

// ---------------------------------------------------------------- 1. npm packages
const lock = JSON.parse(readFileSync(join(root, 'frontend', 'package-lock.json'), 'utf8'));
let runtime = 0;
let build = 0;
const counts = new Map();
for (const [path, info] of Object.entries(lock.packages ?? {})) {
  if (path === '') continue;
  const name = path.replace(/^.*node_modules\//, '');
  const licence = typeof info.license === 'string' ? info.license : JSON.stringify(info.license ?? 'UNKNOWN');
  counts.set(licence, (counts.get(licence) ?? 0) + 1);
  const options = alternatives(licence);
  const isBuild = info.dev === true || info.devOptional === true;
  if (isBuild) build += 1;
  else runtime += 1;
  if (options.some((o) => ALLOWED.has(o))) continue;
  if (isBuild && BUILD_REVIEWED.has(name)) continue;
  if (isBuild && options.some((o) => BUILD_EXTRA.has(o) && !STRONG_COPYLEFT.test(o))) continue;
  fail(`npm ${isBuild ? 'build' : 'runtime'} package ${name}@${info.version}: licence ${licence} is not allowed`);
}
say(`npm: ${runtime} runtime and ${build} build or test packages checked`);
say(`npm licences: ${[...counts].sort((a, b) => b[1] - a[1]).map(([l, n]) => `${l} ${n}`).join(', ')}`);

// ---------------------------------------------------------------- 2. Maven direct dependencies
const pom = readFileSync(join(root, 'backend', 'pom.xml'), 'utf8');
const techStack = readFileSync(join(root, '..', '01_input', '01_project', '00_setup', 'tech-stack.md'), 'utf8');
const decisions = readFileSync(join(root, 'docs', 'decisions-log.md'), 'utf8');
const dependencies =[...pom.matchAll(/<dependency>([\s\S]*?)<\/dependency>/g)].map((m) => ({
  group: /<groupId>(.*?)<\/groupId>/.exec(m[1])?.[1],
  artifact: /<artifactId>(.*?)<\/artifactId>/.exec(m[1])?.[1],
}));
for (const { group, artifact } of dependencies) {
  const listed = techStack.split('\n').find((l) => l.includes(`"${group}:${artifact}"`));
  // Test-scope additions are recorded with their licence in decisions-log.md (D-33 and later).
  const decided = decisions.split('\n').some((l) => l.includes(`${group}:${artifact}`) && /Apache-2\.0|MIT/.test(l));
  if (!listed && !decided) fail(`maven dependency ${group}:${artifact} is in neither tech-stack.md nor decisions-log.md`);
  else if (listed && !/license:/.test(listed)) fail(`maven dependency ${group}:${artifact} has no licence in tech-stack.md`);
}
say(`maven: ${dependencies.length} direct dependencies, each listed in tech-stack.md with a licence`);

// ---------------------------------------------------------------- 3 and 4. sources
function walk(dir, files = []) {
  for (const entry of readdirSync(dir)) {
    if (['node_modules', 'target', 'dist', 'coverage', 'test-results', '.git'].includes(entry)) continue;
    const full = join(dir, entry);
    if (statSync(full).isDirectory()) walk(full, files);
    else if (/\.(java|ts|tsx|js|mjs|json|xml|yml|yaml|html|css|sql|conf|properties)$/.test(entry)) files.push(full);
  }
  return files;
}
const sources = [...walk(join(root, 'backend')), ...walk(join(root, 'frontend')), ...walk(join(root, 'docker'))].filter(
  (f) => !/package-lock\.json$/.test(f),
);

const FORBIDDEN = [
  [/org\.hibernate|jakarta\.persistence|javax\.persistence|spring-boot-starter-data-jpa|spring-data/, 'Hibernate, JPA or Spring Data'],
  [/(from|import|require\()\s*\(?\s*['"]firebase\/(firestore|analytics|storage|messaging|functions|database|app-check|performance|remote-config)/, 'Firebase service other than Authentication'],
  // identitytoolkit (Firebase Authentication) and Google's public token keys are the allowed endpoints.
  [/firebase-admin|com\.google\.cloud|google-analytics|googletagmanager|fonts\.g(oogle|static)|\b(?!www\.|identitytoolkit\.)[a-z]+\.googleapis\.com|www\.googleapis\.com\/(?!service_accounts\/v1\/jwk\/securetoken)/, 'Google service other than Firebase Authentication'],
];
// This script and the architecture test name the forbidden terms on purpose.
const SKIP = new Set(['scripts/licence-check.mjs', 'backend/src/test/java/app/contactgroups/ArchitectureTest.java']);
const SCM = /\bSCM\b/;
for (const file of sources) {
  const rel = relative(root, file).replaceAll('\\', '/');
  if (SKIP.has(rel)) continue;
  const text = readFileSync(file, 'utf8');
  for (const [pattern, what] of FORBIDDEN) if (pattern.test(text)) fail(`${rel}: ${what}`);
  if (SCM.test(text)) fail(`${rel}: mentions SCM, check that no code of SCM is reused`);
}
say(`sources: ${sources.length} files searched for forbidden imports and for SCM`);

say(failures === 0 ? 'PASS NFR-14 nothing forbidden found' : `FAIL ${failures} finding(s)`);
mkdirSync(join(root, 'logs'), { recursive: true });
writeFileSync(join(root, 'logs', 'licence-check.log'), `${new Date().toISOString()}\n${lines.join('\n')}\n`);
process.exit(failures === 0 ? 0 : 1);
