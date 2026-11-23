#!/usr/bin/env node
// Runner for the conductor (/sdd-run). Deterministic and agent-agnostic: it reads
// the handover in 02_output/docs/00_progress.md, starts the next session as a
// separate non-interactive process of the agent named for that session in
// run-config.md (Claude Code, Codex, a local model, any command template), and
// runs the hidden evaluation itself, so no agent ever sees the hidden suite.
//
// Usage:
//   node system/run.mjs --status              report the next step, run nothing
//   node system/run.mjs --once                run the next session, then report
//   node system/run.mjs --once --command "/sdd-clarify D-03: 2"   run this command instead
//   node system/run.mjs --vault               move the hidden suite out of the project
//   node system/run.mjs --hidden-eval         run the hidden suite on a copy, write results
//   node system/run.mjs --restore-hidden      copy the hidden suite back (before /sdd-release)
//   node system/run.mjs --jobs                run the requested long jobs only (manual runs)
//   node system/run.mjs --env-check           .env keys: filled, empty or missing (never a value)
//   node system/run.mjs --leak-check          files and commit messages holding a .env value (names only)
//   node system/run.mjs --once --force        run even though an earlier phase gate is missing
//                                            (humans only, never the conductor on its own)
//
// The last line of output is always: RUN_STATUS {json}
// status: ready | done | waiting | interactive | paused | refused | failed | complete
//   paused:  the agent hit its usage limit; nothing was retried
//   refused: the command needs a phase gate that 00_progress.md ("Gates") does not list
// Long jobs: a session that hands over with "Waiting for: runner jobs" lists commands in
// 02_output/logs/jobs/request.json; the runner runs them one at a time, without a time
// limit, writes <name>.log and <name>.result.json next to it, then starts the next command.
// No dependencies; Node 18 or newer.

import {
  readFileSync, writeFileSync, appendFileSync, mkdirSync, existsSync, readdirSync,
  statSync, cpSync, rmSync, mkdtempSync,
} from "node:fs";
import { spawnSync } from "node:child_process";
import { createHash } from "node:crypto";
import { join, dirname, basename, relative, resolve } from "node:path";
import { tmpdir } from "node:os";
import { fileURLToPath } from "node:url";

const ROOT = dirname(dirname(fileURLToPath(import.meta.url)));
const DOCS = join(ROOT, "02_output/docs");
const PROGRESS = join(DOCS, "00_progress.md");
const DECISIONS = join(DOCS, "decisions-log.md");
const HIDDEN_MANIFEST = join(DOCS, "03_hidden-manifest.sha256");
const HIDDEN_RESULTS = join(DOCS, "06_hidden-evaluation.md");
const HIDDEN_DIR = join(ROOT, "02_output/hidden-tests");
const RUN_CONFIG = join(ROOT, "01_input/01_project/00_setup/run-config.md");
const LOG_DIR = join(ROOT, "02_output/logs/run");
const JOBS_DIR = join(ROOT, "02_output/logs/jobs");
const JOBS_REQUEST = join(JOBS_DIR, "request.json");

// Sessions that are a conversation with the developer and cannot run headless.
const INTERACTIVE = new Set(["/sdd-grill-me", "/sdd-design", "/sdd-change"]);

// The phase whose gate must be listed in 00_progress.md ("Gates") before a skill may start.
// Phase gates: 0 preflight, 1 clarify, 2 plan + wireframes, 3 acceptance tests,
// 4 every increment built and reviewed and the design applied, 5 quality gate, 6 verify.
const NEEDS_GATE = {
  "/sdd-clarify": 0, "/sdd-plan": 1, "/sdd-wireframes": 1, "/sdd-acceptance-tests": 2,
  "/sdd-implement": 3, "/sdd-review": 3, "/sdd-design": 3, "/sdd-fix": 3,
  "/sdd-quality-gate": 4, "/sdd-verify": 5, "/sdd-release": 6,
};

// Text an agent CLI prints when the account's usage limit stops a session.
// Checked only when the session ended with an error, so a handover that mentions a limit is not misread.
const LIMIT_TEXT = /(hit your [^\n]*limit|usage limit reached|limit · resets)/i;

