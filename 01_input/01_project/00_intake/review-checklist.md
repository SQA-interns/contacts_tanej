# Review checklist

> For: agent (reads), human (reviews) · Owner: Developer · Written by: `/to-spec` · Agent: writes in intake only

Everything the developer must confirm or answer before the run. Edit the project file named in each row, then mark the row done. `Block` topics (rights, personal data, deletion, destructive actions, legal) stay `OPEN — needs customer` until the customer confirms them; the rules are written as the developer gave them.

| Done | File | ID | Kind (assumption, open, developer answer) | Question | Proposed answer |
|---|---|---|---|---|---|
| [ ] | business-rules.md | Project-wide facts: Time zone | assumption | Is Europe/Ljubljana the customer's time zone? (Q-26, Q-11b: the customer's countries are unknown.) | Yes, `Europe/Ljubljana`. |
| [ ] | business-rules.md | Project-wide facts: Number format | assumption | English UI uses a decimal point? | Yes. |
| [ ] | business-rules.md | Project-wide facts: country list | assumption | Which standard country list? (Q-08b) | ISO 3166-1, names per UI language. |
| [ ] | business-rules.md | BR-04 | assumption | Cleanup of Trash runs at least once a day. | Yes; purge at the first run after 30 days. |
| [ ] | business-rules.md | Data model: Group name | assumption | Group name 1 to 100 characters, not unique. | Yes. |
| [ ] | business-rules.md | Data model: Contact phone | assumption | Phone: digits, spaces and `+ ( ) - . /`, at most 30 characters. (Q-08b: "only a character and length check") | Yes. |
| [ ] | intent.md | MVP | assumption | Version recording (US-019) is in the MVP though F10 is second increment, so that versions exist from the first save. | Yes; only the screens (US-020) come later. |
| [ ] | US-001 | R-001-02, OQ-001-01 | assumption | Name asked at registration, 1 to 100 characters. | Yes. |
| [ ] | US-001 | OQ-001-02 | open | Firebase Authentication processes e-mail addresses at Google, possibly outside the EU: acceptable? (Q-33) | Needs customer. |
| [ ] | US-002 | R-002-02, R-002-03 | assumption | Group name limit 100; two groups may share a name. | Yes. |
| [ ] | US-002 | OQ-002-01 | open (Block: rights) | Groups are shared; the creator is the Owner (Q-02b, Q-05). | Confirm. |
| [ ] | US-002 | OQ-002-02 | open | Renaming a group is excluded. | Not in the MVP. |
| [ ] | US-003 | R-003-01, OQ-003-02 | assumption | An invitation may grant any of the three roles, including Owner. | Yes. |
| [ ] | US-003 | R-003-08 | assumption | Owners see the list of pending invitations (needed to cancel). | Yes. |
| [ ] | US-003 | OQ-003-01 | open (Block: rights) | Invitation by e-mail, no accept step, 14 days, 20 pending, no e-mail sent (Q-02c, Q-20a, Q-30a). | Confirm. |
| [ ] | US-004 | R-004-01, OQ-004-02 | assumption | Only Owners see the member list. | Yes. |
| [ ] | US-004 | OQ-004-01 | open (Block: rights) | Three roles, several Owners, last-Owner rule, no system administrator (Q-06, Q-06a, Q-06b). | Confirm. |
| [ ] | US-005 | OQ-005-01 | open | Three fields beyond the brief (phone, job title, notes), the one-of-four rule, e-mail not unique (Q-08, Q-08a, Q-08c). | Confirm. |
| [ ] | US-005 | OQ-005-02 | open | Which country list? | ISO 3166-1. |
| [ ] | US-005 | OQ-005-03 | open (Block: personal data) | The notes field may hold special categories of data (Q-33). | Needs customer. |
| [ ] | US-006 | Definitions, R-006-07, R-006-10, OQ-006-01 | assumption | Tag letters are Unicode letters; a tag with no contacts disappears; suggestions while typing. | Yes. |
| [ ] | US-007 | R-007-01, Definitions, OQ-007-01 | assumption | Rename and delete also change contacts in Trash; a "Manage tags" screen exists. | Yes. |
| [ ] | US-008 | Definitions, R-008-03, OQ-008-01 | assumption | Key may use any characters; an empty value is allowed. | Yes. |
| [ ] | US-008 | OQ-008-02 | open | US-008 is "should" but the key-value filter of must feature F6 (US-010) needs it. | Treat US-008 as MVP (already `mvp: true`). |
| [ ] | US-009 | R-009-02, OQ-009-02 | assumption | List columns: last name, first name, e-mail, phone, company, city, country, tags. | Yes. |
| [ ] | US-009 | R-009-03, OQ-009-01 | assumption | Several search words must all match, each in any searched field. | Yes. |
| [ ] | US-009 | R-009-06 | assumption | Empty group shows an empty state with "Add contact" for Owner and Editor. | Yes. |
| [ ] | US-010 | R-010-04, R-010-07, OQ-010-01 | assumption | Key and value compared ignoring case (and accents for the value); company filter is equality ignoring case and accents. Q-23a seven conditions confirmed? | Yes. |
| [ ] | US-011 | R-011-02 | assumption | A change of the search text also clears the selection. | Yes. |
| [ ] | US-011 | R-011-05, R-011-06 | assumption | Attribute columns in alphabetical order; illegal file-name characters become "_". | Yes. |
| [ ] | US-011 | OQ-011-01 | open (Block: personal data) | Only Owner and Editor export; every export is logged (Q-24b, Q-06b). | Confirm. |
| [ ] | US-011 | OQ-011-02 | open | Export of up to 10,000 contacts has no time target. | Record the time only. |
| [ ] | US-012 | R-012-02, OQ-012-02 | assumption | Viewers do not see Trash. | Yes. |
| [ ] | US-012 | OQ-012-01 | open (Block: deletion) | 30 days in Trash (no legal period given); Owner and Editor restore, Owner "Delete forever" (Q-06c, Q-06d). | Confirm. |
| [ ] | US-012 | OQ-012-03 | open | Backups (14 days) still hold erased data (Q-15b). | Needs customer. |
| [ ] | US-013 | Definitions, OQ-013-02 | assumption | Erasure marker text "[erased]". | Yes. |
| [ ] | US-013 | R-013-05 | assumption | A member leaving is recorded as a removal by the member. | Yes. |
| [ ] | US-013 | OQ-013-01 | open (Block: personal data) | History append-only, kept as long as the group, anonymised after erasure (Q-06e, Q-25b). | Confirm. |
| [ ] | US-013 | OQ-013-03 | open | History is `should` but records events of `must` stories. | Build it in the same increment. |
| [ ] | US-014 | R-014-01, OQ-014-01 | assumption | History view page size 25. | Yes. |
| [ ] | US-015 | R-015-02 | assumption | Typed group name is compared case-sensitively after trimming. | Yes. |
| [ ] | US-015 | OQ-015-01 | open (Block: deletion) | Group deletion: Owner only, typed name, 30 days Trash, restore by Owners (Q-06f). | Confirm. |
| [ ] | US-016 | Purpose, OQ-016-02 | assumption | Priority `should`, MVP `true` (the interview gives none; journey J7 needs it). | Yes. |
| [ ] | US-016 | R-016-07 | assumption | Confirmation and recent sign-in are required. | Yes. |
| [ ] | US-016 | OQ-016-01 | open (Block: personal data, legal) | The user's name stays in History after account deletion; legal basis needed (Q-20b). | Needs customer. |
| [ ] | US-017 | R-017-03, R-017-07, R-017-12, R-017-15 | assumption | Statuses and codes: 403 `EMAIL_NOT_VERIFIED`, 400 validation, 200/503 health, 409 `CONFLICT`. | Yes. |
| [ ] | US-017 | R-017-08 | assumption | Rate limit is exactly 100 requests per minute ("about 100"). | Yes. |
| [ ] | US-017 | OQ-017-01 | open | Whether other tools may call the API directly later (Q-02a). | Needs customer. |
| [ ] | US-017 | OQ-017-02 | open | Security details Q-12 to Q-12d are developer proposals. | Confirm. |
| [ ] | US-018 | Purpose, R-018-03 | assumption | Priority `should`, MVP `true`; the language choice is remembered per browser. | Yes. |
| [ ] | US-018 | OQ-018-01 | open | Slovenian and English, Slovenian default, formal "vi" (Q-11, Q-34a). | Confirm. |
| [ ] | US-019 | Purpose, OQ-019-01 | assumption | See intent.md: recording versions in the MVP. | Yes. |
| [ ] | US-020 | R-020-06, R-020-08 | assumption | Restoring a version equal to the current one changes nothing; a stale restore is a conflict. | Yes. |
| [ ] | US-020 | OQ-020-01 | open (Block: personal data) | Versions hold personal data and are erased with the contact (Q-29b). | Confirm. |
| [ ] | US-021 | Definitions | assumption | A duplicate set is the linked group of contacts; phone numbers compare as digits and "+". | Yes. |
| [ ] | US-021 | R-021-11, OQ-021-02 | assumption | Only Owner and Editor find duplicates. | Yes. |
| [ ] | US-021 | OQ-021-01 | open | Duplicate rules (Q-27, Q-27a, Q-28a). | Confirm. |
| [ ] | US-022 | R-022-09, R-022-12, OQ-022-02 | assumption | A merged result over a limit is refused; a stale merge is a conflict. | Yes. |
| [ ] | US-022 | OQ-022-01 | open (Block: destructive) | The merge flow (Q-28). | Confirm. |
| [ ] | US-022 | OQ-022-03 | open | Estimate 8; split if the plan finds it larger. | Planning. |
| [ ] | intent.md | OQ-P-01, OQ-P-02 | open | Success criteria (Q-03), acceptance list (Q-36), SCM only as inspiration (Q-04). | Confirm. |
| [ ] | intent.md | OQ-P-03 | open (Block) | All rights, personal-data, deletion and merge answers are developer proposals. | Confirm. |
| [ ] | intent.md | OQ-P-04 to OQ-P-06 | open | Hosting region, countries, special categories, production Firebase project, host and domain (Q-33, Q-14b, Q-17). | Needs customer. |
| [ ] | intent.md | OQ-P-07 | open | No import from Excel (Q-22a) although customers start from Excel sheets. | Needs customer. |
| [ ] | intent.md | OQ-P-08 | open | Product name, brand, colours, logo, liked and disliked screens (Q-34, Q-34a, Q-34b). | Needs customer. |
| [ ] | intent.md | OQ-P-09 | open (Block: legal) | Licence rules and "no Google service except Firebase Authentication" (Q-16c, Q-16d). | Needs customer. |
| [ ] | intent.md | OQ-P-10, OQ-P-11 | open | Cost of the current way is unknown (Q-01); backup retention versus erasure, who owns 99 % availability (Q-15b). | Needs customer. |
| [ ] | intent.md | Non-goals | developer answer | The 14 out-of-scope items and the principles (Q-32, Q-32a). | Confirm. |
| [ ] | intent.md | DoD-P01 to DoD-P11 | developer answer | Project done criteria (Q-36). | Confirm. |
| [ ] | quality-requirements.md | NFR-01 to NFR-15 | developer answer | The numbers (Q-15a, Q-15b, Q-15c, Q-23c); the 95th percentile and the 15th NFR are additions. | Confirm. |
| [ ] | quality-requirements.md | Thresholds | assumption | The interview gives no coverage or mutation thresholds; the defaults of 95 % and 90 % apply. | Yes. |
| [ ] | security-requirements.md | Verification standard, SR-01 to SR-20 | developer answer | OWASP ASVS 5.0 Level 2 and the security rules (Q-12 to Q-12d). | Confirm. |
| [ ] | tech-stack.md | all versions | assumption | Every version is the latest stable the author knew and is unverified; Spring Boot 3.x as asked (Q-16a). | `/preflight` verifies. |
| [ ] | architecture.md | components: bootstrap | assumption | Root package `app.contactgroups`. | Yes. |
| [ ] | ui-design.md | Overrides UI-08 | developer answer | Desktop first, responsive to 320 px; neutral look, one accent, text as logo (Q-34, Q-34a). | Confirm. |
| [ ] | run-config.md | Allowed commands | assumption | Maven, Docker Compose, Playwright, Vitest, Stryker, ESLint, Prettier, tsc, jscpd, Firebase emulators, npm audit, the gitleaks container; no shell, curl, rm -rf or sudo. | Confirm. |
| [ ] | secrets.env.example | DB_PASSWORD, FIREBASE_WEB_API_KEY | assumption | Only these two are secrets; the other settings are in environments.md. No service-account key is needed to verify tokens. | Yes. |
| [ ] | interview.md | all rows marked `developer` | developer answer | Roles, search, filters, export, History, duplicates, merge, versions, security, quality and journeys (Q-02b to Q-35h) were written by the developer. | Customer to confirm; Block topics stay open. |

