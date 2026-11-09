# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S1 preflight D-12: 2 (vitest 4.1.11, Stryker 10.0.0), D-13: 1 (Tomcat 10.1.59, Jackson 2.21.7)
- Result: done
- Gate: phase 0 passed
- Notes for the next session: Final versions differ from `tech-stack.md` per `decisions-log.md` (D-04 to D-09, D-12, D-13). Product poms must set `tomcat.version` 10.1.59 and `jackson-bom.version` 2.21.7 (Boot 3.5.16). Two Critical in spring-webmvc 6.2.19 (GHSA-j9f9-w8pj-32f8, GHSA-pc63-qcmh-9cmg) are accepted as not reachable (D-13): no SSE, no `XsltView`; `/verify` re-checks. firebase High recorded as Medium (D-08), `/verify` re-checks the bundle. Maven runs in a container (D-10); OWASP scan did not run (NVD), OSV is the Maven scan. Input manifest written (73 files).

## Next
- Command: /clarify
- Waiting for: nothing (D-04 non-blocking, pending review)

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
| US-001 to US-019 | 1 (MVP) | ready | |
| US-020 to US-022 | 2 | ready | |
