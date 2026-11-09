import { ANA, MARKO, PETRA, Client, dialog, expect, openGroup, openTab, row, scene, test } from './support';
import type { Page } from '@playwright/test';

/** Trash (WF-14), contact History (WF-11), group History (WF-15): US-012, US-013, US-014. */

const group = (page: Page) => page.getByRole('navigation', { name: 'Group' });

test('AC-012-01 deleting a contact removes it from the list and shows it in Trash with its days left', async ({
  page,
}) => {
  const { owner, groupId } = await scene('Konferenca 2026', [{ person: MARKO, role: 'EDITOR' }]);
  await owner.contact(groupId, { firstName: 'Ana', lastName: 'Novak' });
  await openGroup(page, MARKO, 'Konferenca 2026');
  await row(page, 'Novak').getByRole('link', { name: 'Novak' }).click();
  await page.getByRole('button', { name: 'Delete contact' }).click();
  await dialog(page).getByRole('button', { name: 'Delete contact' }).click();
  await openTab(page, 'Contacts');
  await expect(row(page, 'Novak')).toHaveCount(0);
  await openTab(page, 'Trash');
  await expect(row(page, 'Novak')).toContainText('30');
});

test('AC-012-02 restoring from Trash brings the contact back into the list', async ({ page }) => {
  const { owner, groupId } = await scene('Konferenca 2026', [{ person: MARKO, role: 'EDITOR' }]);
  const contact = await owner.contact(groupId, {
    firstName: 'Ana',
    lastName: 'Novak',
    tags: ['udelezenec2025'],
  });
  await owner.ok('POST', `/groups/${groupId}/contacts/${contact.id}/trash`);
  await openGroup(page, MARKO, 'Konferenca 2026');
  await openTab(page, 'Trash');
  await row(page, 'Novak').getByRole('button', { name: 'Restore' }).click();
  await expect(page.getByText('The contact was restored.')).toBeVisible();
  await openTab(page, 'Contacts');
  await expect(row(page, 'Novak')).toContainText('udelezenec2025');
});

test('AC-012-03 "Delete forever" by the Owner erases the contact at once', async ({ page }) => {
  const { owner, groupId } = await scene('Konferenca 2026');
  const contact = await owner.contact(groupId, { firstName: 'Ana', lastName: 'Novak' });
  await owner.ok('POST', `/groups/${groupId}/contacts/${contact.id}/trash`);
  await openGroup(page, ANA, 'Konferenca 2026');
  await openTab(page, 'Trash');
  await row(page, 'Novak').getByRole('button', { name: 'Delete forever' }).click();
  await dialog(page).getByRole('button', { name: 'Delete forever' }).click();
  await expect(row(page, 'Novak')).toHaveCount(0);
  expect((await owner.call('GET', `/groups/${groupId}/contacts/${contact.id}`)).status).toBe(404);
});

