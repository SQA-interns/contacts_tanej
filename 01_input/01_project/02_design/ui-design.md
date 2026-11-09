# UI design

> For: agent (reads), human (reviews) · Owner: Design lead · Agent: read-only

Project overrides of `general/design/ui-baseline.md`. Everything not listed here follows the baseline. Topic 15 of the interview (Q-34, Q-34a, Q-34b) gave the following.

## Brand and tone

| Item | Value | Source |
|---|---|---|
| Primary colour | None given. The baseline accent applies and lives only in the tokens, so the customer's colour can replace it without touching the layout. | Q-34 |
| Logo | None. The product name as text: working title "Contact Groups" (Slovenian "Skupine kontaktov"), also kept in the tokens and language files. | Q-34 |
| Typeface | None given. The baseline typeface applies. | Q-34 |
| Tone of the copy | Plain, short and friendly; formal "vi" in Slovenian. | Q-34a |
| Reference products | None given. Which screens of SCM the customer likes and what they dislike in competing CRMs: unknown. | Q-34b |

## Overrides

| Baseline rule | Override | Reason | Source |
|---|---|---|---|
| UI-08 | Design desktop first (the list and the contact screen are planned for a wide screen) and make the layout responsive down to a phone at 320 px, without horizontal scrolling. | The customer's users work with contact lists on a desktop; viewing, searching and filtering must still work on a tablet and a phone (NFR-08). The baseline's breakpoints and 320 px rule stay. | Q-34a, Q-15c |