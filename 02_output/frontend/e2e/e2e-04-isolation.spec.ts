import { ANA, BORIS, Client, STACK_URL, expect, openGroup, readXlsx, row, test } from '../acceptance/support';
import { readFileSync } from 'node:fs';

/**
 * E2E-04 J4 Isolation (US-002, US-009, US-010, US-011, US-017): two Owners with different groups. Neither
 * sees the other's contacts in the list, the search or an export; each gets "not found" when guessing the
 * other's group address in the browser and 404 when sending the other's group id to the API.
 */
test('E2E-04 J4 Isolation', async ({ page, browser }) => {
  const ana = await Client.of(ANA);
  const boris = await Client.of(BORIS);
  const konferenca = await ana.group('Konferenca 2026');
  const poroka = await boris.group('Poroka');
  await ana.contact(konferenca, { firstName: 'Ana', lastName: 'Novak', tags: ['skupna'] });
  await boris.contact(poroka, { firstName: 'Boris', lastName: 'Tuj', tags: ['skupna'] });

  await openGroup(page, ANA, 'Konferenca 2026');
  await expect(row(page, 'Novak')).toBeVisible();
  await expect(row(page, 'Tuj')).toHaveCount(0);
  await page.getByLabel('Search').fill('tuj');
  await expect(page.getByRole('heading', { name: 'No contacts match' })).toBeVisible();
  await page.getByLabel('Search').fill('');
  await page.getByLabel('Select all on this page').check();
  const download = page.waitForEvent('download');
  await page.getByRole('button', { name: 'Export', exact: true }).click();
  const rows = readXlsx(readFileSync((await (await download).path()) as string));
  expect(rows.flat().join('|')).not.toContain('Tuj');
  expect(rows).toHaveLength(2);
  await page.goto(`/groups/${poroka}`);
  await expect(page.getByRole('heading', { name: 'Group not found' }).first()).toBeVisible();
  await expect(page.getByText('Poroka')).toHaveCount(0);
  expect((await ana.call('POST', `/groups/${poroka}/contacts/search`, {})).status).toBe(404);

  const second = await (await browser.newContext({ baseURL: STACK_URL })).newPage();
  await second.context().addInitScript(() => localStorage.setItem('language', 'en'));
  await openGroup(second, BORIS, 'Poroka');
  await expect(row(second, 'Tuj')).toBeVisible();
  await expect(row(second, 'Novak')).toHaveCount(0);
  await second.getByLabel('Search').fill('novak');
  await expect(second.getByRole('heading', { name: 'No contacts match' })).toBeVisible();
  await second.goto(`/groups/${konferenca}`);
  await expect(second.getByRole('heading', { name: 'Group not found' }).first()).toBeVisible();
  expect((await boris.call('POST', `/groups/${konferenca}/contacts/search`, {})).status).toBe(404);
});
