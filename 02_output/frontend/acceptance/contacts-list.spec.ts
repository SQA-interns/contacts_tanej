import {
  ANA,
  MARKO,
  PETRA,
  STACK_URL,
  dialog,
  expect,
  openGroup,
  row,
  scene,
  test,
  type Client,
} from './support';
import type { Page } from '@playwright/test';

/** The contacts list (WF-08), the filter dialog (WF-09) and the export bar: US-009, US-010, US-011. */

async function fill(owner: Client, groupId: string, count: number, make: (i: number) => object) {
  for (let i = 0; i < count; i++) await owner.contact(groupId, make(i));
}

const search = (page: Page) => page.getByLabel('Search');

async function addCondition(page: Page, condition: string, values: Record<string, string>) {
  await page.getByRole('button', { name: 'Filter' }).click();
  await dialog(page).getByLabel('Condition').selectOption({ label: condition });
  for (const [label, value] of Object.entries(values)) {
    await dialog(page).getByLabel(label, { exact: true }).fill(value);
    if (label === 'Tags') await page.keyboard.press('Enter');
  }
  await dialog(page).getByRole('button', { name: 'Add condition' }).click();
}

test('AC-009-01 60 contacts: 25 on the first page, the total 60, and 10 on page 3', async ({ page }) => {
  const { owner, groupId } = await scene('Konferenca 2026');
  await fill(owner, groupId, 60, (i) => ({
    firstName: 'Ime',
    lastName: `Priimek${String(i).padStart(2, '0')}`,
  }));
  await openGroup(page, ANA, 'Konferenca 2026');
  await expect(page.getByText('60 contacts')).toBeVisible();
  await expect(row(page, 'Priimek00')).toBeVisible();
  await expect(row(page, 'Priimek24')).toBeVisible();
  await expect(row(page, 'Priimek25')).toHaveCount(0);
  await page.getByRole('button', { name: '3', exact: true }).click();
  await expect(row(page, 'Priimek50')).toBeVisible();
  await expect(row(page, 'Priimek59')).toBeVisible();
  await expect(row(page, 'Priimek49')).toHaveCount(0);
});

test('AC-009-06 a search without a match shows "No contacts match" and "Clear filters" restores the list', async ({
  page,
}) => {
  const { owner, groupId } = await scene('Konferenca 2026');
  await owner.contact(groupId, { firstName: 'Ana', lastName: 'Novak' });
  await openGroup(page, ANA, 'Konferenca 2026');
  await search(page).fill('zzzz');
  await expect(page.getByRole('heading', { name: 'No contacts match' })).toBeVisible();
  await page.getByRole('button', { name: 'Clear filters' }).first().click();
  await expect(row(page, 'Novak')).toBeVisible();
  await expect(search(page)).toHaveValue('');
});

