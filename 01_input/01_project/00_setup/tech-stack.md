# Tech stack

> For: agent (reads), human (reviews) · Owner: Architect · Agent: read-only

The single source of truth for every platform, dependency and tool version.
Build manifests and lock files in `02_output/` (e.g. `pom.xml`, `package.json`, `build.gradle`, `*.csproj`, `Podfile`) must match this file exactly; the phase 0 gate checks it. Resolvability and vulnerability results are recorded by the agent in `docs/00_preflight-report.md`.

## Rules

- Exact versions only: no ranges, wildcards, `latest` or floating tags.
- Changing any entry below is a blocking decision (`general/rules/decisions.md`). The human's approval in the decision record replaces the entry; nobody edits this file mid-run.
- A pin that resolves and runs but whose tool reports another version (for example an image tag) is a non-blocking decision; the listed pin stays authoritative. A pin that does not resolve or run is blocking.
- A dependency or tool not listed here may be added only with an exact version and a non-blocking decision record; it is scanned in phase 6 like any other.
- Every dependency's licence must be compatible with the project's licence.
- A `platforms` or `tooling` entry may carry `version_check` (a command whose output contains the version) and, for `tooling`, `options` (flags, analysers or rule sets to enable or disable, for example analysers that need credentials not listed in `secrets.env.example`). Preflight uses them.
- `tooling` must include at least one tool for each purpose required by `general/quality/` and `general/security/`: build, format, lint, type check (if the language has one), test, coverage, mutation, static analysis, dependency scan, secret scan; and for a component with a user interface: the UI stack of `general/design/ui-baseline.md` (UI-01), a browser test runner with screenshots, and an accessibility checker.
- A version proposed by `/to-spec` and not yet verified is marked `[ASSUMPTION] verify in preflight`; `/preflight` confirms or replaces it through a decision.

Project licence: proprietary (all rights with the customer, Q-16c). Allowed dependency licences: Apache-2.0, MIT, BSD-2-Clause, BSD-3-Clause, ISC, 0BSD, CC0-1.0, PostgreSQL. Strong copyleft (GPL, AGPL) is forbidden in the product. Listed exceptions: Logback under its EPL option (a dependency of the Spring Boot logging starter); EPL and MPL only in test or build scope (JUnit, axe-core); the Java runtime (GPL with classpath exception) only as the container base image. No Hibernate and no JPA: the database layer uses Spring JDBC (JdbcTemplate) with parameterised queries (Q-16c).

Versions verified to resolve on: not yet verified. [ASSUMPTION] verify in preflight — every version below is the latest stable the author knew and is unverified; the author's knowledge of releases may be out of date. The backend line uses Spring Boot 3.x as the customer's developer asked (Q-16a); a newer major line exists and is not chosen here.

## Platforms

```yaml
platforms:
  - { id: java, name: "Java (Eclipse Temurin JDK, LTS)", version: "21.0.8", source: "https://adoptium.net", version_check: "java -version" }  # [ASSUMPTION] verify in preflight
  - { id: node, name: "Node.js (LTS)", version: "22.20.0", source: "https://nodejs.org", version_check: "node --version" }  # [ASSUMPTION] verify in preflight
  - { id: postgres, name: "PostgreSQL", version: "17.6", source: "https://www.postgresql.org", version_check: "postgres --version" }  # [ASSUMPTION] verify in preflight; image postgres:17.6
  - { id: docker, name: "Docker Engine with Compose", version: "28.4.0", source: "https://www.docker.com", version_check: "docker --version" }  # [ASSUMPTION] verify in preflight; host tool, not installed by the agent
```

## Dependencies

One entry per artifact, written the way the ecosystem writes coordinates.

