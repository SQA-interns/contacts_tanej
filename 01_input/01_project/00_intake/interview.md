# Interview

> For: agent (reads), human (reviews) · Owner: Product owner · Written by: `/grill-me` · Agent: writes in intake only

Notes of the requirements interview. IDs never change once given. Source: `customer: <file>` (from `customer_requirement/`), `customer` (an answer the customer gave the developer) or `developer`. Status: `answered`, `deferred` (with reason) or `open`.

Pre-filled from `customer-requirements.md` (Slovenian, translated); every pre-filled row marked `open` still needs the developer's confirmation.

## 1. Problem

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-01 | What hurts today, for whom, how do they cope now, what does it cost? | Mixed customers: some keep contacts in Excel sheets and e-mail lists; others use a competing CRM that is too complex or too expensive for them. Pains named: lost time, duplicate mailings, contacting people tagged `neObvescaj`. Cost is not known. | customer (+ customer: customer-requirements.md for the CRM ease-of-use hint) | answered (cost: unknown, deferred: no baseline to measure against) |

## 2. Product

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-02 | What do we build? | A multi-tenant system for managing the contact data of a project or company (only the name is needed). Design, upgrade, implement and deploy. Confirmed by the developer. | customer: customer-requirements.md | answered |
| Q-02a | Who uses the REST API? | In the MVP only our own React frontend. JSON with a published OpenAPI description; the same API serves the UI. API keys or tokens for other tools are out of scope for now. Whether other tools (e.g. a mailing tool) call it directly later: not decided. | customer | answered (later use: open — needs customer) |
| Q-02b | What is a "group": unit of isolation and sharing (several users per group) or owned by one user? | A group is shared. The creator becomes its Owner and can add other people. Members see all contacts in the group and nothing outside it. | developer (customer must confirm: rights topic) | open — needs customer |
| Q-02c | How does a person join a group? | Invite by e-mail: the Owner enters an e-mail address and a role; the invited person sees the group after signing in through Firebase with that address (registering first if needed). No invite links; no admin-only adding. | developer (customer must confirm: rights topic) | open — needs customer |
| Q-03 | What is the competitive advantage / core outcome, and how do we measure success? | Customer: ease of use compared with competing CRM systems (customer: customer-requirements.md); no measurable criteria set. Developer proposal: (1) a new user signs in, creates a group and adds a first contact in under 2 minutes without help; (2) in a group of about 1,000 contacts, search and filter results appear in under 1 second; (3) selected contacts export to an .xlsx that opens in Excel with all columns filled; (4) a user never sees another group's contacts (tested, zero tolerance). | developer | open — needs customer |
| Q-04 | Reference solution? | https://github.com/anzo26/SCM is a benchmark only; we write our own code on the required stack and do not reuse or fork any of SCM's code. First answer: its feature set was the minimum to match or beat. Updated at Q-36: SCM is inspiration only, not an acceptance check; acceptance is defined by our features (Q-07) and journeys (Q-35). Known SCM features we deliberately do not have: import from Excel (Q-22a) and bulk actions on several contacts besides export. | customer (+ customer: customer-requirements.md for the link); update via developer at Q-36 | answered (update: customer to confirm) |

## 3. Users and roles

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-05 | Who creates contact groups? | Users create groups themselves. Contacts are visible only inside their group. | customer: customer-requirements.md | open (confirm) |
| Q-06 | Which roles exist and what may each do? | Customer file: not stated ("authorisation: own"). Developer proposal, three roles per group. **Owner:** everything. **Editor:** view, search, filter; create, edit, tag, delete (to Trash) and merge contacts; restore an earlier version; see History and versions; export to Excel. **Viewer:** view, search, filter contacts only; may NOT export, may NOT see History or versions, may NOT edit, delete, merge or restore. **Owner only:** invite or remove members, change roles, delete the group, "Delete forever" (before the Trash period ends). | developer (customer must confirm: rights topic) | open — needs customer |
| Q-06a | More than one Owner? Handover? Last Owner? System administrator? | A group may have several Owners. An Owner can change any member's role, so ownership is handed over by making someone else Owner. The last Owner cannot be removed, demoted or leave; a group always has an Owner. No in-app system administrator: nobody sees or manages a group they are not a member of. | developer (customer must confirm: rights topic) | open — needs customer |
| Q-06c | Contact deletion: Trash period | A deleted contact goes to the group's Trash for 30 days; after that the system deletes it permanently and removes its personal data. 30 days is a developer proposal; no legal retention period was given. | developer | open — needs customer |
| Q-06d | Who may restore from Trash, who may "Delete forever"? | Owner and Editor may restore (the same people who may delete). "Delete forever" before 30 days: Owner only, after a confirmation. | developer (customer must confirm: rights topic) | open — needs customer |
| Q-06e | What happens to the History of a contact that is erased? | History entries stay, anonymised: History is append-only and lives as long as the group, but the erased contact's personal data is removed from its entries. | developer (customer must confirm: personal data) | open — needs customer |
| Q-06f | Deleting a group | Owner only; the group name must be typed to confirm; the group goes to its own Trash for 30 days, then is deleted permanently. While a group is in Trash it disappears from every member's list and its contacts are hidden; only Owners see it, in a "Deleted groups" list, and only an Owner can restore it within the 30 days. | developer (customer must confirm) | open — needs customer |
| Q-06b | What does a Viewer see that an Editor does not? Why? | Export is Editor/Owner only (personal data leaves the system); History and versions are Editor/Owner only. An Owner who wants someone to export makes them an Editor. | developer | open — needs customer |