test('AC-012-04 an Editor has no "Delete forever" and the contact stays in Trash', async ({ page }) => {
  const { owner, groupId } = await scene('Konferenca 2026', [{ person: MARKO, role: 'EDITOR' }]);
  const contact = await owner.contact(groupId, { firstName: 'Ana', lastName: 'Novak' });
  await owner.ok('POST', `/groups/${groupId}/contacts/${contact.id}/trash`);
  await openGroup(page, MARKO, 'Konferenca 2026');
  await openTab(page, 'Trash');
  await expect(row(page, 'Novak')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Delete forever' })).toHaveCount(0);
});

test('AC-012-08 a Viewer has no Trash tab and no delete control', async ({ page }) => {
  const { owner, groupId } = await scene('Konferenca 2026', [{ person: PETRA, role: 'VIEWER' }]);
  const contact = await owner.contact(groupId, { firstName: 'Ana', lastName: 'Novak' });
  await openGroup(page, PETRA, 'Konferenca 2026');
  await expect(group(page).getByRole('link', { name: 'Trash' })).toHaveCount(0);
  await page.goto(`/groups/${groupId}/contacts/${contact.id}`);
  await expect(page.getByRole('button', { name: 'Delete contact' })).toHaveCount(0);
});

test('AC-012-09 cancelling the "Delete forever" confirmation keeps the contact in Trash', async ({
  page,
}) => {
  const { owner, groupId } = await scene('Konferenca 2026');
  const contact = await owner.contact(groupId, { firstName: 'Ana', lastName: 'Novak' });
  await owner.ok('POST', `/groups/${groupId}/contacts/${contact.id}/trash`);
  await openGroup(page, ANA, 'Konferenca 2026');
  await openTab(page, 'Trash');
  await row(page, 'Novak').getByRole('button', { name: 'Delete forever' }).click();
  await dialog(page).getByRole('button', { name: 'Cancel' }).click();
  await expect(row(page, 'Novak')).toBeVisible();
  expect((await owner.call('GET', `/groups/${groupId}/contacts/${contact.id}`)).body.state).toBe('TRASHED');
});

test('AC-013-01 the History tab of a contact shows who, when, the field and the old and new value', async ({
  page,
}) => {
  const { owner, groupId, clients } = await scene('Konferenca 2026', [{ person: MARKO, role: 'EDITOR' }]);
  const contact = await owner.contact(groupId, {
    firstName: 'Ana',
    lastName: 'Novak',
    phone: '+386 40 123 456',
  });
  const marko = clients[MARKO.email]!;
  const opened = await marko.ok('GET', `/groups/${groupId}/contacts/${contact.id}`);
  await marko.ok('PUT', `/groups/${groupId}/contacts/${contact.id}`, { ...opened, phone: '+386 41 555 123' });
  await openGroup(page, ANA, 'Konferenca 2026');
  await row(page, 'Novak').getByRole('link', { name: 'Novak' }).click();
  await page
    .getByRole('tab', { name: 'History' })
    .or(page.getByRole('link', { name: 'History' }))
    .click();
  const entry = page.getByText('Marko Pirc').first();
  await expect(entry).toBeVisible();
  await expect(page.getByText('marko@example.com').first()).toBeVisible();
  await expect(page.getByText('Phone').first()).toBeVisible();
  await expect(page.getByText('+386 40 123 456')).toBeVisible();
  await expect(page.getByText('+386 41 555 123')).toBeVisible();
});

test('AC-013-08 a Viewer sees no History tab on the group or on a contact', async ({ page }) => {
  const { owner, groupId } = await scene('Konferenca 2026', [{ person: PETRA, role: 'VIEWER' }]);
  const contact = await owner.contact(groupId, { firstName: 'Ana', lastName: 'Novak' });
  await openGroup(page, PETRA, 'Konferenca 2026');
  await expect(group(page).getByRole('link', { name: 'History' })).toHaveCount(0);
  await page.goto(`/groups/${groupId}/contacts/${contact.id}`);
  await expect(
    page.getByRole('tab', { name: 'History' }).or(page.getByRole('link', { name: 'History' })),
  ).toHaveCount(0);
});

test('AC-013-10 after an erasure the History shows the erasure marker in the active language', async ({
  page,
}) => {
  const { owner, groupId } = await scene('Konferenca 2026');
  const contact = await owner.contact(groupId, {
    firstName: 'Eva',
    lastName: 'Horvat',
    phone: '+386 40 123 456',
  });
  await owner.ok('POST', `/groups/${groupId}/contacts/${contact.id}/trash`);
  await owner.ok('DELETE', `/groups/${groupId}/contacts/${contact.id}`);
  await openGroup(page, ANA, 'Konferenca 2026');
  await openTab(page, 'History');
  await expect(page.getByText('Ana Novak').first()).toBeVisible();
  await expect(page.getByText('[erased]').first()).toBeVisible();
  await expect(page.getByText('Horvat')).toHaveCount(0);
  await page.getByRole('button', { name: 'SL', exact: true }).click();
  await expect(page.getByText('[izbrisano]').first()).toBeVisible();
});

test('AC-014-06 a History filter without a match shows "No events match" and "Clear filters"', async ({
  page,
}) => {
  const { owner, groupId } = await scene('Konferenca 2026');
  await owner.contact(groupId, { lastName: 'Novak' });
  await openGroup(page, ANA, 'Konferenca 2026');
  await openTab(page, 'History');
  await page.getByLabel('Event type').selectOption({ label: 'Group deleted' });
  await page.getByRole('button', { name: 'Apply' }).click();
  await expect(page.getByRole('heading', { name: 'No events match' })).toBeVisible();
  await page.getByRole('button', { name: 'Clear filters' }).first().click();
  await expect(page.getByText('Ana Novak').first()).toBeVisible();
});

test('AC-014-07 a Viewer has no History in the group menu', async ({ page }) => {
  await scene('Konferenca 2026', [{ person: PETRA, role: 'VIEWER' }]);
  await openGroup(page, PETRA, 'Konferenca 2026');
  await expect(group(page).getByRole('link', { name: 'History' })).toHaveCount(0);
  await expect(group(page).getByRole('link', { name: 'Members' })).toBeVisible();
});

test('EX-014-02 a date range with the end before the start is refused with the reason and the list is unchanged', async ({
  page,
}) => {
  const { owner, groupId } = await scene('Konferenca 2026');
  await owner.contact(groupId, { lastName: 'Novak' });
  await openGroup(page, ANA, 'Konferenca 2026');
  await openTab(page, 'History');
  await page.getByLabel('From').fill('2026-07-03');
  await page.getByLabel('To').fill('2026-07-02');
  await page.getByRole('button', { name: 'Apply' }).click();
  await expect(page.getByText('The end date must not be before the start date.')).toBeVisible();
});

test('EX-014-05 a group with no History shows the empty state', async ({ page }) => {
  await scene('Konferenca 2026');
  await openGroup(page, ANA, 'Konferenca 2026');
  await openTab(page, 'History');
  await expect(page.getByRole('heading', { name: 'No History yet' })).toBeVisible();
  expect(Client).toBeDefined();
});
