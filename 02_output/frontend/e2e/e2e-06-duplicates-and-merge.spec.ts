import { MARKO, dialog, expect, openGroup, openTab, row, scene, test } from '../acceptance/support';

/**
 * E2E-06 J6 Duplicates and merge, second increment (US-019, US-020, US-021, US-022): two contacts with
 * the same e-mail are reported as a certain duplicate, the Editor merges them and confirms, the survivor
 * has the chosen values and the combined tags, the other is in Trash, the History shows the merge, and
 * the Editor then restores the survivor's earlier version.
 */
test('E2E-06 J6 Duplicates and merge', async ({ page }) => {
  const { owner, groupId } = await scene('Konferenca 2026', [{ person: MARKO, role: 'EDITOR' }]);
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
    email: 'ANA@example.com',
    phone: '+386 41 555 123',
    tags: ['b'],
  });

  await openGroup(page, MARKO, 'Konferenca 2026');
  await openTab(page, 'Duplicates');
  await page.getByRole('button', { name: 'Find duplicates' }).first().click();
  await expect(page.getByText('Certain')).toBeVisible();
  await expect(page.getByText('Same e-mail address')).toBeVisible();
  await page.getByRole('button', { name: 'Merge' }).first().click();
  await expect(page.getByRole('heading', { name: 'Merge contacts' })).toBeVisible();
  await page.getByRole('button', { name: 'Confirm merge' }).click();

  await openTab(page, 'Contacts');
  await expect(row(page, 'Novak')).toHaveCount(1);
  await expect(row(page, 'Novak')).toContainText('+386 41 555 123');
  await expect(row(page, 'Novak')).toContainText('Acme');
  await expect(row(page, 'Novak')).toContainText('a');
  await expect(row(page, 'Novak')).toContainText('b');
  await openTab(page, 'Trash');
  await expect(row(page, 'Novak')).toHaveCount(1);
  await openTab(page, 'History');
  await expect(page.getByText('Marko Pirc').first()).toBeVisible();

  await openTab(page, 'Contacts');
  await row(page, 'Novak').getByRole('link', { name: 'Novak' }).click();
  await page
    .getByRole('tab', { name: 'Versions' })
    .or(page.getByRole('link', { name: 'Versions' }))
    .click();
  await row(page, 'Version 1').getByRole('button', { name: 'Restore' }).click();
  await dialog(page).getByRole('button', { name: 'Restore this version' }).click();
  await openTab(page, 'Contacts');
  await expect(row(page, 'Novak')).not.toContainText('+386 41 555 123');
});
