// Runs the hidden suite (general/testing/hidden-evaluation.md) and writes one JUnit report.
//
// Part 1 (backend, API): the Java cases in src/test/java are copied into the backend's test folder of
//   the project copy they run in, run with Maven in the pinned image (backend/README.md; Docker runs the
//   PostgreSQL of the tests), and removed again. They use the harness of the visible acceptance tests.
// Part 2 (browser): the Playwright cases in ui/ run against the local stack (docker compose). When no
//   stack answers at PLAYWRIGHT_BASE_URL (default http://localhost:5173) the script starts the stack of
//   this project copy and stops it again afterwards.
//
// usage (from the project root): node 02_output/hidden-tests/run-hidden.mjs
import { spawnSync } from "node:child_process";
import {
  cpSync,
  existsSync,
  mkdirSync,
  readFileSync,
  readdirSync,
  rmSync,
  writeFileSync,
} from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const out = resolve(here, "..");
const backend = join(out, "backend");
const frontend = join(out, "frontend");
const report = join(here, "report");
const copied = join(
  backend,
  "src",
  "test",
  "java",
  "app",
  "contactgroups",
  "hidden",
);
const stackUrl = process.env.PLAYWRIGHT_BASE_URL ?? "http://localhost:5173";
const suites = [];

mkdirSync(report, { recursive: true });

// ---- part 1: backend
cpSync(
  join(here, "src", "test", "java", "app", "contactgroups", "hidden"),
  copied,
  { recursive: true },
);
rmSync(join(backend, "target", "failsafe-reports"), {
  recursive: true,
  force: true,
});
const maven = spawnSync(
  "docker",
  [
    "run",
    "--rm",
    "-v",
    `${backend}:/w`,
    "-v",
    "m2cache:/root/.m2",
    "-v",
    "/var/run/docker.sock:/var/run/docker.sock",
    "-e",
    "TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal",
    "-w",
    "/w",
    "maven:3.9.11-eclipse-temurin-21",
    "mvn",
    "-B",
    "-ntp",
    "verify",
    "-Dtest=NoSuchTest",
    "-Dsurefire.failIfNoSpecifiedTests=false",
    "-Dit.test=app.contactgroups.hidden.*IT",
  ],
  { stdio: ["ignore", "pipe", "pipe"], maxBuffer: 1 << 30 },
);
writeFileSync(join(report, "maven.log"), `${maven.stdout}\n${maven.stderr}`);
rmSync(copied, { recursive: true, force: true });
const reports = join(backend, "target", "failsafe-reports");
if (existsSync(reports)) {
  for (const name of readdirSync(reports)) {
    if (/^TEST-.*hidden.*\.xml$/.test(name)) {
      suites.push(
        readFileSync(join(reports, name), "utf8").replace(/<\?xml[^>]*\?>/, ""),
      );
    }
  }
}

// ---- part 2: browser
async function reachable() {
  try {
    return (await fetch(stackUrl)).ok;
  } catch {
    return false;
  }
}
const compose = [
  "compose",
  "-f",
  join(out, "docker-compose.yml"),
  "--env-file",
  join(out, "..", ".env"),
];
const startedHere = !(await reachable());
if (startedHere)
  spawnSync("docker", [...compose, "up", "--build", "-d", "--wait"], {
    stdio: "ignore",
  });
const playwright = spawnSync(
  process.platform === "win32" ? "npx.cmd" : "npx",
  ["playwright", "test", "-c", join(here, "ui", "playwright.config.mjs")],
  {
    cwd: frontend,
    env: { ...process.env, PLAYWRIGHT_BASE_URL: stackUrl },
    stdio: "pipe",
    shell: process.platform === "win32",
    maxBuffer: 1 << 30,
  },
);
writeFileSync(
  join(report, "playwright.log"),
  `${playwright.stdout}\n${playwright.stderr}`,
);
const ui = join(here, "ui", "results", "junit-ui.xml");
if (existsSync(ui))
  suites.push(
    readFileSync(ui, "utf8")
      .replace(/<\?xml[^>]*\?>/, "")
      .replace(/<\/?testsuites[^>]*>/g, ""),
  );
if (startedHere) spawnSync("docker", [...compose, "down"], { stdio: "ignore" });

writeFileSync(
  join(report, "junit.xml"),
  `<?xml version="1.0" encoding="UTF-8"?>\n<testsuites>\n${suites.join("\n")}\n</testsuites>\n`,
);
process.exit(0);