test('AC-009-07 a new group shows the empty state with "Add contact" for the Owner and without it for a Viewer', async ({
  page,
  browser,
}) => {
  await scene('Konferenca 2026', [{ person: PETRA, role: 'VIEWER' }]);
  await openGroup(page, ANA, 'Konferenca 2026');
  await expect(page.getByRole('heading', { name: 'No contacts yet' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Add contact' }).first()).toBeVisible();
  const viewerContext = await browser.newContext({ baseURL: STACK_URL });
  await viewerContext.addInitScript(() => localStorage.setItem('language', 'en'));
  const viewer = await viewerContext.newPage();
  await openGroup(viewer, PETRA, 'Konferenca 2026');
  await expect(viewer.getByRole('heading', { name: 'No contacts yet' })).toBeVisible();
  await expect(viewer.getByRole('button', { name: 'Add contact' })).toHaveCount(0);
  await viewerContext.close();
});

test('AC-009-10 sorting on page 2 of a search keeps the search text', async ({ page }) => {
  const { owner, groupId } = await scene('Konferenca 2026');
  await fill(owner, groupId, 30, (i) => ({
    lastName: 'Novak',
    firstName: `N${i}`,
    city: `Mesto${String(29 - i).padStart(2, '0')}`,
  }));
  await owner.contact(groupId, { lastName: 'Horvat' });
  await openGroup(page, ANA, 'Konferenca 2026');
  await search(page).fill('novak');
  await page.getByRole('button', { name: '2', exact: true }).click();
  await page.getByRole('columnheader', { name: /City/ }).click();
  await expect(search(page)).toHaveValue('novak');
  await expect(page.getByText('30 contacts')).toBeVisible();
  await expect(row(page, 'Horvat')).toHaveCount(0);
});

test('AC-009-11 a Viewer searches and sorts like an Editor', async ({ page }) => {
  const { owner, groupId } = await scene('Konferenca 2026', [{ person: PETRA, role: 'VIEWER' }]);
  await owner.contact(groupId, { firstName: 'Ana', lastName: 'Novak' });
  await owner.contact(groupId, { firstName: 'Eva', lastName: 'Horvat' });
  await openGroup(page, PETRA, 'Konferenca 2026');
  await search(page).fill('novak');
  await expect(row(page, 'Novak')).toBeVisible();
  await expect(row(page, 'Horvat')).toHaveCount(0);
  await search(page).fill('');
  await page.getByRole('columnheader', { name: /Last name/ }).click();
  await expect(row(page, 'Horvat')).toBeVisible();
});

test('AC-010-08 a filter that shows nothing is cleared with "Clear filters"', async ({ page }) => {
  const { owner, groupId } = await scene('Konferenca 2026');
  await owner.contact(groupId, { lastName: 'Novak' });
  await openGroup(page, ANA, 'Konferenca 2026');
  await addCondition(page, 'Has all of these tags', { Tags: 'nobody' });
  await expect(page.getByRole('heading', { name: 'No contacts match' })).toBeVisible();
  await page.getByRole('button', { name: 'Clear filters' }).first().click();
  await expect(row(page, 'Novak')).toBeVisible();
});

test('AC-010-09 an active condition is shown as a chip with a control to remove it', async ({ page }) => {
  const { owner, groupId } = await scene('Konferenca 2026');
  await owner.contact(groupId, { lastName: 'A', tags: ['x'] });
  await owner.contact(groupId, { lastName: 'B' });
  await openGroup(page, ANA, 'Konferenca 2026');
  await addCondition(page, 'Has all of these tags', { Tags: 'x' });
  await expect(page.getByText('Filters:')).toBeVisible();
  await expect(page.getByText('Has all of: x')).toBeVisible();
  await expect(row(page, 'B')).toHaveCount(0);
  await page
    .getByRole('button', { name: /×|Remove/ })
    .first()
    .click();
  await expect(page.getByText('Has all of: x')).toHaveCount(0);
  await expect(row(page, 'B')).toBeVisible();
});

test('AC-010-10 the filter dialog offers exactly the seven conditions and no way to save a filter', async ({
  page,
}) => {
  await scene('Konferenca 2026');
  await openGroup(page, ANA, 'Konferenca 2026');
  await page.getByRole('button', { name: 'Filter' }).click();
  const options = (await dialog(page).getByLabel('Condition').locator('option').allTextContents()).filter(
    (t) => !/^Choose/.test(t),
  );
  expect(options).toEqual([
    'Has all of these tags',
    'Has any of these tags',
    'Does not have tag',
    'Attribute key equals value',
    'Attribute key exists',
    'Country',
    'Company',
  ]);
  await expect(dialog(page).getByRole('button', { name: /save/i })).toHaveCount(0);
});

test('AC-011-01 a filtered selection of 2 contacts is exported to a dated file', async ({ page }) => {
  const { owner, groupId } = await scene('Konferenca 2026', [{ person: MARKO, role: 'EDITOR' }]);
  for (const l of ['A', 'B', 'C']) await owner.contact(groupId, { lastName: l, tags: ['udelezenec2026'] });
  await owner.contact(groupId, { lastName: 'D', tags: ['udelezenec2026', 'neObvescaj'] });
  await owner.contact(groupId, { lastName: 'E' });
  await openGroup(page, MARKO, 'Konferenca 2026');
  await addCondition(page, 'Has all of these tags', { Tags: 'udelezenec2026' });
  await addCondition(page, 'Does not have tag', { Tag: 'neObvescaj' });
  await expect(page.getByText('3 contacts')).toBeVisible();
  await page.getByRole('button', { name: 'Select all 3 results' }).click();
  const download = page.waitForEvent('download');
  await page.getByRole('button', { name: 'Export', exact: true }).click();
  const file = await download;
  expect(file.suggestedFilename()).toMatch(/^Konferenca 2026_\d{4}-\d{2}-\d{2}\.xlsx$/);
});

test('AC-011-05 with nothing ticked Export is disabled', async ({ page }) => {
  const { owner, groupId } = await scene('Konferenca 2026');
  await owner.contact(groupId, { lastName: 'Novak' });
  await openGroup(page, ANA, 'Konferenca 2026');
  await expect(page.getByRole('button', { name: 'Export', exact: true })).toBeDisabled();
  await expect(page.getByText('Nothing selected')).toBeVisible();
});

test('AC-011-06 the selection is kept over two pages: 3 on page 1 and 2 on page 2 give 5', async ({
  page,
}) => {
  const { owner, groupId } = await scene('Konferenca 2026');
  await fill(owner, groupId, 60, (i) => ({ firstName: 'Ime', lastName: `P${String(i).padStart(2, '0')}` }));
  await openGroup(page, ANA, 'Konferenca 2026');
  const boxes = page.getByRole('checkbox', { name: 'Select contact' });
  for (let i = 0; i < 3; i++) await boxes.nth(i).check();
  await page.getByRole('button', { name: '2', exact: true }).click();
  for (let i = 0; i < 2; i++) await boxes.nth(i).check();
  await expect(page.getByText('5 selected')).toBeVisible();
});

test('AC-011-07 adding a filter clears the selection', async ({ page }) => {
  const { owner, groupId } = await scene('Konferenca 2026');
  await fill(owner, groupId, 5, (i) => ({ lastName: `P${i}`, tags: ['x'] }));
  await openGroup(page, ANA, 'Konferenca 2026');
  await page.getByLabel('Select all on this page').check();
  await expect(page.getByText('5 selected')).toBeVisible();
  await addCondition(page, 'Has all of these tags', { Tags: 'x' });
  await expect(page.getByText('Nothing selected')).toBeVisible();
});

test('AC-011-09 too many contacts: the refusal is shown and no file is downloaded', async ({ page }) => {
  const { owner, groupId } = await scene('Konferenca 2026');
  await owner.contact(groupId, { lastName: 'Novak' });
  await page.route('**/api/groups/*/exports', (route) =>
    route.fulfill({
      status: 400,
      json: {
        status: 400,
        code: 'VALIDATION_FAILED',
        message: 'Too many contacts.',
        errors: [{ field: 'contactIds', code: 'TOO_MANY_CONTACTS', message: 'Too many' }],
      },
    }),
  );
  await openGroup(page, ANA, 'Konferenca 2026');
  await page.getByLabel('Select all on this page').check();
  let downloaded = false;
  page.on('download', () => (downloaded = true));
  await page.getByRole('button', { name: 'Export', exact: true }).click();
  await expect(
    page.getByText('You selected more than 10,000 contacts. Narrow the selection to export.'),
  ).toBeVisible();
  expect(downloaded).toBe(false);
});

test('AC-011-10 a Viewer sees no tick boxes and no Export button', async ({ page }) => {
  const { owner, groupId } = await scene('Konferenca 2026', [{ person: PETRA, role: 'VIEWER' }]);
  await owner.contact(groupId, { lastName: 'Novak' });
  await openGroup(page, PETRA, 'Konferenca 2026');
  await expect(row(page, 'Novak')).toBeVisible();
  await expect(page.getByRole('checkbox')).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Export' })).toHaveCount(0);
});