```yaml
dependencies:
  # backend, runtime (Maven Central: https://repo.maven.apache.org/maven2)
  - { id: "org.springframework.boot:spring-boot-starter-web", ecosystem: maven, version: "3.5.6", scope: runtime, component: backend, source: maven-central, purpose: "REST API", license: Apache-2.0 }   # [ASSUMPTION] verify in preflight
  - { id: "org.springframework.boot:spring-boot-starter-jdbc", ecosystem: maven, version: "3.5.6", scope: runtime, component: backend, source: maven-central, purpose: "JdbcTemplate and HikariCP", license: Apache-2.0 }   # [ASSUMPTION] verify in preflight
  - { id: "org.springframework.boot:spring-boot-starter-validation", ecosystem: maven, version: "3.5.6", scope: runtime, component: backend, source: maven-central, purpose: "input validation", license: Apache-2.0 }   # [ASSUMPTION] verify in preflight
  - { id: "org.springframework.boot:spring-boot-starter-security", ecosystem: maven, version: "3.5.6", scope: runtime, component: backend, source: maven-central, purpose: "bearer-token resource server, CORS, security headers", license: Apache-2.0 }   # [ASSUMPTION] verify in preflight
  - { id: "org.springframework.boot:spring-boot-starter-oauth2-resource-server", ecosystem: maven, version: "3.5.6", scope: runtime, component: backend, source: maven-central, purpose: "verify Firebase ID tokens (JWT) against Google's public keys", license: Apache-2.0 }   # [ASSUMPTION] verify in preflight
  - { id: "org.springframework.boot:spring-boot-starter-actuator", ecosystem: maven, version: "3.5.6", scope: runtime, component: backend, source: maven-central, purpose: "health check", license: Apache-2.0 }   # [ASSUMPTION] verify in preflight
  - { id: "org.springframework.boot:spring-boot-starter-logging", ecosystem: maven, version: "3.5.6", scope: runtime, component: backend, source: maven-central, purpose: "logging; Logback used under its EPL option (listed exception)", license: Apache-2.0 }   # [ASSUMPTION] verify in preflight
  - { id: "org.postgresql:postgresql", ecosystem: maven, version: "42.7.8", scope: runtime, component: backend, source: maven-central, purpose: "JDBC driver", license: BSD-2-Clause }   # [ASSUMPTION] verify in preflight
  - { id: "org.flywaydb:flyway-core", ecosystem: maven, version: "11.13.1", scope: runtime, component: backend, source: maven-central, purpose: "database migrations", license: Apache-2.0 }   # [ASSUMPTION] verify in preflight
  - { id: "org.flywaydb:flyway-database-postgresql", ecosystem: maven, version: "11.13.1", scope: runtime, component: backend, source: maven-central, purpose: "Flyway PostgreSQL support", license: Apache-2.0 }   # [ASSUMPTION] verify in preflight
  - { id: "org.apache.poi:poi-ooxml", ecosystem: maven, version: "5.4.1", scope: runtime, component: backend, source: maven-central, purpose: "Excel export (.xlsx)", license: Apache-2.0 }   # [ASSUMPTION] verify in preflight
  - { id: "org.springdoc:springdoc-openapi-starter-webmvc-ui", ecosystem: maven, version: "2.8.13", scope: runtime, component: backend, source: maven-central, purpose: "OpenAPI description", license: Apache-2.0 }   # [ASSUMPTION] verify in preflight
  - { id: "com.bucket4j:bucket4j_jdk17-core", ecosystem: maven, version: "8.14.0", scope: runtime, component: backend, source: maven-central, purpose: "rate limit of 100 requests per minute", license: Apache-2.0 }   # [ASSUMPTION] verify in preflight
  # backend, test
  - { id: "org.springframework.boot:spring-boot-starter-test", ecosystem: maven, version: "3.5.6", scope: test, component: backend, source: maven-central, purpose: "JUnit 5, AssertJ, Mockito, MockMvc", license: "Apache-2.0; JUnit EPL-2.0 (test scope, listed exception)" }   # [ASSUMPTION] verify in preflight
  - { id: "org.testcontainers:postgresql", ecosystem: maven, version: "1.21.3", scope: test, component: backend, source: maven-central, purpose: "real PostgreSQL in tests", license: MIT }   # [ASSUMPTION] verify in preflight
  - { id: "org.testcontainers:junit-jupiter", ecosystem: maven, version: "1.21.3", scope: test, component: backend, source: maven-central, purpose: "Testcontainers for JUnit 5", license: MIT }   # [ASSUMPTION] verify in preflight
  # frontend, runtime (npm: https://registry.npmjs.org)
  - { id: react, ecosystem: npm, version: "19.1.1", scope: runtime, component: frontend, source: npm, purpose: "UI library", license: MIT }   # [ASSUMPTION] verify in preflight
  - { id: react-dom, ecosystem: npm, version: "19.1.1", scope: runtime, component: frontend, source: npm, purpose: "UI rendering", license: MIT }   # [ASSUMPTION] verify in preflight
  - { id: react-router, ecosystem: npm, version: "7.9.3", scope: runtime, component: frontend, source: npm, purpose: "routing", license: MIT }   # [ASSUMPTION] verify in preflight
  - { id: "@tanstack/react-query", ecosystem: npm, version: "5.90.2", scope: runtime, component: frontend, source: npm, purpose: "server state", license: MIT }   # [ASSUMPTION] verify in preflight
  - { id: firebase, ecosystem: npm, version: "12.3.0", scope: runtime, component: frontend, source: npm, purpose: "Firebase Authentication client (the only Firebase service used)", license: Apache-2.0 }   # [ASSUMPTION] verify in preflight
  - { id: i18next, ecosystem: npm, version: "25.5.2", scope: runtime, component: frontend, source: npm, purpose: "Slovenian and English language files", license: MIT }   # [ASSUMPTION] verify in preflight
  - { id: react-i18next, ecosystem: npm, version: "16.0.0", scope: runtime, component: frontend, source: npm, purpose: "i18n bindings", license: MIT }   # [ASSUMPTION] verify in preflight
  - { id: tailwindcss, ecosystem: npm, version: "4.1.13", scope: runtime, component: frontend, source: npm, purpose: "UI-01 token-based styling", license: MIT }   # [ASSUMPTION] verify in preflight
  - { id: "@tailwindcss/vite", ecosystem: npm, version: "4.1.13", scope: build, component: frontend, source: npm, purpose: "Tailwind plugin for Vite", license: MIT }   # [ASSUMPTION] verify in preflight
  - { id: "radix-ui", ecosystem: npm, version: "1.4.3", scope: runtime, component: frontend, source: npm, purpose: "UI-01 accessible primitives under shadcn/ui", license: MIT }   # [ASSUMPTION] verify in preflight
  - { id: lucide-react, ecosystem: npm, version: "0.544.0", scope: runtime, component: frontend, source: npm, purpose: "UI-01 icons", license: ISC }   # [ASSUMPTION] verify in preflight
  - { id: class-variance-authority, ecosystem: npm, version: "0.7.1", scope: runtime, component: frontend, source: npm, purpose: "shadcn/ui variants", license: Apache-2.0 }   # [ASSUMPTION] verify in preflight
  - { id: clsx, ecosystem: npm, version: "2.1.1", scope: runtime, component: frontend, source: npm, purpose: "shadcn/ui class names", license: MIT }   # [ASSUMPTION] verify in preflight
  - { id: tailwind-merge, ecosystem: npm, version: "3.3.1", scope: runtime, component: frontend, source: npm, purpose: "shadcn/ui class merging", license: MIT }   # [ASSUMPTION] verify in preflight
  # frontend, test and build
  - { id: vitest, ecosystem: npm, version: "3.2.4", scope: test, component: frontend, source: npm, purpose: "unit and component tests", license: MIT }   # [ASSUMPTION] verify in preflight
  - { id: "@testing-library/react", ecosystem: npm, version: "16.3.0", scope: test, component: frontend, source: npm, purpose: "component tests", license: MIT }   # [ASSUMPTION] verify in preflight
  - { id: "@vitest/coverage-v8", ecosystem: npm, version: "3.2.4", scope: test, component: frontend, source: npm, purpose: "coverage", license: MIT }   # [ASSUMPTION] verify in preflight
  - { id: "@playwright/test", ecosystem: npm, version: "1.55.1", scope: test, component: frontend, source: npm, purpose: "browser tests with screenshots; end-to-end tests", license: Apache-2.0 }   # [ASSUMPTION] verify in preflight
  - { id: "@axe-core/playwright", ecosystem: npm, version: "4.10.2", scope: test, component: frontend, source: npm, purpose: "accessibility scan (axe-core is MPL-2.0, test scope, listed exception)", license: MPL-2.0 }   # [ASSUMPTION] verify in preflight
  - { id: vite, ecosystem: npm, version: "7.1.7", scope: build, component: frontend, source: npm, purpose: "build tool", license: MIT }   # [ASSUMPTION] verify in preflight
  - { id: "@vitejs/plugin-react", ecosystem: npm, version: "5.0.4", scope: build, component: frontend, source: npm, purpose: "React in Vite", license: MIT }   # [ASSUMPTION] verify in preflight
  - { id: typescript, ecosystem: npm, version: "5.9.2", scope: build, component: frontend, source: npm, purpose: "type checking", license: Apache-2.0 }   # [ASSUMPTION] verify in preflight
  # test infrastructure
  - { id: firebase-tools, ecosystem: npm, version: "14.19.1", scope: test, component: all, source: npm, purpose: "Firebase Authentication Emulator for tests and CI", license: MIT }   # [ASSUMPTION] verify in preflight
  # container images
  - { id: "eclipse-temurin", ecosystem: container, version: "21.0.8_9-jre", scope: runtime, component: backend, source: docker-hub, purpose: "backend base image (Java runtime GPL with classpath exception, listed exception)", license: "GPL-2.0 with Classpath Exception" }   # [ASSUMPTION] verify in preflight
  - { id: "postgres", ecosystem: container, version: "17.6", scope: runtime, component: all, source: docker-hub, purpose: "database image", license: PostgreSQL }   # [ASSUMPTION] verify in preflight
  - { id: "nginx", ecosystem: container, version: "1.29.1", scope: runtime, component: frontend, source: docker-hub, purpose: "serves the built web app and can act as the HTTPS front proxy", license: BSD-2-Clause }   # [ASSUMPTION] verify in preflight
```

