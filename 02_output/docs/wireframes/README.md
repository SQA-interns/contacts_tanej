# Wireframes

> For: agent (writes), human (reads) · Written in: phase 2 (`/wireframes`) · Source: `docs/02_specification.md` ("User interface"), `general/design/ui-baseline.md` · Agent: writes

Grey wireframes of every screen at the three widths (UI-22), approved by the developer before any screen is built. Files: `WF-nn_<slug>.html`, `wireframe.css`, `WF-nn_<slug>_mobile.png`, `…_tablet.png`, `…_desktop.png`.

| Id | Screen | Stories and AC | States | Files | Approved (decision) |
|---|---|---|---|---|---|
| WF-01 | Sign in (with forgot password) | US-001 | busy, Firebase error, forgot password | `WF-01_sign-in` | D-26 pending |
| WF-02 | Register | US-001 | busy, validation, Firebase error | `WF-02_register` | D-26 pending |
| WF-03 | Verify your e-mail | US-001 | sent again, error | `WF-03_verify-email` | D-26 pending |
| WF-04 | Reset password confirmation | US-001 | error | `WF-04_reset-password` | D-26 pending |
| WF-05 | Groups list; Create, Rename, Delete group dialogs | US-002, US-015 | loading, empty, error, name taken | `WF-05_groups` | D-26 pending |
| WF-06 | Error and not-found pages | US-002, US-017 | not found, group not found, rate limit, unavailable, sign in again | `WF-06_error-pages` | D-26 pending |
| WF-07 | Members tab, invitations | US-003, US-004 | loading, empty, limit, errors, Viewer view, confirmations, last Owner | `WF-07_members` | D-26 pending |
| WF-08 | Contacts list (search, chips, selection, Export) | US-009, US-010, US-011 | loading, empty (Owner, Editor, Viewer), no match, export disabled, too many, exporting, error | `WF-08_contacts` | D-26 pending |
| WF-09 | Filter dialog | US-010 | active chips, validation, no match | `WF-09_filter-dialog` | D-26 pending |
| WF-10 | Contact (11 fields, tags, attributes) | US-005, US-006, US-008 | new, loading, error, validation, saving, conflict, Viewer, in Trash, delete dialog | `WF-10_contact` | D-26 pending |
| WF-11 | Contact History | US-013 | loading, empty, error, erasure marker | `WF-11_contact-history` | D-26 pending |
| WF-12 | Versions, version view, compare | US-020 | loading, empty, error, restore dialog, nothing to change | `WF-12_contact-versions` | D-26 pending |
| WF-13 | Tags | US-007 | loading, empty, error, rename, delete | `WF-13_tags` | D-26 pending |
| WF-14 | Trash | US-012 | loading, empty, error, Editor view, Delete forever dialog | `WF-14_trash` | D-26 pending |
| WF-15 | Group History | US-014 | loading, empty, no match, invalid range, error | `WF-15_group-history` | D-26 pending |
| WF-16 | Duplicates | US-021 | not searched, searching, none found, error | `WF-16_duplicates` | D-26 pending |
| WF-17 | Merge | US-022 | loading, confirm enabled, merging, limit, error | `WF-17_merge` | D-26 pending |
| WF-18 | Deleted groups | US-015 | loading, empty, error, name clash, restored | `WF-18_deleted-groups` | D-26 pending |
| WF-19 | Account | US-016 | delete dialog, sole Owner refusal, re-sign-in, deleting, error | `WF-19_account` | D-26 pending |

US-018 (language switch) is in the header of every page. US-019 has no screen.

## Notes

- One page per screen, one `wireframe.css`. States and dialogs are labelled sections under the main view. The three widths come from the same page (375, 768, 1440 px, Playwright Chromium; no horizontal overflow also checked at 320 px).
- Below 768 px tables become stacked cards. Below 1024 px the navigation is behind the Menu button and phone, city and country are hidden in the contacts list. The action bar is drawn in the flow; in the build it is sticky on mobile.
- No global search in the header: no story asks for it (UI-27 lists it); the contact search is on the list.
- Dynamic content is grey bars; sample numbers (60 contacts, 23 days left) are labels only.
