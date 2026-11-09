# Quality requirements

> For: agent (reads), human (reviews) · Owner: QA lead · Agent: read-only

Adds to `general/quality/` (definition of done, test strategy, severity scale); do not repeat those here. Project done criteria (`DoD-P`) are in `project/01_requirements/intent.md`. Every number below is a developer proposal (Q-15 to Q-15c, Q-23c) the customer has not confirmed.

## Non-functional requirements

| ID | Category (performance, availability, accessibility, localisation, usability, …) | Requirement | How it is checked |
|---|---|---|---|
| NFR-01 | Performance | The list, search and filter (US-009, US-010) answer in under 1 second (95th percentile [ASSUMPTION]) for a group of up to 10,000 contacts, with 20 users working at the same time. | Automated timing test on the deployed system with 10,000 seeded contacts and 20 parallel virtual users; the result is recorded in the verification report. |
| NFR-02 | Performance | Opening a contact (US-005) takes under 500 ms (95th percentile). | Same timing test on the deployed system. |
| NFR-03 | Performance | An export of 1,000 contacts (US-011) is ready in under 10 seconds. | Timing test on the deployed system with 1,000 contacts with tags and attributes. |
| NFR-04 | Capacity | The whole system serves up to 100 registered users, 20 at the same time, and groups of up to 10,000 contacts. | The load of NFR-01 plus a data set of 100 users and several groups; no error responses. |
| NFR-05 | Availability | The system aims at 99 % availability per month, with no on-call duty and planned maintenance announced; it offers the health check (US-017 R-017-12); a restart loses no data. | Restart test: write data, restart the containers, read the data back; health check responds 200 afterwards. The 99 % target is documented in the install guide as the customer's operating target (OQ-P-11). |
| NFR-06 | Backup and recovery | The database is backed up daily, backups are kept 14 days, and a restore is tested once before release; the install guide says what to back up and how to restore. | Documented procedure, a scripted restore into a clean Compose setup and a data comparison; log in `out/logs/`. |
| NFR-07 | Compatibility | The web app works in the current and previous major version of Chrome, Edge, Firefox and Safari on desktop. | Playwright runs the main flows in Chromium, Firefox and WebKit (engines of the four browsers); manual Safari and Edge check once before delivery. |
| NFR-08 | Responsiveness | The layout works from 320 px width without horizontal scrolling for viewing, searching and filtering; editing on a phone is allowed but not tested in depth. | Playwright screenshots at 320, 768 and 1280 px for list, filter, contact screens; assertion of no horizontal scroll. |
| NFR-09 | Accessibility | The main flows (sign-in, list, create or edit a contact, export) meet WCAG 2.1 level AA, can be used with the keyboard and show a visible focus. | Automated accessibility scan on each main screen in both themes and languages; keyboard-only Playwright run of the main flows. |
| NFR-10 | Localisation | Every UI text exists in Slovenian and English language files; no text is in a component; formats follow `business-rules.md`. | A check that both language files have the same keys; a test that switches the language on each screen of the main flows (AC-018-01 to AC-018-07). |
| NFR-11 | Usability | A new user needs no manual; every screen has a clear empty state and every error a clear message. | Screenshot review of the empty and error states of every screen against the wireframes; DoD-P02. |
| NFR-12 | Portability and configuration | The product runs from container images and a Docker Compose setup on any host; all configuration (database address and credentials, Firebase project ID and web app key, allowed origin) comes from environment variables; nothing is tied to one provider. | Start the delivered Compose setup on a clean host with only the `.env`; E2E-01 passes. |
| NFR-13 | Maintainability | Database changes are Flyway migrations, versioned, run at start-up, forward only; the repository holds no hand-edited schema. | The backend starts on an empty database and on the previous release's database; the migration list is checked. |
| NFR-14 | Legal | Every dependency has an allowed licence (`tech-stack.md`); there is no Hibernate or JPA; no code of SCM is reused; no Google service other than Firebase Authentication is used. | Licence scan in phase 6; dependency list review; search for forbidden imports. |
| NFR-15 | Capacity of export | An export of 10,000 contacts completes without error and without running out of memory; no time target is set yet. | Test with 10,000 contacts; time is recorded only. |

## Thresholds

These thresholds are the **quality gate** of the project, per component, over the scope and with the tests defined in `general/testing/measurement.md`. The agent works until each component meets them, then stops adding tests for that component (rules and stop rule: "Quality gate" in `general/testing/measurement.md`). Keep the format `>= 95 %`. The interview gives no other values, so the defaults apply.

| Measure | Threshold |
|---|---|
| Line coverage (all levels together) | >= 95 % |
| Mutation score (adjusted) | >= 90 % |
| Branch coverage | record only |

## Mutation scope

The paths or packages whose code is mutated (default: all production code of the component). Selection rule: validation, security, persistence and business-rule code. Rules: `general/testing/measurement.md`.

| Component | Paths or packages | Exclusions and reason |
|---|---|---|
| backend | all production code under `app.contactgroups` | None. |
| frontend | all production code under `src/` | None. Presentation (styles, tokens, language files) holds no logic and is not mutated. |

## Overrides

None.
