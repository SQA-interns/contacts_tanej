# ADR-04: Search, filter and sort in the database

- Status: accepted · Phase 2 · Drivers: AR-15, BR-07, R-009-01..10, R-010-01..11, SR-07, NFR-01, EX-009-03, EX-009-07

## Context

Search ignores case and accents ("skerlj" finds "Škerlj"); sorting follows Slovenian rules (č, š, ž after c, s, z); filters ignore case or case and accents by field (BR-07). 10,000 contacts must answer in under 1 second at the 95th percentile with 20 users (NFR-01). User text must never become SQL text (SR-07). PostgreSQL ICU collations that ignore accents are non-deterministic, and `LIKE` on them is not supported in PostgreSQL 17.

## Options

1. Fetch all contacts and filter in Java. Simple; breaks NFR-01 and AR-15 ("use the database").
2. Non-deterministic ICU collation for matching. Rejected: no `LIKE` on it in PostgreSQL 17.
3. Fold text in the database: `f_fold(x) = lower(unaccent(x))` (an IMMUTABLE wrapper over the `unaccent` extension) and `LIKE` on the folded value; a trigram (`pg_trgm`) GIN index on a generated column; sorting with the ICU collation `"sl-SI-x-icu"`.
4. Fold in the application and store folded copies. Rejected: every write path must keep the copies in step.

## Decision

Option 3 (V1 migration creates `unaccent`, `pg_trgm`, `f_fold`, `contacts.search_fold` as a generated column, and trigram indexes on `search_fold` and on `f_fold(attributes.value)`).

- **Words.** The search text is trimmed, split at spaces, each word folded the same way in Java (`Normalizer` NFD, strip combining marks, lower-case) and bound as `'%' || escape(word) || '%'` with `\`, `%`, `_` escaped (EX-009-03). A contact matches when every word is contained in `search_fold` (first name, last name, e-mail, company, city) or in the folded name of one of its tags or in the folded value of one of its attributes (R-009-03). Phone and notes are never searched (R-009-04).
- **Conditions** combine with AND (R-010-08): tags have-all / have-any / does-not-have (`EXISTS` and `NOT EXISTS` over `contact_tags` and `tags`, names compared with `lower`); attribute key equals value (`lower(key)` and `f_fold(value)`); key exists; country (code equality); company (`f_fold(company) = f_fold(?)`). Contacts in Trash are never listed (R-009-07, EX-010-04).
- **Sort.** The sort field comes from the fixed list `lastName, firstName, email, phone, company, city, country` mapped to column names in code; it never comes from the request text (SR-07). Default: last name, first name, then `id` for a stable order across pages (EX-009-06). `ORDER BY col COLLATE "sl-SI-x-icu" NULLS LAST` for ascending; descending is the exact reverse, so an empty last name sorts first (F-17, EX-009-07). Country sorts by the code's display name in the active language: the service sorts the page's country column with the language's `Collator` only when the country column is chosen (document in the spec).
- **Page** is 25 rows; a page past the last gives the last (EX-009-09). `total` is a `count(*)` of the same WHERE.
- **Suggestions** (tags, attribute keys, companies) use the same fold on a `LIKE` prefix/substring.
- **Duplicates** use the same `f_fold` (ADR-11).

## Consequences

- Needs `CREATE EXTENSION` rights at the first start (the official `postgres:17.6` image's user has them; a managed database must allow `unaccent` and `pg_trgm`; the install guide says so).
- The migration test proves `f_fold('ŠKERLJ') = 'skerlj'` and the Slovenian order Cerar, Čuk, Sever, Šuster, Zupan (`SkeletonIT`).
- NFR-01 is verified by the timing test of task T-NFR-1 on 10,000 seeded contacts; if the 95th percentile is over 1 s the first remedy is an index, not a change of approach.