## Tooling

```yaml
tooling:
  # backend
  - { id: "maven", ecosystem: maven, version: "3.9.11", purpose: build, component: backend, source: maven-central }   # [ASSUMPTION] verify in preflight; use the Maven wrapper, version_check: "./mvnw -v"
  - { id: "com.diffplug.spotless:spotless-maven-plugin", ecosystem: maven, version: "2.46.1", purpose: format, component: backend, source: maven-central }   # [ASSUMPTION] verify in preflight
  - { id: "org.apache.maven.plugins:maven-pmd-plugin", ecosystem: maven, version: "3.27.0", purpose: lint, component: backend, source: maven-central, options: "PMD with the quickstart rule set" }   # [ASSUMPTION] verify in preflight; also static-analysis; its CPD gives duplication
  - { id: "org.apache.maven.plugins:maven-surefire-plugin", ecosystem: maven, version: "3.5.4", purpose: test, component: backend, source: maven-central }   # [ASSUMPTION] verify in preflight
  - { id: "org.apache.maven.plugins:maven-failsafe-plugin", ecosystem: maven, version: "3.5.4", purpose: test, component: backend, source: maven-central }   # [ASSUMPTION] verify in preflight; integration tests with Testcontainers
  - { id: "org.jacoco:jacoco-maven-plugin", ecosystem: maven, version: "0.8.13", purpose: coverage, component: backend, source: maven-central }   # [ASSUMPTION] verify in preflight
  - { id: "org.pitest:pitest-maven", ecosystem: maven, version: "1.20.4", purpose: mutation, component: backend, source: maven-central }   # [ASSUMPTION] verify in preflight
  - { id: "org.owasp:dependency-check-maven", ecosystem: maven, version: "12.1.6", purpose: dependency-scan, component: backend, source: maven-central, options: "needs no API key for a first run; an NVD API key is optional and must be listed in secrets.env.example if used" }   # [ASSUMPTION] verify in preflight
  # frontend
  - { id: "eslint", ecosystem: npm, version: "9.36.0", purpose: lint, component: frontend, source: npm }   # [ASSUMPTION] verify in preflight; also static-analysis with typescript-eslint and eslint-plugin-jsx-a11y
  - { id: "typescript-eslint", ecosystem: npm, version: "8.44.1", purpose: static-analysis, component: frontend, source: npm }   # [ASSUMPTION] verify in preflight
  - { id: "eslint-plugin-jsx-a11y", ecosystem: npm, version: "6.10.2", purpose: static-analysis, component: frontend, source: npm }   # [ASSUMPTION] verify in preflight
  - { id: "prettier", ecosystem: npm, version: "3.6.2", purpose: format, component: frontend, source: npm }   # [ASSUMPTION] verify in preflight
  - { id: "typescript", ecosystem: npm, version: "5.9.2", purpose: type-check, component: frontend, source: npm }   # [ASSUMPTION] verify in preflight; run as tsc --noEmit
  - { id: "vite", ecosystem: npm, version: "7.1.7", purpose: build, component: frontend, source: npm }   # [ASSUMPTION] verify in preflight
  - { id: "vitest", ecosystem: npm, version: "3.2.4", purpose: test, component: frontend, source: npm }   # [ASSUMPTION] verify in preflight
  - { id: "@vitest/coverage-v8", ecosystem: npm, version: "3.2.4", purpose: coverage, component: frontend, source: npm }   # [ASSUMPTION] verify in preflight
  - { id: "@stryker-mutator/core", ecosystem: npm, version: "9.1.1", purpose: mutation, component: frontend, source: npm, options: "with @stryker-mutator/vitest-runner 9.1.1" }   # [ASSUMPTION] verify in preflight
  - { id: "@playwright/test", ecosystem: npm, version: "1.55.1", purpose: test, component: frontend, source: npm, options: "browser test runner with screenshots; Chromium, Firefox and WebKit" }   # [ASSUMPTION] verify in preflight
  - { id: "@axe-core/playwright", ecosystem: npm, version: "4.10.2", purpose: static-analysis, component: frontend, source: npm, options: "accessibility checker; WCAG 2.1 AA tags" }   # [ASSUMPTION] verify in preflight
  - { id: "npm", ecosystem: npm, version: "10.9.3", purpose: dependency-scan, component: frontend, source: npm, options: "npm audit --omit=dev; also used as package manager" }   # [ASSUMPTION] verify in preflight
  - { id: "jscpd", ecosystem: npm, version: "4.0.5", purpose: duplication, component: frontend, source: npm }   # [ASSUMPTION] verify in preflight
  # all
  - { id: "gitleaks", ecosystem: container, version: "8.28.0", purpose: secret-scan, component: all, source: "ghcr.io/gitleaks/gitleaks" }   # [ASSUMPTION] verify in preflight; run as a container so nothing is installed on the host
  - { id: "firebase-tools", ecosystem: npm, version: "14.19.1", purpose: test, component: all, source: npm, options: "Firebase Authentication Emulator only; no deploy and no other Firebase service" }   # [ASSUMPTION] verify in preflight
```