## Stories

| Story | Title | Priority | MVP | Estimate | Depends on |
|---|---|---|---|---|---|
| US-001 | Register and sign in | must | yes | 3 | — |
| US-002 | Create a group and list my groups | must | yes | 3 | US-001 |
| US-003 | Invite people to a group | must | yes | 5 | US-002 |
| US-004 | Manage members and roles | must | yes | 5 | US-003 |
| US-005 | Create, view and edit a contact | must | yes | 5 | US-002 |
| US-006 | Tag contacts | must | yes | 3 | US-005 |
| US-007 | Rename and delete tags of a group | must | yes | 3 | US-006 |
| US-008 | Free key-value attributes on a contact | should | yes | 3 | US-005 |
| US-009 | Contact list with search and sorting | must | yes | 5 | US-005, US-006, US-008 |
| US-010 | Filter the contact list | must | yes | 5 | US-009 |
| US-011 | Select contacts and export to Excel | must | yes | 5 | US-009 |
| US-012 | Delete to Trash, restore and delete forever | must | yes | 5 | US-005 |
| US-013 | History of contact changes | should | yes | 5 | US-003, US-004, US-005, US-006, US-007, US-008, US-011, US-012, US-015 |
| US-014 | Group-wide History view with filters | should | yes | 3 | US-013 |
| US-015 | Delete and restore a group | should | yes | 3 | US-002, US-004 |
| US-016 | Delete my own account | should | yes | 3 | US-004, US-013 |
| US-017 | Secured REST API with OpenAPI description | must | yes | 5 | US-001 |
| US-018 | Slovenian and English user interface | should | yes | 2 | US-001 |
| US-019 | Record versions of a contact | should | yes | 2 | US-005, US-006, US-008 |
| US-020 | View, compare and restore versions | should | no | 5 | US-019, US-013 |
| US-021 | Find duplicate contacts | should | no | 5 | US-005, US-012 |
| US-022 | Merge duplicate contacts | should | no | 8 | US-021, US-012, US-019, US-020, US-013 |

## Next step

Review: answer the rows above (with `/run` the review asks the questions), then continue with `/preflight`.
