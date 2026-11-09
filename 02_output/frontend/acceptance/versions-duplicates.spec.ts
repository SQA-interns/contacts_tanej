import { ANA, MARKO, PETRA, dialog, expect, openGroup, openTab, row, scene, test } from './support';
import type { Page } from '@playwright/test';

/** Versions (WF-12), Duplicates (WF-16) and Merge (WF-17): US-020, US-021, US-022, the parts only the browser shows. */

const group = (page: Page) => page.getByRole('navigation', { name: 'Group' });
const contactTab = (page: Page, name: string) =>
  page.getByRole('tab', { name }).or(page.getByRole('link', { name }));

/** Ana Novak with versions 1 (phone 040), 2 (phone 041) and 3 (tag b). */
async function threeVersions(groupId: string, owner: import('./support').Client) {
  const contact = await owner.contact(groupId, {
    firstName: 'Ana',
    lastName: 'Novak',
    phone: '+386 40 123 456',
    tags: ['a'],
  });
  let current = await owner.ok('GET', `/groups/${groupId}/contacts/${contact.id}`);
  current = await owner.ok('PUT', `/groups/${groupId}/contacts/${contact.id}`, {
    ...current,
    phone: '+386 41 555 123',
  });
  await owner.ok('PUT', `/groups/${groupId}/contacts/${contact.id}`, { ...current, tags: ['a', 'b'] });
  return contact.id;
}

async function openVersions(page: Page, person: typeof MARKO) {
  await openGroup(page, person, 'Konferenca 2026');
  await row(page, 'Novak').getByRole('link', { name: 'Novak' }).click();
  await contactTab(page, 'Versions').click();
}

test('AC-020-02 a version opens read-only', async ({ page }) => {
  const { owner, groupId } = await scene('Konferenca 2026', [{ person: MARKO, role: 'EDITOR' }]);
  await threeVersions(groupId, owner);
  await openVersions(page, MARKO);
  await row(page, 'Version 1').getByRole('button', { name: 'View' }).click();
  await expect(page.getByText('+386 40 123 456')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Save', exact: true })).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Restore this version' })).toBeVisible();
});

test('AC-020-03 comparing version 1 with the current contact shows the phone difference', async ({
  page,
}) => {
  const { owner, groupId } = await scene('Konferenca 2026', [{ person: MARKO, role: 'EDITOR' }]);
  await threeVersions(groupId, owner);
  await openVersions(page, MARKO);
  await row(page, 'Version 1').getByRole('button', { name: 'Compare' }).click();
  await expect(page.getByRole('heading', { name: 'Compare versions' })).toBeVisible();
  await expect(page.getByText('Highlighted rows differ.')).toBeVisible();
  await expect(row(page, 'Phone')).toContainText('+386 40 123 456');
  await expect(row(page, 'Phone')).toContainText('+386 41 555 123');
});

test('AC-020-08 cancelling the restore dialog changes nothing', async ({ page }) => {
  const { owner, groupId } = await scene('Konferenca 2026', [{ person: MARKO, role: 'EDITOR' }]);
  const id = await threeVersions(groupId, owner);
  await openVersions(page, MARKO);
  await row(page, 'Version 1').getByRole('button', { name: 'Restore' }).click();
  await dialog(page).getByRole('button', { name: 'Cancel' }).click();
  await expect(dialog(page)).toHaveCount(0);
  expect(await owner.ok('GET', `/groups/${groupId}/contacts/${id}/versions`)).toHaveLength(3);
});

test('AC-020-04 confirming the restore brings back version 1 and records version 4', async ({ page }) => {
  const { owner, groupId } = await scene('Konferenca 2026', [{ person: MARKO, role: 'EDITOR' }]);
  const id = await threeVersions(groupId, owner);
  await openVersions(page, MARKO);
  await row(page, 'Version 1').getByRole('button', { name: 'Restore' }).click();
  await dialog(page).getByRole('button', { name: 'Restore this version' }).click();
  await expect(page.getByText('Version 4')).toBeVisible();
  const contact = await owner.ok('GET', `/groups/${groupId}/contacts/${id}`);
  expect(contact.phone).toBe('+386 40 123 456');
  expect(contact.tags).toEqual(['a']);
});

test('AC-020-06 restoring a version equal to the current contact says there is nothing to change', async ({
  page,
}) => {
  const { owner, groupId } = await scene('Konferenca 2026', [{ person: MARKO, role: 'EDITOR' }]);
  const id = await threeVersions(groupId, owner);
  await openVersions(page, MARKO);
  await row(page, 'Version 3').getByRole('button', { name: 'Restore' }).click();
  await dialog(page).getByRole('button', { name: 'Restore this version' }).click();
  await expect(
    page.getByText('There is nothing to change: the contact already has these values.'),
  ).toBeVisible();
  expect(await owner.ok('GET', `/groups/${groupId}/contacts/${id}/versions`)).toHaveLength(3);
});

test('AC-020-07 a Viewer has no Versions tab', async ({ page }) => {
  const { owner, groupId } = await scene('Konferenca 2026', [{ person: PETRA, role: 'VIEWER' }]);
  await threeVersions(groupId, owner);
  await openGroup(page, PETRA, 'Konferenca 2026');
  await row(page, 'Novak').getByRole('link', { name: 'Novak' }).click();
  await expect(contactTab(page, 'Versions')).toHaveCount(0);
});