// Tools every Claude session may use without a prompt; project commands come
// from run-config.md ("Allowed commands").
const BASE_TOOLS = [
  "Read", "Edit", "Write", "Glob", "Grep",
  "Bash(git *)", "Bash(ls *)", "Bash(mkdir *)", "Bash(find *)", "Bash(grep *)",
  "Bash(sed -n *)", "Bash(wc *)", "Bash(sha256sum *)", "Bash(shasum *)",
  "Bash(node *)", "Bash(npm *)", "Bash(npx *)", "Bash(docker *)",
  // Advisory and support-date sources for /sdd-preflight, /sdd-verify and /sdd-fix (v001 could not read them).
  "WebFetch(domain:endoflife.date)", "WebFetch(domain:osv.dev)", "WebFetch(domain:api.osv.dev)",
  "WebFetch(domain:nvd.nist.gov)", "WebFetch(domain:github.com)",
];

// Used when run-config.md has no "Agents" table.
const DEFAULT_AGENTS = {
  claude: { invocation: "slash", command: "claude -p {prompt} --model {model} --effort {effort} --permission-mode {permission} --output-format json --allowedTools {allowed}" },
};

// ---------- reading the handover and the configuration ----------

const read = (p) => (existsSync(p) ? readFileSync(p, "utf8") : "");

function field(text, name) {
  const m = text.match(new RegExp(`^- ${name}:[ \\t]*(.*)$`, "m"));
  return m ? m[1].trim() : "";
}

