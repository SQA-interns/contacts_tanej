import { MARKO, dialog, expect, openGroup, openTab, readXlsx, row, scene, test } from '../acceptance/support';
import { readFileSync } from 'node:fs';

/**
 * E2E-03 J3 Tags and export (US-006, US-010, US-011, US-013): the Editor tags 3 of 5 contacts
 * `udelezenec2026` and one of them `neObvescaj`, filters "has udelezenec2026" and "does not have
 * neObvescaj", selects all results and exports; the file has 2 rows with the right columns and tags, and
 * an export entry appears in the group-wide History.
 */
test('E2E-03 J3 Tags and export', async ({ page }) => {
  const { owner, groupId } = await scene('Konferenca 2026', [{ person: MARKO, role: 'EDITOR' }]);
  for (const last of ['Ana', 'Boris', 'Cene', 'Dora', 'Eva'])
    await owner.contact(groupId, { firstName: 'Ime', lastName: last });

  await openGroup(page, MARKO, 'Konferenca 2026');
  const tags: Record<string, string[]> = {
    Ana: ['udelezenec2026'],
    Boris: ['udelezenec2026', 'neObvescaj'],
    Cene: ['udelezenec2026'],
  };
  for (const [last, names] of Object.entries(tags)) {
    await row(page, last).getByRole('link', { name: last }).click();
    for (const name of names) {
      await page.getByLabel('Tags', { exact: true }).fill(name);
      await page.keyboard.press('Enter');
    }
    await page.getByRole('button', { name: 'Save', exact: true }).click();
    await expect(page.getByText('The contact was saved.')).toBeVisible();
    await openTab(page, 'Contacts');
  }

  await page.getByRole('button', { name: 'Filter' }).click();
  await dialog(page).getByLabel('Condition').selectOption({ label: 'Has all of these tags' });
  await dialog(page).getByLabel('Tags', { exact: true }).fill('udelezenec2026');
  await page.keyboard.press('Enter');
  await dialog(page).getByRole('button', { name: 'Add condition' }).click();
  await page.getByRole('button', { name: 'Filter' }).click();
  await dialog(page).getByLabel('Condition').selectOption({ label: 'Does not have tag' });
  await dialog(page).getByLabel('Tag', { exact: true }).fill('neObvescaj');
  await dialog(page).getByRole('button', { name: 'Add condition' }).click();
  await expect(page.getByText('2 contacts')).toBeVisible();

  await page.getByRole('button', { name: 'Select all 2 results' }).click();
  const download = page.waitForEvent('download');
  await page.getByRole('button', { name: 'Export', exact: true }).click();
  const file = await download;
  const rows = readXlsx(readFileSync((await file.path()) as string));
  expect(rows).toHaveLength(3);
  expect(rows[0]).toEqual([
    'First name',
    'Last name',
    'E-mail',
    'Phone',
    'Company',
    'Job title',
    'Street and house number',
    'Postal code',
    'City',
    'Country',
    'Notes',
    'Tags',
  ]);
  expect(rows.slice(1).map((r) => r[1])).toEqual(['Ana', 'Cene']);
  expect(rows.slice(1).map((r) => r[11])).toEqual(['udelezenec2026', 'udelezenec2026']);

  await page.goto(`/groups/${groupId}/history`);
  await expect(page.getByText('2 contacts exported').first()).toBeVisible();
  await expect(page.getByText('Marko Pirc').first()).toBeVisible();
});