test('AC-021-11 a Viewer has no Duplicates tab and no "Find duplicates" button', async ({ page }) => {
  await scene('Konferenca 2026', [{ person: PETRA, role: 'VIEWER' }]);
  await openGroup(page, PETRA, 'Konferenca 2026');
  await expect(group(page).getByRole('link', { name: 'Duplicates' })).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Find duplicates' })).toHaveCount(0);
});

test('AC-021-12 without duplicates the search says "No duplicates found"', async ({ page }) => {
  const { owner, groupId } = await scene('Konferenca 2026');
  await owner.contact(groupId, { lastName: 'A', email: 'a@example.com' });
  await owner.contact(groupId, { lastName: 'B', email: 'b@example.com' });
  await openGroup(page, ANA, 'Konferenca 2026');
  await openTab(page, 'Duplicates');
  await page.getByRole('button', { name: 'Find duplicates' }).first().click();
  await expect(page.getByRole('heading', { name: 'No duplicates found' })).toBeVisible();
});

test('AC-021-01 two contacts with the same e-mail are shown as one certain set with the reason', async ({
  page,
}) => {
  const { owner, groupId } = await scene('Konferenca 2026');
  await owner.contact(groupId, { lastName: 'Novak', email: 'ana.novak@example.com' });
  await owner.contact(groupId, { lastName: 'Nova', email: 'ANA.novak@example.com' });
  await openGroup(page, ANA, 'Konferenca 2026');
  await openTab(page, 'Duplicates');
  await page.getByRole('button', { name: 'Find duplicates' }).first().click();
  await expect(page.getByText('Certain')).toBeVisible();
  await expect(page.getByText('Same e-mail address')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Not a duplicate' })).toBeVisible();
});

async function toMergeScreen(page: Page, groupId: string, owner: import('./support').Client) {
  await owner.contact(groupId, {
    firstName: 'Ana',
    lastName: 'Novak',
    email: 'ana@example.com',
    company: 'Acme',
    tags: ['a'],
  });
  await owner.contact(groupId, {
    firstName: 'Ana',
    lastName: 'Novak',
    email: 'ana@example.com',
    phone: '+386 41 555 123',
    tags: ['b'],
  });
  await openGroup(page, ANA, 'Konferenca 2026');
  await openTab(page, 'Duplicates');
  await page.getByRole('button', { name: 'Find duplicates' }).first().click();
  await page.getByRole('button', { name: 'Merge' }).first().click();
}

test('AC-022-01 the merge screen shows both contacts side by side and preselects the first non-empty values', async ({
  page,
}) => {
  const { owner, groupId } = await scene('Konferenca 2026');
  await toMergeScreen(page, groupId, owner);
  await expect(page.getByRole('heading', { name: 'Merge contacts' })).toBeVisible();
  await expect(page.getByText('Survivor (the oldest, stays)')).toBeVisible();
  await expect(page.getByText('Contact to join')).toBeVisible();
  await page.getByRole('button', { name: 'Confirm merge' }).click();
  await expect
    .poll(async () => {
      const found = await owner.ok('POST', `/groups/${groupId}/contacts/search`, {});
      return found.total;
    })
    .toBe(1);
  const list = await owner.ok('POST', `/groups/${groupId}/contacts/search`, {});
  expect(list.items[0].phone).toBe('+386 41 555 123');
  expect(list.items[0].company).toBe('Acme');
});

test('AC-022-07 the merge screen says that the survivor can be reverted by restoring an earlier version', async ({
  page,
}) => {
  const { owner, groupId } = await scene('Konferenca 2026');
  await toMergeScreen(page, groupId, owner);
  await expect(
    page.getByText(/You can revert a merge later from the contact.s Versions tab\./),
  ).toBeVisible();
});

test('AC-022-13 cancelling the merge screen changes nothing', async ({ page }) => {
  const { owner, groupId } = await scene('Konferenca 2026');
  await toMergeScreen(page, groupId, owner);
  await page.getByRole('button', { name: 'Cancel' }).click();
  expect((await owner.ok('POST', `/groups/${groupId}/contacts/search`, {})).total).toBe(2);
});

test('AC-022-03 with the same key and different values Confirm stays disabled until a value is chosen', async ({
  page,
}) => {
  const { owner, groupId } = await scene('Konferenca 2026');
  await owner.contact(groupId, {
    lastName: 'Novak',
    email: 'ana@example.com',
    attributes: [{ key: 'kompetenca', value: 'java' }],
  });
  await owner.contact(groupId, {
    lastName: 'Novak',
    email: 'ana@example.com',
    attributes: [{ key: 'Kompetenca', value: 'python' }],
  });
  await openGroup(page, ANA, 'Konferenca 2026');
  await openTab(page, 'Duplicates');
  await page.getByRole('button', { name: 'Find duplicates' }).first().click();
  await page.getByRole('button', { name: 'Merge' }).first().click();
  await expect(page.getByRole('heading', { name: 'Attribute conflicts' })).toBeVisible();
  const confirm = page.getByRole('button', { name: 'Confirm merge' });
  await expect(confirm).toBeDisabled();
  await page.getByRole('radio', { name: 'python' }).check();
  await expect(confirm).toBeEnabled();
});
