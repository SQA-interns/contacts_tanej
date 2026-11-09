# ADR-11: Finding duplicates and merging

- Status: accepted · Phase 2 · Drivers: R-021-01..12, R-022-01..14, BR-05, BR-07, D-22, F-15 (OQ-022-03)

## Context

Duplicates are reported on demand over all contacts of a group not in Trash (R-021-01), as sets linked through certain (same e-mail) or probable (same first and last name plus the same company or the same phone) matches. A merge joins a picked set into the oldest contact, moves the others to Trash and must be atomic (US-022). Estimate of US-022 is 8, above a third of increment 2; OQ-022-03 leaves the split to planning.

## Decision

- **Detection in the service, on folded values from the database.** One query returns `id, created_at, first_name, last_name, email, company, phone` (and display fields) of the group's active contacts, with `f_fold` applied in SQL to names, company and e-mail. The service builds pairs by keys: e-mail (non-empty, lower-case), name+company (first and last name both non-empty; company non-empty), name+phone (phone reduced to digits and `+`, non-empty). Contacts that have no e-mail, first name, last name and company are never matched (R-021-05). Pairs become sets with union-find; a set is `CERTAIN` if any link is an e-mail match, else `PROBABLE`; reasons list the kinds of links present (R-021-07). Groups with 10,000 contacts are handled with hash maps, not pair loops.
- **Dismissals.** `set_key` is the SHA-256 of the sorted contact ids; a found set whose key is stored is not shown; a set with another member shows (R-021-09, R-021-10, EX-021-05). Marking changes no contact and records no version.
- **Merge** (`MergeService`, one transaction): lock the picked contacts `FOR UPDATE` in id order; refuse with 400 `TOO_FEW_CONTACTS` for fewer than two, a repeated id or a contact in Trash, with 404 for an id outside the group, with 409 `CONFLICT` when any revision differs from the one sent (R-022-12). Survivor = oldest `created_at`, lower id on a tie (EX-022-03). Result fields come from the request (the screen preselects the first non-empty value of the oldest contact, R-022-02); tags are the union ignoring case; attributes are the union by key ignoring case, and a key with different values needs a choice in `attributeChoices`, else 400 `ATTRIBUTE_CHOICE_REQUIRED` (R-022-04, EX-022-02). The merged result is validated with the same rules as a save (BR-05, 50 tags, 50 attributes): a break gives 400 naming the limit and changes nothing (R-022-09). Then: update the survivor, bump `revision`, record one version only if its content changed (D-22), move the others to Trash with `trashed_at = now`, insert `contact_merges`, append one `CONTACTS_MERGED` entry listing the merged contacts and chosen values.
- **Restore of a merged-away contact** is the normal restore; the survivor is untouched (R-022-07). Reverting the survivor is the version restore of US-020 (R-022-08).
- **Split of US-022 (OQ-022-03).** Two task groups, each one `/implement` session: "merge screen and merge operation" (R-022-01..05, R-022-07..14) and "History linking" (R-022-06); see `02_tasks.md`.

## Consequences

- No fuzzy matching: Novak/Nowak is not reported (EX-021-02). Phones `+386 41 555 123` and `041 555 123` differ (EX-021-08) because only digits and `+` are compared.
- The search runs only when the user asks (R-021-08) and is read-only.