function section(text, heading) {
  const start = text.indexOf(`## ${heading}`);
  if (start < 0) return "";
  const rest = text.slice(start + heading.length + 3);
  const end = rest.search(/^## /m);
  return end < 0 ? rest : rest.slice(0, end);
}

// Rows of every Markdown table in `text` whose header contains all `need` columns.
function table(text, need) {
  const lines = text.split("\n");
  for (let i = 0; i < lines.length - 1; i++) {
    const head = cells(lines[i]);
    if (!need.every((n) => head.includes(n)) || !/^\|[\s|:-]+\|$/.test(lines[i + 1].trim())) continue;
    const rows = [];
    for (let j = i + 2; j < lines.length && lines[j].trim().startsWith("|"); j++) {
      const c = cells(lines[j]);
      rows.push(Object.fromEntries(head.map((h, k) => [h, c[k] ?? ""])));
    }
    return rows;
  }
  return [];
}

function cells(line) {
  const t = line.trim();
  if (!t.startsWith("|")) return [];
  return t.slice(1, t.endsWith("|") ? -1 : undefined).split("|").map((c) => c.trim());
}

const unquote = (s) => s.replace(/^`|`$/g, "");

function progress() {
  const text = read(PROGRESS);
  const last = section(text, "Last session");
  const next = section(text, "Next");
  const gates = table(section(text, "Gates"), ["Phase", "Passed in", "Commit"])
    .map((r) => Number.parseInt(r.Phase, 10)).filter((n) => Number.isInteger(n));
  return {
    lastBlock: last.trim(),
    lastSession: field(last, "Session"),
    result: field(last, "Result"),
    gate: field(last, "Gate"),
    command: field(next, "Command"),
    waiting: field(next, "Waiting for"),
    gates,
  };
}

// The gate a command needs, if it is missing; "" when the command may start.
function missingGate(command, gates) {
  const need = NEEDS_GATE[command.split(/\s+/)[0]];
  if (need === undefined) return "";
  const missing = [];
  for (let p = 0; p <= need; p++) if (!gates.includes(p)) missing.push(p);
  return missing.length ? `phase gate(s) ${missing.join(", ")} not listed in 00_progress.md ("Gates")` : "";
}

function runConfig() {
  const text = read(RUN_CONFIG);
  const defaultAgent = field(text, "Agent") || "claude";
  const sessions = {};
  for (const row of table(text, ["Skill", "Model", "Effort"])) {
    for (const m of row.Skill.matchAll(/`(\/[a-z-]+)`/g)) {
      sessions[m[1]] = { agent: unquote(row.Agent || "") || defaultAgent, model: unquote(row.Model), effort: unquote(row.Effort), maxTurns: unquote(row["Max turns"] || "") };
    }
  }
  const agents = { ...DEFAULT_AGENTS };
  for (const row of table(text, ["Agent", "Invocation", "Command"])) {
    agents[unquote(row.Agent)] = { invocation: unquote(row.Invocation), command: unquote(row.Command) };
  }
  const allowed = section(text, "Allowed commands").split("\n")
    .map((l) => l.match(/^- `(.+)`/)).filter(Boolean).map((m) => m[1]).filter((r) => !r.includes("<"));
  const vault = field(text, "Hidden vault") || `../${basename(ROOT)}.hidden-vault`;
  return { sessions, agents, allowed, permission: field(text, "Permission mode") || "default", vault: resolve(ROOT, vault) };
}

function decisionsWaiting(waiting) {
  const log = read(DECISIONS);
  return (waiting.match(/D-\d+/g) || []).map((id) => {
    const start = log.indexOf(`## ${id}:`);
    if (start < 0) return { id, text: "(not found in decisions-log.md)" };
    const rest = log.slice(start);
    const end = rest.slice(3).search(/^## D-\d+:/m);
    return { id, text: (end < 0 ? rest : rest.slice(0, end + 3)).trim() };
  });
}

// The session's last message (its "Report" block, AGENTS.md), passed on to the conductor.
let sessionReport = "";

function report(status, extra = {}) {
  const p = progress();
  console.log("RUN_STATUS " + JSON.stringify({
    status, last: p.lastSession, result: p.result, gate: p.gate, next: p.command, waiting: p.waiting, ...extra,
    report: sessionReport || undefined,
  }));
  process.exit(status === "failed" ? 1 : 0);
}

// ---------- starting a session through an agent adapter ----------

// Split a command template into words, honouring "double" and 'single' quotes.
function words(template) {
  const out = [];
  for (const m of template.matchAll(/"([^"]*)"|'([^']*)'|(\S+)/g)) out.push(m[1] ?? m[2] ?? m[3]);
  return out;
}

function prompt(command, invocation) {
  if (invocation === "slash") return command;
  if (invocation === "dollar") return "$" + command.slice(1);
  const [skill, ...rest] = command.split(/\s+/);
  const args = rest.join(" ");
  return `Read AGENTS.md first. Then follow the skill .claude/skills/${skill.slice(1)}/SKILL.md exactly` +
    (args ? `, with the argument: ${args}` : "") + ". Do only that session's work, then stop.";
}

function buildArgs(agent, row, command, cfg) {
  const values = {
    "{prompt}": [prompt(command, agent.invocation)], "{model}": [row.model], "{effort}": [row.effort],
    "{permission}": [cfg.permission], "{allowed}": [...BASE_TOOLS, ...cfg.allowed],
    "{limits}": row.maxTurns ? ["--max-turns", row.maxTurns] : [],
  };
  const out = [];
  for (const w of words(agent.command)) {
    if (values[w]) { out.push(...values[w]); continue; }
    out.push(w.replace(/\{(model|effort|permission)\}/g, (_, k) => values[`{${k}}`][0]));
  }
  return out;
}

function nextLogFile(skill) {
  mkdirSync(LOG_DIR, { recursive: true });
  const n = readdirSync(LOG_DIR).filter((f) => f.endsWith(".json")).length + 1;
  return join(LOG_DIR, `${String(n).padStart(3, "0")}-${skill.slice(1)}.json`);
}

function runSession(command) {
  const skill = command.split(/\s+/)[0];
  const cfg = runConfig();
  const row = cfg.sessions[skill];
  if (!row) return { ok: false, error: `no row for ${skill} in run-config.md` };
  const agent = cfg.agents[row.agent];
  if (!agent) return { ok: false, error: `no adapter for agent "${row.agent}" in run-config.md` };
  const [bin, ...args] = buildArgs(agent, row, command, cfg);
  const started = new Date();
  const res = spawnSync(bin, args, { cwd: ROOT, encoding: "utf8", maxBuffer: 64 * 1024 * 1024 });
  const ended = new Date();
  const logFile = nextLogFile(skill);
  let parsed = null;
  try { parsed = JSON.parse(res.stdout); } catch { /* not JSON: keep the raw text */ }
  const text = `${parsed ? parsed.result ?? "" : res.stdout ?? ""}\n${res.stderr ?? ""}`;
  const last = String(parsed ? parsed.result ?? "" : res.stdout ?? "");
  const block = last.lastIndexOf("Result:");
  sessionReport = (block >= 0 ? last.slice(block) : last).trim().slice(0, 4000);
  const errored = res.status !== 0 || res.error || parsed?.is_error;
  const limitLine = errored ? (text.split("\n").find((l) => LIMIT_TEXT.test(l)) || "").trim() : "";
  writeFileSync(logFile, JSON.stringify({
    command, agent: row.agent, model: row.model, effort: row.effort, exitCode: res.status,
    error: res.error ? String(res.error) : undefined,
    started: started.toISOString(), ended: ended.toISOString(),
    wall_ms: ended - started, duration_ms: parsed?.duration_ms, num_turns: parsed?.num_turns,
    usage: parsed?.usage, session_id: parsed?.session_id, total_cost_usd: parsed?.total_cost_usd,
    usage_limit: limitLine || undefined,
    result: parsed ? parsed.result : res.stdout, stderr: res.stderr,
  }, null, 2));
  return {
    ok: res.status === 0 && !res.error && !limitLine, logFile, limit: limitLine,
    error: res.error ? String(res.error) : limitLine || (res.status ? `exit code ${res.status}` : ""),
  };
}

// ---------- long jobs a session asks the runner to run (no time limit) ----------

// request.json: [{ "name": "frontend-mutation", "cwd": "02_output/frontend", "command": "npx stryker run" }]
function runJobs() {
  if (!existsSync(JOBS_REQUEST)) return { ok: false, error: "02_output/logs/jobs/request.json is missing" };
  let jobs;
  try { jobs = JSON.parse(read(JOBS_REQUEST)); } catch (e) { return { ok: false, error: `request.json is not valid JSON: ${e.message}` }; }
  if (!Array.isArray(jobs) || !jobs.every((j) => /^[a-z0-9-]+$/.test(j.name || "") && j.command)) {
    return { ok: false, error: "request.json must be a list of { name (a-z, 0-9, -), command, cwd }" };
  }
  const requested = statSync(JOBS_REQUEST).mtimeMs;
  const results = [];
  for (const job of jobs) { // one at a time: two heavy jobs at once slow each other into timeouts
    const resultFile = join(JOBS_DIR, `${job.name}.result.json`);
    if (existsSync(resultFile) && statSync(resultFile).mtimeMs > requested) { // already run for this request
      results.push(JSON.parse(read(resultFile))); continue;
    }
    const started = new Date();
    const res = spawnSync(job.command, { cwd: resolve(ROOT, job.cwd || "."), shell: true, encoding: "utf8", maxBuffer: 256 * 1024 * 1024 });
    const ended = new Date();
    writeFileSync(join(JOBS_DIR, `${job.name}.log`), `${res.stdout ?? ""}\n${res.stderr ?? ""}`);
    const result = { name: job.name, command: job.command, cwd: job.cwd || ".", exitCode: res.status, error: res.error ? String(res.error) : undefined, started: started.toISOString(), ended: ended.toISOString(), minutes: Math.round((ended - started) / 60000) };
    writeFileSync(resultFile, JSON.stringify(result, null, 2));
    results.push(result);
  }
  return { ok: true, note: results.map((r) => `${r.name}: exit ${r.exitCode}, ${r.minutes} min`).join("; ") };
}

// ---------- hidden evaluation (never shown to an agent) ----------

function files(dir) {
  const out = [];
  for (const name of readdirSync(dir)) {
    const p = join(dir, name);
    if (statSync(p).isDirectory()) out.push(...files(p)); else out.push(p);
  }
  return out;
}

const lfHash = (p) => createHash("sha256").update(readFileSync(p).toString("binary").replace(/\r\n/g, "\n"), "binary").digest("hex");

function vault() {
  const cfg = runConfig();
  if (!existsSync(HIDDEN_DIR)) return existsSync(cfg.vault) ? { ok: true, note: "already in the vault" } : { ok: false, error: "no hidden suite in 02_output/hidden-tests/" };
  if (!existsSync(join(HIDDEN_DIR, "hidden.json"))) return { ok: false, error: "02_output/hidden-tests/hidden.json is missing" };
  rmSync(cfg.vault, { recursive: true, force: true });
  cpSync(HIDDEN_DIR, cfg.vault, { recursive: true });
  rmSync(HIDDEN_DIR, { recursive: true, force: true });
  return { ok: true, note: `hidden suite moved to ${cfg.vault}` };
}

function manifestCheck(dir) {
  const lines = read(HIDDEN_MANIFEST).split("\n").filter((l) => /^[0-9a-f]{64}\s+/.test(l));
  if (!lines.length) return ["docs/03_hidden-manifest.sha256 is empty or missing"];
  const bad = [];
  for (const line of lines) {
    const [hash, ...rest] = line.split(/\s+/);
    const rel = rest.join(" ").replace(/^\*/, "").replace(/^hidden-tests\//, "");
    const p = join(dir, rel);
    if (!existsSync(p)) bad.push(`missing: ${rel}`); else if (lfHash(p) !== hash) bad.push(`changed: ${rel}`);
  }
  return bad;
}

function junitCases(xml) {
  const out = [];
  for (const m of xml.matchAll(/<testcase\b([^>]*?)(\/>|>([\s\S]*?)<\/testcase>)/g)) {
    const name = (m[1].match(/\bname="([^"]*)"/) || [])[1] || "";
    const body = m[3] || "";
    out.push({ name, failed: /<(failure|error|skipped)\b/.test(body) });
  }
  return out;
}

function hiddenEval() {
  const cfg = runConfig();
  if (existsSync(HIDDEN_DIR)) { const v = vault(); if (!v.ok) return v; }
  if (!existsSync(join(cfg.vault, "hidden.json"))) return { ok: false, error: `no hidden suite in ${cfg.vault}` };
  const spec = JSON.parse(read(join(cfg.vault, "hidden.json")));
  const head = spawnSync("git", ["rev-parse", "--short", "HEAD"], { cwd: ROOT, encoding: "utf8" }).stdout.trim() || "unknown";
  const bad = manifestCheck(cfg.vault);
  const work = mkdtempSync(join(tmpdir(), "hidden-eval-"));
  let cases = [], exitCode = null, error = "";
  try {
    if (!bad.length) {
      // Skip git data and tool sandboxes: a Stryker sandbox holds links to node_modules
      // that a copy cannot recreate on Windows without extra rights.
      const skipDirs = new Set([".git", ".stryker-tmp"]);
      cpSync(ROOT, work, { recursive: true, filter: (src) => !relative(ROOT, src).split(/[\\/]/).some((p) => skipDirs.has(p)) });
      cpSync(cfg.vault, join(work, "02_output/hidden-tests"), { recursive: true });
      const res = spawnSync(spec.command, { cwd: work, shell: true, encoding: "utf8", maxBuffer: 64 * 1024 * 1024, timeout: 60 * 60 * 1000 });
      exitCode = res.status;
      const junit = join(work, spec.junit);
      if (existsSync(junit)) cases = junitCases(read(junit)); else error = `JUnit report ${spec.junit} was not written`;
    }
  } finally {
    rmSync(work, { recursive: true, force: true });
  }
  const failed = cases.filter((c) => c.failed).map((c) => {
    const rule = (c.name.match(/\b(?:R-\d{3}-\d{2}|BR-\d{2})\b/) || ["unknown rule"])[0];
    const category = (c.name.match(/boundary|combination|invalid|role/i) || ["other"])[0].toLowerCase();
    return { rule, category };
  });
  const runs = (read(HIDDEN_RESULTS).match(/^## Run \d+/gm) || []).length;
  if (!existsSync(HIDDEN_RESULTS)) {
    writeFileSync(HIDDEN_RESULTS, "# Hidden evaluation\n\n> For: agent, human · Written by: `system/run.mjs --hidden-eval` · Agent: read-only\n\nResults of the held-out suite, run by the runner on a copy of the project. Failures name only the rule and the case category, never inputs, expected values or test code.\n");
  }
  const lines = [
    "", `## Run ${runs + 1}`, "",
    `- Commit: ${head}`, `- Date: ${new Date().toISOString()}`,
    `- Manifest: ${bad.length ? "MISMATCH (" + bad.join("; ") + ")" : "ok"}`,
    `- Tests: ${cases.length}, passed: ${cases.length - failed.length}, failed: ${failed.length}`,
    `- Exit code: ${exitCode ?? "not run"}${error ? `; ${error}` : ""}`, "",
    "| Rule | Case category |", "|---|---|",
    ...failed.map((f) => `| ${f.rule} | ${f.category} |`), "",
  ];
  appendFileSync(HIDDEN_RESULTS, lines.join("\n"));
  return { ok: !bad.length && !error, note: `hidden suite: ${cases.length - failed.length}/${cases.length} passed`, error: bad.length ? "manifest mismatch" : error };
}

function restoreHidden() {
  const cfg = runConfig();
  if (!existsSync(cfg.vault)) return { ok: existsSync(HIDDEN_DIR), note: "nothing to restore", error: "no vault" };
  cpSync(cfg.vault, HIDDEN_DIR, { recursive: true });
  return { ok: true, note: "hidden suite copied back to 02_output/hidden-tests/" };
}

// ---------- run summary (numbers for the retrospective) ----------

function summary() {
  const logs = existsSync(LOG_DIR) ? readdirSync(LOG_DIR).filter((f) => f.endsWith(".json")) : [];
  let cost = 0, failed = 0, paused = 0, ms = 0, turns = 0;
  const tokens = { input: 0, output: 0, cacheRead: 0, cacheWrite: 0 };
  const bySkill = {};
  for (const f of logs) {
    const j = JSON.parse(read(join(LOG_DIR, f)));
    const skill = (j.command || "").split(/\s+/)[0];
    const s = (bySkill[skill] ||= { n: 0, ms: 0 });
    s.n += 1;
    const d = typeof j.duration_ms === "number" ? j.duration_ms : j.wall_ms || 0;
    s.ms += d; ms += d;
    if (typeof j.num_turns === "number") turns += j.num_turns;
    const u = j.usage || {};
    tokens.input += u.input_tokens || 0; tokens.output += u.output_tokens || 0;
    tokens.cacheRead += u.cache_read_input_tokens || 0; tokens.cacheWrite += u.cache_creation_input_tokens || 0;
    if (typeof j.total_cost_usd === "number") cost += j.total_cost_usd;
    if (j.usage_limit) paused += 1; else if (j.exitCode) failed += 1;
  }
  const hours = (x) => (x / 3600000).toFixed(1);
  const dlog = read(DECISIONS);
  const count = (re) => (dlog.match(re) || []).length;
  const verifyRuns = (read(join(DOCS, "06_verification-report.md")).match(/^## Run \d+/gm) || []).length;
  const hidden = [...read(HIDDEN_RESULTS).matchAll(/^- Tests: (\d+), passed: (\d+)/gm)].map((m) => `${m[2]}/${m[1]} (${m[1] > 0 ? ((100 * m[2]) / m[1]).toFixed(1) : "0"} %)`);
  const commits = spawnSync("git", ["rev-list", "--count", "HEAD"], { cwd: ROOT, encoding: "utf8" }).stdout.trim() || "0";
  const rows = [
    ["Sessions started", String(logs.length)], ["Sessions failed (exit code)", String(failed)],
    ["Sessions paused by the usage limit", String(paused)],
    ["Sessions and agent time per skill (h)", Object.entries(bySkill).map(([k, v]) => `${k} ${v.n} / ${hours(v.ms)}`).join(", ") || "none"],
    ["Agent time (h)", hours(ms)], ["Model turns", String(turns)],
    ["Tokens (input / output / cache read / cache write)", `${tokens.input} / ${tokens.output} / ${tokens.cacheRead} / ${tokens.cacheWrite}`],
    ["Cost reported by the agents (USD)", cost ? cost.toFixed(2) : "not reported"],
    ["Decisions", String(count(/^## D-\d+:/gm))],
    ["Decisions by class (allow / open / block)", `${count(/^- Class: allow/gm)} / ${count(/^- Class: open/gm)} / ${count(/^- Class: block/gm)}`],
    ["Approvals (incl. changes to frozen tests)", String(count(/^- Type: approval/gm))],
    ["Hidden suite, first run (main result)", hidden[0] || "not run"],
    ["Hidden suite, last run", hidden.length ? `${hidden[hidden.length - 1]} after ${hidden.length} run(s)` : "not run"],
    ["Verification runs", String(verifyRuns)], ["Commits", commits],
  ];
  const block = "## Numbers\n\nWritten by `system/run.mjs --summary`.\n\n| Measure | Value |\n|---|---|\n" + rows.map(([a, b]) => `| ${a} | ${b} |`).join("\n") + "\n";
  const file = join(DOCS, "07_retrospective.md");
  const text = read(file);
  const start = text.indexOf("## Numbers");
  const next = start < 0 ? -1 : text.indexOf("\n## ", start + 3);
  const out = start < 0 ? text.trimEnd() + "\n\n" + block : text.slice(0, start) + block + (next < 0 ? "" : text.slice(next + 1));
  writeFileSync(file, out);
  return { ok: true, note: rows.map(([a, b]) => `${a}: ${b}`).join("; ") };
}

// ---------- .env check (key names and filled or empty, never a value) ----------

function envCheck() {
  const example = read(join(ROOT, "01_input/01_project/00_setup/secrets.env.example"));
  const env = read(join(ROOT, ".env"));
  if (!existsSync(join(ROOT, ".env"))) return { ok: false, error: ".env does not exist in the project folder" };
  const keys = (text) => new Map(text.split(/\r?\n/).map((l) => l.match(/^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=(.*)$/)).filter(Boolean)
    .map((m) => [m[1], m[2].replace(/\s+#.*$/, "").trim().replace(/^["']|["']$/g, "").trim()]));
  const have = keys(env);
  const lines = [];
  for (const [name] of keys(example)) lines.push(`${name}: ${!have.has(name) ? "missing" : have.get(name) ? "filled" : "empty"}`);
  for (const [name, value] of have) if (!keys(example).has(name)) lines.push(`${name}: ${value ? "filled" : "empty"} (not in secrets.env.example)`);
  return { ok: true, note: lines.join("; ") || "no keys" };
}

// Files (tracked and untracked, not ignored) and commit messages that contain a .env value of 6+ characters.
function leakCheck() {
  if (!existsSync(join(ROOT, ".env"))) return { ok: false, error: ".env does not exist in the project folder" };
  const values = read(join(ROOT, ".env")).split(/\r?\n/).map((l) => l.match(/^\s*[A-Za-z_][A-Za-z0-9_]*\s*=(.*)$/)).filter(Boolean)
    .map((m) => m[1].replace(/\s+#.*$/, "").trim().replace(/^["']|["']$/g, "").trim()).filter((v) => v.length >= 6);
  if (!values.length) return { ok: true, note: "no .env value of 6 or more characters to look for" };
  const git = (args) => spawnSync("git", args, { cwd: ROOT, encoding: "utf8", maxBuffer: 512 * 1024 * 1024 }).stdout || "";
  const hits = git(["ls-files", "-co", "--exclude-standard", "-z"]).split("\0").filter((f) => f && f !== ".env").filter((f) => {
    const p = join(ROOT, f);
    if (!existsSync(p) || statSync(p).size > 20 * 1024 * 1024) return false;
    const text = readFileSync(p, "latin1");
    return values.some((v) => text.includes(v));
  });
  const messages = git(["log", "--all", "--format=%B"]);
  const inCommits = values.filter((v) => messages.includes(v)).length;
  const found = hits.length || inCommits;
  return { ok: !found, note: `files with a .env value: ${hits.length ? hits.join(", ") : "none"}; commit messages with a .env value: ${inCommits}`, error: found ? "a .env value leaked (Critical)" : "" };
}

// ---------- template lint (structure, duplication and reference checks) ----------

function lintTemplate() {
  const errors = [];
  const skip = (rel) => /^(\.git|\.agents|02_output\/(logs|hidden-tests)|node_modules|01_input\/01_project\/00_intake\/customer_requirement)(\/|$)/.test(rel); // customer files are not template documents
  const all = [];
  (function walk(dir) {
    for (const name of readdirSync(dir)) {
      const p = join(dir, name), rel = relative(ROOT, p);
      if (skip(rel)) continue;
      if (statSync(p).isDirectory()) walk(p); else all.push(rel);
    }
  })(ROOT);
  const md = all.filter((f) => f.endsWith(".md"));
  const alias = { "general/": "01_input/00_general/", "project/": "01_input/01_project/", "skills/": ".claude/skills/", "docs/": "02_output/docs/", "out/": "02_output/" };
  const agentMade = /(manifest\.sha256|02_adr\/|02_contracts\/|logs\/|scripts\/|changes\/|US-nnn|CR-nn|06_hidden-evaluation\.md|wireframe\.css|WF-nn)/;
  const defined = {};
  for (const f of md) {
    const text = read(join(ROOT, f));
    const lines = text.split("\n");
    if (lines.length > 200) errors.push(`${f}: ${lines.length} lines (max 200)`);
    if (!/^> For: /m.test(text)) errors.push(`${f}: no "> For:" header line`);
    for (const m of text.matchAll(/`((?:general|project|skills|docs|out)\/[A-Za-z0-9_./-]+)`/g)) {
      const ref = m[1];
      if (agentMade.test(ref) || ref.includes("*") || ref.includes("<")) continue;
      const key = Object.keys(alias).find((k) => ref.startsWith(k));
      if (!existsSync(join(ROOT, alias[key] + ref.slice(key.length)))) errors.push(`${f}: reference ${ref} does not exist`);
    }
    if (f.startsWith("01_input/00_general/")) {
      for (const m of text.matchAll(/^\| ((?:UI|ES|SB|DoR-[SR]|DoD-[SIR])-\d+) \|/gm)) (defined[m[1]] ||= []).push(f);
    }
    if (f.startsWith(".claude/skills/")) {
      const reads = (text.match(/^- Reads: .*$/m) || [""])[0];
      if (/(^|[\s`])(README\.md|Run_Guide\.md|system\/)/.test(reads)) errors.push(`${f}: "Reads" names a human or system file`);
    }
  }
  for (const [id, files] of Object.entries(defined)) if (files.length > 1) errors.push(`${id} is defined ${files.length} times (${files.join(", ")})`);
  const cfg = runConfig();
  for (const dir of readdirSync(join(ROOT, ".claude/skills"))) {
    if (["sdd-build-ui", "sdd-grill-me", "sdd-run"].includes(dir)) continue; // not started by the runner
    if (!cfg.sessions[`/${dir}`]) errors.push(`skill /${dir} has no row in run-config.md ("Sessions")`);
  }
  return errors.length ? { ok: false, error: `${errors.length} problem(s): ` + errors.join(" | ") } : { ok: true, note: `template ok: ${md.length} Markdown files checked` };
}

// ---------- main ----------

function main() {
  const argv = process.argv.slice(2);
  const has = (f) => argv.includes(f);
  const ci = argv.indexOf("--command");
  const override = ci >= 0 ? argv[ci + 1] : "";

  for (const [flag, fn] of [["--vault", vault], ["--hidden-eval", hiddenEval], ["--restore-hidden", restoreHidden], ["--summary", summary], ["--lint-template", lintTemplate], ["--env-check", envCheck], ["--leak-check", leakCheck], ["--jobs", runJobs]]) {
    if (has(flag)) { const r = fn(); report(r.ok ? "done" : "failed", { step: flag, note: r.note, error: r.error }); }
  }

  const before = progress();
  const jobsWaiting = /runner jobs/i.test(before.waiting);
  if (!override && (/complete/i.test(before.result) || /^(none|-|)$/i.test(before.command))) report("complete");
  if (!override && !jobsWaiting && before.waiting && !/^nothing$/i.test(before.waiting)) {
    report("waiting", { decisions: decisionsWaiting(before.waiting) });
  }
  const command = override || before.command;
  const skill = command.split(/\s+/)[0];
  const gateProblem = has("--force") ? "" : missingGate(command, before.gates);
  if (gateProblem) report("refused", { command, error: `${skill} cannot start: ${gateProblem}`, gates: before.gates });
  const designAsk = !/^-\s*Design round:\s*skip/im.test(read(RUN_CONFIG));
  if (INTERACTIVE.has(skill) && (skill !== "/sdd-design" || designAsk)) report("interactive", { command });
  if (!has("--once")) report("ready", { command, jobs: jobsWaiting || undefined });

  // Deterministic steps around the sessions: long jobs first, and the hidden suite never stays in the project.
  let jobsNote;
  if (jobsWaiting) { const r = runJobs(); if (!r.ok) report("failed", { step: "runner jobs", error: r.error }); jobsNote = r.note; }
  if (skill === "/sdd-verify") { const r = hiddenEval(); if (!r.ok) report("failed", { step: "--hidden-eval", error: r.error }); }
  if (skill === "/sdd-release") restoreHidden();
  // A change request adds hidden cases to the existing suite, so it needs the suite back first.
  if (skill === "/sdd-acceptance-tests" && !existsSync(HIDDEN_DIR) && existsSync(runConfig().vault)) restoreHidden();

  let outcome;
  for (let attempt = 1; attempt <= 2; attempt++) {
    // A retry tells the session to continue where the failed one stopped (AGENTS.md, "Resume").
    outcome = runSession(attempt === 1 ? command : `${command} resume`);
    // A usage limit is not a failure of the session: retrying now would hit it again.
    if (outcome.limit) report("paused", { command, error: outcome.limit, log: outcome.logFile, jobs: jobsNote });
    const after = progress();
    if (outcome.ok && after.lastBlock !== before.lastBlock) {
      if (skill === "/sdd-acceptance-tests") { const v = vault(); if (!v.ok) report("failed", { step: "--vault", error: v.error, log: outcome.logFile }); }
      if (/complete/i.test(after.result)) report("complete", { log: outcome.logFile });
      if (/runner jobs/i.test(after.waiting)) report("done", { log: outcome.logFile, note: "runner jobs requested; the next --once runs them, then the next session" });
      if (after.waiting && !/^nothing$/i.test(after.waiting)) report("waiting", { log: outcome.logFile, decisions: decisionsWaiting(after.waiting) });
      if (/failed/i.test(after.result)) report("failed", { log: outcome.logFile, attempt });
      report("done", { log: outcome.logFile, jobs: jobsNote });
    }
  }
  report("failed", { log: outcome?.logFile, error: outcome?.error || "the session ended without updating 00_progress.md", attempts: 2 });
}

main();
