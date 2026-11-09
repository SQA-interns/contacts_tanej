# ADR-12: Excel export

- Status: accepted · Phase 2 · Drivers: R-011-01..12, SR-08, NFR-03, NFR-15, DoD-P04, D-21

## Context

The export is the only way contact data leaves the product (PP-04). The file must be exact, safe against formula injection, open correctly in Excel (UTF-8, text cells for e-mail and phone) and traceable (a History entry). 1,000 contacts in under 10 seconds; 10,000 must not run out of memory.

## Decision

- **Library** Apache POI `poi-ooxml` 5.4.1 (listed), using `SXSSFWorkbook` with a window of 100 rows so 10,000 rows stream (NFR-15).
- **Selection.** The request carries either `contactIds` (the ticks, kept across pages by the web app) or `filter` (select all N results). The service re-evaluates both against the database: contacts of this group, not in Trash, ids that no longer qualify are left out (R-011-12, EX-011-08). Over 10,000 contacts is refused with 400 `TOO_MANY_CONTACTS` before any file is built (R-011-09, EX-011-01).
- **Order** is the current list sort, carried by the request as `sort`, with the same tie-break as the list, for both `contactIds` and `filter` (R-011-04).
- **Columns** are the 11 predefined fields, `Tags`, then one column per attribute key used in the selection (case-insensitive, alphabetical by Slovenian rules, spelling of the first contact in export order, D-21). Headers and country names use the request `language` (`sl` or `en`) from a server-side message file of the same wording as the UI; country names from `java.util.Locale` display names (R-011-05). Tags are sorted by Slovenian rules and joined by `, ` (D-21, AC-011-03).
- **Cells** are strings. E-mail and phone use the text cell format. A value starting with `=`, `+`, `-` or `@` is written as a string cell with the quote-prefix style set (POI `CellStyle.setQuotePrefixed`), so the text is stored unchanged and Excel never evaluates it (R-011-08, SR-08); other characters, including a leading tab, are written as given (EX-011-06).
- **Header** row bold and frozen (R-011-07). Encoding is UTF-8 inside the XLSX package (R-011-11).
- **File name** `<group name>_<YYYY-MM-DD>.xlsx`, date from the `Clock` in `Europe/Ljubljana`, characters `\ / : * ? " < > |` and control characters replaced by `_` (R-011-06, D-21, EX-011-02, EX-011-05); sent as `Content-Disposition: attachment; filename*=UTF-8''<percent-encoded>`.
- **History.** One `EXPORTED` entry with the number of contacts in the same transaction that reads the selection (R-013-04). The export is allowed to Owner and Editor only (403 for a Viewer, R-011-10).
- **Tests** read the file back with POI and compare every cell with the source contacts (AC-011-02, AC-011-11), and compare the structure with `export-workbook.schema.json` (DoD-P04).

## Consequences

- The Slovenian and English headers live in the backend as a small message bundle; the language files of the web app carry the same words, and a test compares the two so they cannot drift.
- One manual opening in Microsoft Excel at the demo is still needed (DoD-P04, environments.md).