## 4. Features (user goals, priority, MVP)

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-07 | Features named by the customer | (a) create/manage contact groups; (b) predefined contact fields; (c) free key-value fields; (d) free tags as categories; (e) audit trail of all contact changes; (f) versioning of contacts; (g) find duplicates; (h) merge duplicates; (i) list with search and filtering; (j) export selected contacts to MS Excel; (k) secured REST API. Priorities and MVP not stated in the file. | customer: customer-requirements.md | answered (see Q-07a) |
| Q-07a | Priority and MVP per feature | **Must, MVP:** F1 sign in through Firebase, create a group (name only); F2 invite members by e-mail with a role, change roles, remove members; F3 create, edit, view a contact with predefined fields; F5 free tags; F6 list with search and filtering (field, key-value, tag); F7 export selected contacts to .xlsx; F8 delete a contact to Trash, restore, "Delete forever"; F14 secured REST API with OpenAPI. **Should, MVP:** F4 free key-value fields; F9 History (audit trail) of every contact change (corrected by developer: should, not must); F13 delete and restore a group. **Should, later (second increment):** F10 versions of a contact (view, restore); F11 find duplicates; F12 merge duplicates. The later features are not dropped: the customer accepts the product only when they work too. No deadline given. Priorities come from the developer's table with two changes by the developer (F4, F13 to MVP). | customer (priority table: developer, changes confirmed by developer) | answered (customer to confirm priorities) |

## 5. Main flows, rules, exceptions, examples

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-20 | F1 first sign-in flow | Sign-in with e-mail and password, and "Sign in with Google"; no other providers and no multi-factor authentication in the MVP. A verified e-mail is required before any group can be opened (invitations match on e-mail). Passwords: Firebase defaults, at least 8 characters; reset through Firebase's e-mail flow. The front end keeps the Firebase session; the server checks the token on every request. A person with no group and no invitation sees an empty state with one button, "Create a group" (name only). | developer (customer must confirm: access) | open — needs customer |
| Q-20a | Invitations: what happens | No Accept/Decline step: the invited person sees the group automatically after signing in with that verified e-mail address (registering first if needed). Invitations expire after 14 days; at most 20 pending per group; the Owner can cancel them. | developer (customer must confirm: rights topic) | open — needs customer |
| Q-20b | A user deletes their own account | Allowed. Groups where the user is the only Owner must first be handed to another member or deleted. Their name stays in the History entries they caused, so the group keeps accountability for who changed what (chosen over replacing it with "Deleted user" or a pseudonym); their e-mail address is removed from those entries. This needs a legal basis the customer must confirm. | developer (customer must confirm: personal data, legal basis) | open — needs customer |
| Q-23 | F6 search | One box; partial-word match ignoring case and accents (Skerlj finds Škerlj) over first name, last name, e-mail, company, city, tag names and attribute values. Phone and notes are not searched. | developer | open — needs customer |
| Q-23a | F6 filters | Tag has all / has any / does not have; key equals value; key exists; country; company. Combined with AND; filters and search work together. No other filters. Saved filters: out of scope. | developer | open — needs customer |
| Q-23b | F6 list, selection, empty state | 25 contacts per page, default sort by last name (then first name), sortable columns, total count of matches shown. Tick box per row, "select all on this page" and "select all N results of the current filter"; the selection survives paging and is cleared when the filters change. Deleted contacts appear only in the Trash view. Empty result: "No contacts match" and a "Clear filters" button. | developer | open — needs customer |
| Q-23c | F6 performance | Results in under 1 second for a group of up to 10,000 contacts (developer proposal; the customer gave no group size). | developer | open — needs customer |
| Q-24 | F7 export content | Columns: the 11 predefined fields, a Tags column (tags separated by commas), then one column per key-value key used in the selection; headers use the UI names. One row per ticked contact, in the order of the current list sort; only ticked contacts of the group. With nothing ticked the Export button is disabled ("select all N results" covers exporting everything). | developer | open — needs customer |
| Q-24a | F7 file and limits | One sheet, `.xlsx`, header row, one row per contact. File name `<group name>_<YYYY-MM-DD>.xlsx`. Bold frozen header; e-mail and phone in text format. At most 10,000 contacts per export; above that the user must narrow the selection. Cells starting with `=`, `+`, `-` or `@` are written as plain text (formula injection). | developer | open — needs customer |
| Q-24b | F7 audit | Every export is logged in the History (who, when, how many contacts), visible to Owners and Editors like the rest of the History. Re-import from Excel: no, not in the MVP. | developer (customer must confirm: personal data leaves the system) | open — needs customer |
| Q-25 | F9 History: which events | Contact created, edited, deleted to Trash, restored, deleted forever (erasure); tags added or removed (including bulk rename and delete); key-value attributes added or removed; merges of duplicates (F12); exports; member invited, role changed, member removed, invitation cancelled (group-level entry); group deleted or restored. Viewing and searching are not recorded. | developer | open — needs customer |
| Q-25a | F9 History: entry content | Who (the user's name and e-mail), when (stored in UTC, shown in Europe/Ljubljana time for every user), the field name, old value and new value for every changed field; entries of one save are grouped. | developer | open — needs customer |
| Q-25b | F9 History: where shown, who can change it, retention | A History tab on each contact (newest first) and a group-wide History for Owners and Editors, filterable by person, event type and date range. Append-only: nobody can change or delete an entry; the only change is the anonymisation of an erased contact (Q-06e). Kept as long as the group exists; deleted with the group. History export: no, not in the MVP. | developer | open — needs customer |
| Q-27 | F11 when are two contacts duplicates? | Certain duplicate: the same e-mail, ignoring case. Probable duplicate: the same first and last name (ignoring case and accents) PLUS the same company or the same phone number; the same name alone is not reported. Contacts without e-mail, name and company are never matched. Within one group only, never across groups. Fuzzy matching (e.g. Novak / Nowak) is a later improvement. | developer | open — needs customer |
| Q-27a | F11 output and behaviour | A "Find duplicates" button in the group; the search covers the whole group, not only the current filter. The result is a list of duplicate sets, each marked certain or probable with the reason. Finding only reports and never changes data; it runs only when the user starts it (not on save). Owner and Editor may mark a set as "not a duplicate", after which it is not shown again. | developer | open — needs customer |
| Q-28 | F12 merge flow | Owner and Editor, within one group only. The user picks two or more contacts from a duplicate set; a side-by-side screen shows them field by field, and for each field the user chooses the value to keep (default: the first non-empty value of the oldest contact). Tags are combined. Attributes are combined; if the same key has different values, the user chooses. The user must confirm. The oldest contact survives with the chosen values (so its History stays); the others go to Trash for 30 days. Restoring a merged-away contact from Trash brings only that contact back and does not touch the survivor; to revert the survivor, the user restores its earlier version (F10); the screen tells the user this. One History entry lists the merged contacts and the chosen values; the merged contacts' History is linked to the survivor. Not allowed: merging contacts of different groups, or a contact with itself. No automatic merge. | developer (customer must confirm: destructive topic) | open — needs customer |
| Q-28a | "Not a duplicate" mark: scope | A dismissed set is identified by exactly the contacts in it. While dismissed, a duplicate set of exactly the same contacts is not shown again, even if those contacts are edited later. If a duplicate set contains a contact that was not in the dismissed set (e.g. a new contact C matches A and B), the set is shown again. Dismissing changes no contact and creates no version. Undoing a mark is not in the first version. | developer | open — needs customer |
| Q-29 | F10 what is a version | Each save of a contact that changes a field, tag or attribute on that one contact creates a new version with a running number (1, 2, 3, …). A bulk tag rename or delete does not. A merge creates one on the survivor. Restoring a version creates a new version, so numbers never go back. | developer | open — needs customer |
| Q-29a | F10 what can the user do | List the versions of a contact (number, who, when), open one read-only, compare it with the current one (changed fields highlighted), restore it (confirmation; Owner and Editor; Viewers do not see versions). A restore brings back all predefined fields, tags and attributes as they were, not the Trash state (a contact in Trash is restored from Trash first). A restore also records a History entry naming the restored version number and the changed fields. | developer | open — needs customer |
| Q-29b | F10 retention and limit; History vs versions | Versions are kept as long as the contact exists and erased together with it (they hold personal data). No limit on versions per contact in the MVP. Split: the History is the events (who changed what, old and new values, kept in anonymised form after erasure); versions are full snapshots that Owners and Editors can compare and restore. | developer | open — needs customer |
| Q-30 | F2 member management | Inviting someone who is already a member or already has a pending invitation is refused with a message; nothing changes (to change a role the Owner changes the member's role). A role change or removal takes effect at the member's next request; a removed member's next request for the group is answered "not found". Removing a member keeps their contacts (contacts belong to the group) and their History entries with their name. Any member may leave, except the last Owner (Q-06a). No limit on members per group and none on groups per user in the MVP; only 20 pending invitations per group (Q-20a). | developer | open — needs customer |
| Q-30a | Invitation e-mail | No. The product sends no e-mail at all, also not about invitations; the Owner tells the invited person themselves. The only e-mails are Firebase's own (verification and password reset). | developer (customer must confirm) | open — needs customer |
| Q-18 | Two people edit the same contact at the same time | The second save is refused with "this contact was changed by someone else" and the new version is shown; nothing is silently overwritten. | developer | open — needs customer |

## 6. Shared rules and data

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-08 | Predefined contact fields | Customer file: "to a certain degree" predefined: first name, last name, address, country, email, company, "etc."; required/optional, formats and uniqueness not stated. Developer proposal, 11 fields: first name, last name, e-mail, phone, company, job title, street and house number, postal code, city, country, notes (free text). Phone, job title and notes are not in the brief (it ends with "ipd."). | customer: customer-requirements.md (fields in the brief); developer (additions) | open — customer to confirm the additions |
| Q-08a | What is required to save a contact? | At least one of first name, last name, e-mail or company. A contact with all four empty is rejected and nothing is stored. Nothing else is required. | developer | open — needs customer |
| Q-08b | Validation | E-mail, when present, must have a valid format and is stored in lowercase. One e-mail and one phone per contact. Text fields at most 255 characters; notes at most 5,000. Country is chosen from a standard country list, not typed freely. Phone: stored as typed, only a character and length check, not forced into one format. | developer | open — needs customer |
| Q-08c | Is e-mail unique within a group? | No. A second contact with the same e-mail is saved without a warning; the duplicate search (F11) reports it. | developer | open — needs customer |
| Q-09 | Free fields | Customer file: key-value, e.g. `kompetenca=spletnoProgramiranje`. Rules: a key is unique per contact, ignoring case; a value is a single text (typed values like numbers or dates are out of scope for the MVP). Keys are free per contact; the form suggests keys already used in the group (autocomplete) but never forces them. At most 50 key-value pairs per contact; key 1 to 50 characters, value at most 255. Filters: "key equals value" and "key exists". Attributes are part of the History and of the Excel export (one column per key used in the selection). | customer: customer-requirements.md (example); developer (rules) | open — needs customer |
| Q-10 | Tags: format and limits | Customer file: free tags, e.g. `neObvescaj`, `udelezenec2025`; they serve as categories. Rules: tags belong to one group and are invisible to others. 1 to 50 characters, only letters, digits, "-" and "_", no spaces, compared ignoring case; the tag keeps the spelling it was first typed with. At most 50 tags per contact; no limit on distinct tags per group. | customer: customer-requirements.md (examples); developer (rules) | open — needs customer |
| Q-10a | Tag management | Typing a new tag creates it; there is no separate tag list to maintain. Owner and Editor may rename a tag (changes it on all contacts in the group) or delete it (removes it from all contacts, deletes no contact). Renaming a tag to a name another tag already has (ignoring case) is refused with the reason shown; nothing changes. Renaming to the same name with only different capitalisation just changes the spelling. A bulk rename or delete writes one History entry per affected contact, with the old and new tag name. A tag rename or delete does not create a new version of those contacts. | developer | open — needs customer |
| Q-10b | Tag filters | "Has all of these tags", "has any of these tags", "does not have tag X". | developer | open — needs customer |
| Q-10c | Does a tag carry meaning for the system (e.g. `neObvescaj` blocks something)? | No. Tags are labels only. The product sends no notifications or e-mails to contacts at all. `neObvescaj` is a marker telling other tools (using the REST API or the Excel export) not to notify the contact. The product's duty is only to make the tag visible and filterable, and to let users exclude it with "does not have tag" when selecting contacts for export. | developer (customer must confirm: consent) | open — needs customer |

## 7. Project-wide facts

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-11 | Language of the UI | The customer text is Slovenian; the brief states no UI language. Decision (developer): the UI is in Slovenian (the default) and English, with a language switch in the header; all texts in language files. API field names are in English. Excel header texts and country names follow the active UI language. | developer | open — needs customer |
| Q-11a | Formats | Slovenian: date d. M. yyyy, time HH:mm (24 hours), decimal comma. English: date yyyy-MM-dd, time HH:mm. Sorting and search follow Slovenian rules (č, š, ž after c, s, z). No currency and no tax anywhere. UTF-8 everywhere, including the API and the Excel file. Phone numbers stored as typed (character and length check only). | developer | open — needs customer |
| Q-11b | Countries of users and contacts | Not given by the customer. | — | open — needs customer |
| Q-26 | Time zone | Times are stored in UTC and shown in Europe/Ljubljana time for every user. | developer | open — needs customer |

## 8. Out of scope, non-goals, principles

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-21 | Does the product send e-mails or notifications to contacts? | No: sending is out of scope. Extended by Q-30a: the product sends no e-mail at all, except Firebase's own (verification and password reset). | developer (customer must confirm: consent) | open — needs customer |
| Q-22 | Direct API use by other tools | Out of scope in the MVP (see Q-02a). | customer | answered |
| Q-22a | Re-import from Excel | Out of scope in the MVP. | developer | open — needs customer |
| Q-22b | Bulk actions on several contacts | Besides export, no bulk actions on several contacts (a known SCM feature we deliberately do not have). Tagging is per contact; bulk tag rename and delete (Q-10a) apply to a tag, not to a selection. | developer | open — needs customer |
| Q-32 | Out of scope (MVP and second increment) | (1) sending e-mails or notifications to contacts or users (apart from Firebase's own); (2) direct use of the REST API by other tools and API keys for them (open for later); (3) import from Excel or any other file; (4) saved filters; (5) multi-factor authentication and sign-in providers other than e-mail/password and Google; (6) fuzzy duplicate matching and undoing a "not a duplicate" mark; (7) History export; (8) an in-app system administrator, billing and plans; (9) typed attribute values (numbers, dates) and several e-mails or phones per contact; (10) a mobile app; (11) export to CSV (only .xlsx); (12) usual CRM activity features: tasks, deals, a notes timeline, calendar, pipelines; (13) real-time collaboration and offline use; (14) contact data from external sources (social networks, address books). | developer | open — customer to confirm |
| Q-32a | Principles the product must never break | A user never sees data of a group they are not a member of (tested, zero tolerance). Nothing is silently overwritten or lost: a conflicting save is refused, and deletes go to Trash first (except an Owner's confirmed "Delete forever"). The History is never changed after it is written, except the anonymisation after an erasure. Personal data leaves the system only through the export by an Owner or Editor, and every export is logged. The product never contacts a person on its own. Simple wins: no feature may add a step to the common flows (create a contact, search, export) if fewer steps give the same result. | developer | open — customer to confirm |

## 9. Personal data

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-33 | Legal role, hosting region, special categories | Legal matters (controller or processor role, processing agreement, privacy notice, consent) are handled by the customer outside the software and are not part of this project. The product adds no legal features beyond what is specified: isolation, the History, Trash and permanent erasure after 30 days. Hosting region: not decided; the customer chooses and operates production hosting (cloud or own infrastructure), so the product must be deployable anywhere. Whether data must stay in the EU, the countries of the contacts, and whether special categories of data (health, religion, …) are stored: open. | customer (via developer) | open — needs customer (region, countries, special categories) |
| Q-33a | Personal data items and who sees them | Contacts' data (11 fields, tags, attributes; the notes field is free text and may hold anything): visible to the members of the group per role (Viewers see contact data, not History or versions, and cannot export). Users' data: name and e-mail (kept by Firebase and shown in the History to Owners and Editors). Erasure: contacts after 30 days in Trash (Q-06c), anonymised History (Q-06e), account deletion (Q-20b). | developer | open — needs customer |

## 10. Security

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-12 | Authentication / authorisation | Customer file: REST API "appropriately secured"; authentication Firebase; authorisation own (custom). Developer detail: every endpoint except the health check requires a valid Firebase ID token (`Authorization: Bearer`); missing or invalid gives 401 and no data. The group is always in the request path (`/groups/{id}/...`) and never taken from the request body. A non-member gets 404 (the group's existence is not revealed); a member whose role is too low gets 403. The server checks per request: valid token, verified e-mail, membership and role. | customer: customer-requirements.md; developer (detail) | open — needs customer |
| Q-12a | Transport, browser, input | HTTPS only; the API refuses plain HTTP. The web app's own address is the only allowed origin (CORS) and the standard security headers are set. Every input is validated on the server by the rules of the stories; search and filter inputs are never inserted into queries as text. | developer | open — needs customer |
| Q-12b | Abuse limits | About 100 requests per minute per user; 1 MB maximum request size. No separate export limit and no extra lock after failed sign-ins beyond Firebase's own protection. | developer | open — needs customer |
| Q-12c | Errors and logs | One error format with a machine-readable code, no stack traces. The server logs failed token checks, 401, 403 and 404 responses and rate-limit hits; never contact data, tokens or passwords. | developer | open — needs customer |
| Q-12d | Data at rest and standard | Database and backups are encrypted by the hosting (the customer's choice, Q-33); the product does not encrypt fields itself. Standard: the customer named none; use OWASP ASVS 5.0 Level 2, because the product stores personal data of many people and separates tenants. | developer | open — needs customer |
| Q-13 | Data isolation | Data must be isolated between different users and their projects. | customer: customer-requirements.md | open (confirm) |

## 11. Integrations and external services

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-14 | Integrations | Firebase (authentication); Excel (export of selected contacts to MS Excel; built into the product, no external service). | customer: customer-requirements.md | answered |
| Q-14a | Firebase in development and tests | Automated tests and CI use the Firebase Authentication Emulator (no internet, no real accounts, no secrets), with test users that have fixed e-mail addresses, passwords and verified e-mails. One real development Firebase project, created by the developer, is used for manual checks; its credentials live only in the developer's `.env`, never in the repository or the chat. A manual check against a real Firebase project is done once before delivery. | developer | answered |
| Q-14b | Production Firebase project | Whether it exists and who owns it: open. The product reads its settings (project ID, web app key) from environment variables. "Sign in with Google" in production needs setup in the Firebase console by whoever owns the project. | developer | open — needs customer |

## 12. Quality

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-15 | UI quality, ease of use | Customer file: friendly overview, search, filtering; ease of use is the competitive advantage. Developer: a new user needs no manual; every screen has a clear empty state and a clear error message. | customer: customer-requirements.md; developer | open — needs customer |
| Q-15a | Size and response times | The whole system for up to 100 registered users, 20 at the same time, groups of up to 10,000 contacts. List, search and filter under 1 second; opening a contact under 500 ms; export of 1,000 contacts under 10 seconds. Measured on the deployed system with normal data. (Export of up to 10,000 contacts, Q-24a: no time target yet.) | developer | open — needs customer |
| Q-15b | Availability and backups | 99% per month, no on-call duty, planned maintenance announced. The product gives a health-check endpoint, and a restart loses no data. Database backed up daily, backups kept 14 days, a restore tested once before release. The product documents what to back up and how to restore. | developer | open — needs customer |
| Q-15c | Browsers, devices, accessibility | Current and previous major version of Chrome, Edge, Firefox and Safari on desktop; a responsive layout that works on a tablet and a phone (320 px width) for viewing, searching and filtering (editing on a phone allowed, not tested in depth). WCAG 2.1 level AA for the main flows (sign in, list, create or edit a contact, export), with keyboard use and visible focus. | developer | open — needs customer |

## 13. Technology

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-16 | Required stack | Customer file: backend Spring Boot; frontend React ("Resct" in the text, confirmed as React); data PostgreSQL; Excel integration; authentication Firebase; authorisation own. Versions not stated. | customer: customer-requirements.md | answered |
| Q-16a | Versions and tools | The customer fixes no versions. Developer: Java 21 (LTS), Spring Boot 3.x current stable, Maven; React with TypeScript, built with Vite; PostgreSQL 16 or newer; Excel export with Apache POI on the server; tests with JUnit and Testcontainers (backend), Vitest and Playwright (frontend). Exact versions are pinned in `tech-stack.md` and checked by `/preflight`. | developer | answered |
| Q-16b | Database changes | Flyway migrations, versioned in the repository, never edited by hand. | developer | answered |
| Q-16c | Licences | The product's own code is proprietary (all rights with the customer). Allowed: Apache-2.0, MIT, BSD, ISC, 0BSD, CC0, PostgreSQL licence. Strong copyleft (GPL, AGPL) is forbidden in the product. No Hibernate or JPA: the database layer uses Spring JDBC (JdbcTemplate) with parameterised queries. Listed exceptions: Logback, used under its EPL option; EPL and MPL only in test or build scope (e.g. JUnit, axe-core); the Java runtime (GPL with classpath exception) only as the container base image. | developer (customer must confirm: legal) | open — needs customer |
| Q-16d | Forbidden | Reusing any code from the SCM example (Q-04). No Google service other than Firebase Authentication (so no Firestore or Cloud Storage). | developer | open — needs customer |

## 14. Environments and deployment

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-17 | Where does it run? | Customer file: "deploy" is required; where, by whom, backups not stated. Developer: the customer decides and operates production (cloud or own infrastructure), so the product must deploy in either. No hosted demo by us, nobody on call. Budget, domain and the production host are the customer's concern and not known. | customer: customer-requirements.md; developer | open — needs customer (host, domain) |
| Q-17a | Packaging and delivery | Container images (backend, frontend) and a Docker Compose setup with PostgreSQL, plus an install and backup guide (start, update, backup, restore, health check). All configuration (database address and credentials, Firebase project ID and web app key, allowed origin) through environment variables, nothing tied to one provider; `secrets.env.example` lists names without values. The project does not deploy to the customer's production. | developer | answered |
| Q-17b | Environments | Three: development (local Docker, or the team's internal server), test (automated, Firebase emulator and a throwaway database), production (the customer). No staging. | developer | answered |
| Q-17c | Updates and migrations | The backend runs Flyway migrations at start-up. Migrations go forward only; rollback means restoring the backup from before the update. Acceptable. | developer | answered |

## 15. Design constraints

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-34 | Brand, name, look | The customer gave no design, brand, logo, colours or font. A neutral calm look from the baseline with one accent colour and the product name as text; working title "Contact Groups" (Slovenian "Skupine kontaktov"). Brand colours, logo and name come later from the customer, so they live in one place (tokens) and can be replaced without touching the layout. | developer | open — needs customer (brand, name) |
| Q-34a | Tone, theme, devices | Plain, short and friendly texts; formal "vi" in Slovenian. Light theme, plus dark mode that follows the system setting. Desktop first, responsive down to a phone (Q-15c). | developer | open — needs customer |
| Q-34b | Reference products | None given. Which SCM screens the customer likes and what they dislike in competing CRMs: unknown. | — | open — needs customer |

## 16. End-to-end journeys

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-35 | J1 First use | A new person registers with e-mail, verifies it, signs in, sees the empty state, creates the group "Konferenca 2026", adds the contact Ana Novak (ana.novak@example.com, company "Acme"), adds the attribute `kompetenca=spletnoProgramiranje` and the tag `udelezenec2025` to Ana, then finds her with the search box. | developer | open — needs customer |
| Q-35a | J2 Team work | The Owner invites `marko@example.com` as Editor and `petra@example.com` as Viewer. Both sign in and see the group. Marko edits Ana's phone; the Owner sees the change in the History with the old and new value. Petra can view but sees no Export button, no History and no edit controls. | developer | open — needs customer |
| Q-35b | J3 Tags and export | The Editor tags 3 of 5 contacts `udelezenec2026` and one of them `neObvescaj`. Filters "has `udelezenec2026`, does not have `neObvescaj`", selects all results and exports. The file has 2 rows, the right columns and tags, and an export entry appears in the History. | developer | open — needs customer |
| Q-35c | J4 Isolation | Two Owners with different groups. Neither sees the other's contacts in the list, search or export, by guessing a URL (404) or by sending the other's group ID to the API. | developer | open — needs customer |
| Q-35d | J5 Conflict and Trash | Two Editors open the same contact. The first saves; the second is refused with the "changed by someone else" message and sees the new values. An Editor deletes a contact, sees it in Trash, restores it. An Owner deletes it and uses "Delete forever". | developer | open — needs customer |
| Q-35e | J6 Duplicates and merge (second increment) | Two contacts with the same e-mail are reported as a certain duplicate. The Editor merges them, picks values, confirms. The survivor has the chosen values and the combined tags; the other is in Trash; the History shows the merge. The Editor then restores the survivor's earlier version. | developer | open — needs customer |
| Q-35f | J7 Leaving | The only Owner tries to leave and is refused. After handing over to another Owner they delete their account; their name stays in the History and their e-mail is removed. Also, the Owner removes a member; that member's next request for the group is answered "not found" and the group disappears from their list. | developer | open — needs customer |
| Q-35g | J8 Group deletion | The Owner deletes the group by typing its name. It disappears for the members, appears in "Deleted groups" for Owners, and is restored. | developer | open — needs customer |
| Q-35h | Which journeys must pass | J1 to J5, J7 and J8 for the MVP; J6 after the second increment. The language switch and an expiring invitation are covered by the stories' acceptance tests, not as journeys. | developer | open — needs customer |

## 17. Project done

| ID | Question | Answer | Source | Status |
|---|---|---|---|---|
| Q-19 | Does acceptance need the second increment? | Yes: the customer accepts the product only when F10 (versions), F11 (find duplicates) and F12 (merge) also work. | customer (via developer) | answered |
| Q-36 | What must the customer see to accept the product? (DoD-P) | (1) Journeys J1 to J8 pass (J6 with the second increment). (2) Success criteria (Q-03): a new user signs in, creates a group and adds a first contact in under 2 minutes without help; search and filter respond in under 1 second for up to 10,000 contacts (Q-23c); an exported file opens in Excel with all columns filled; zero tolerance on isolation: one test per endpoint shows that a non-member gets 404. (3) Quality: the numbers of Q-15a to Q-15c are met on the deployed system; the security check is against OWASP ASVS 5.0 Level 2 (Q-12d); no Critical or High finding is open. (4) Delivery: container images, Docker Compose setup, install and backup guide, a tested restore, the OpenAPI description, and a short user guide in Slovenian. (5) Demo of J1 to J5 on a system started from the delivered files. (6) The second increment (versions, duplicates, merge) is finished and J6 passes. Acceptance is a demo plus the written verification report the workflow produces; no separate acceptance document. SCM is not an acceptance check (Q-04). | developer | open — needs customer |

## Contradictions resolved

| IDs | Contradiction | Resolution |
|---|---|---|
| Q-10c, Q-02a | `neObvescaj` is honoured by other tools through the API or Excel, but direct API use by other tools is out of the MVP | In the MVP only the Excel export reaches other tools; later API use stays open for the customer (confirmed by developer) |
| Q-03, Q-23c | Q-03 sets search under 1 second for about 1,000 contacts; Q-23c sets it for up to 10,000 | The stricter Q-23c (10,000) replaces the group size in Q-03 criterion 2 (developer, to be confirmed by customer) |
| Q-04, Q-36 | Q-04 first said SCM's feature set is the minimum to match or beat; Q-36 says SCM is inspiration only, not an acceptance check | The later answer (Q-36) replaces the first: acceptance is by our features and journeys; the customer must confirm |
| Q-03, Q-36 | Q-03 criterion 4 ("never sees another group's contacts") is tested how | Q-36: one test per endpoint shows a non-member gets 404 |

## Open items

Every answer marked `open — needs customer` is a developer proposal the customer has not confirmed; the Block-class ones (rights, personal data, deletion, legal) must be confirmed before the related rule is built. Grouped:

| ID | Why open | Who must answer |
|---|---|---|
| Q-01 | Cost of the current way of working is unknown (no baseline) | Customer |
| Q-03, Q-36 | Success criteria and the acceptance list are developer proposals | Customer |
| Q-04 | Update "SCM is inspiration only, not an acceptance check" replaces the first answer | Customer |
| Q-02a, Q-22 | Whether other tools (e.g. a mailing tool) may call the API directly later | Customer |
| Q-02b, Q-02c, Q-06, Q-06a, Q-06b, Q-06d, Q-20a, Q-30 | Rights: shared groups, invite by e-mail, three roles, Owner rules, Viewer restrictions, invitation rules, member rules (Block class) | Customer |
| Q-06c, Q-06e, Q-06f, Q-20b, Q-33a | Deletion and personal data: 30-day Trash, anonymised History, group deletion, account deletion keeping the name (legal basis needed) | Customer |
| Q-27, Q-27a, Q-28, Q-28a | Duplicate rules, "not a duplicate" marks, merge flow (merge is destructive; Block class) | Customer |
| Q-08, Q-08a–c | Contact fields beyond the brief (phone, job title, notes), required rule, limits, e-mail not unique | Customer |
| Q-08b | Which standard country list (e.g. ISO 3166) and its names per language | Developer, then customer |
| Q-09, Q-10, Q-10a–c, Q-21, Q-30a | Key-value and tag rules; `neObvescaj` has no system meaning; the product sends no e-mails: consent-related | Customer |
| Q-11, Q-11a, Q-11b, Q-26 | UI language (Slovenian and English), formats, time zone (Europe/Ljubljana); the countries of users and contacts are unknown | Customer |
| Q-12 … Q-12d | Security details and OWASP ASVS 5.0 Level 2 chosen by the developer | Customer |
| Q-33 | EU hosting requirement, special categories of data (the free-text notes field could hold them), Firebase Authentication processes e-mails at Google possibly outside the EU | Customer |
| Q-14b | Production Firebase project: exists, owner, Google sign-in setup | Customer |
| Q-15, Q-15a–c | Numbers are developer proposals; export of up to 10,000 contacts has no time target (Q-15a gives 1,000 only) | Developer, then customer |
| Q-15b | Backups kept 14 days still hold contacts erased after 30 days in Trash; and who owns the 99% availability when the customer operates production | Customer |
| Q-16c, Q-16d | Licence rule with listed exceptions (Logback under EPL; EPL/MPL in test and build scope only; no Hibernate/JPA) and "no Google service other than Firebase Authentication": legal | Customer |
| Q-17 | Production host, domain, budget, whether their infrastructure runs containers | Customer |
| Q-22a, Q-22b, Q-32 | Risk: no import from Excel and no bulk actions, yet customers come from Excel sheets and e-mail lists and must enter existing contacts by hand; the 14 out-of-scope items | Customer |
| Q-34, Q-34a, Q-34b | Product name, brand, dark mode, which SCM screens they like, what they dislike in competing CRMs | Customer |
| Q-35 … Q-35h | Journeys and which must pass | Customer |
| Q-05, Q-07a, Q-13, Q-16 | Pre-filled from the customer file and confirmed by the developer; no further answer needed except the customer's confirmation of priorities (Q-07a) | Customer |
| Q-18 | Concurrent edit rule (second save refused) | Customer |

**SCM features list:** no longer needed for acceptance (Q-36); `/to-spec` may still use SCM as inspiration for stories.

**Glossary terms to fix in `business-rules.md`** (one name each, from this interview): group, Owner, Editor, Viewer, member, invitation, contact, tag, attribute (key-value), History, version, Trash, Delete forever, duplicate set (certain, probable), merge, survivor, export.

## Next

Run `/to-spec`.
