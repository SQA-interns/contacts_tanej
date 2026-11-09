import { ANA, MARKO, STACK_URL, dialog, expect, row, scene, signIn, test } from '../acceptance/support';

/**
 * E2E-08 J8 Group deletion (US-015, US-013): the Owner deletes the group by typing its name, the group
 * disappears for the members, appears in "Deleted groups" for the Owners, and is restored.
 */
test('E2E-08 J8 Group deletion', async ({ page, browser }) => {
  const { owner, groupId } = await scene('Konferenca 2026', [{ person: MARKO, role: 'EDITOR' }]);
  await owner.contact(groupId, { firstName: 'Ana', lastName: 'Novak' });

  await signIn(page, ANA);
  await row(page, 'Konferenca 2026').getByRole('button', { name: 'Delete group' }).click();
  await dialog(page).getByLabel('Type the group name to confirm').fill('Konferenca 2026');
  await dialog(page).getByRole('button', { name: 'Delete group' }).click();
  await expect(page.getByRole('link', { name: 'Konferenca 2026' })).toHaveCount(0);

  const marko = await (await browser.newContext({ baseURL: STACK_URL })).newPage();
  await marko.context().addInitScript(() => localStorage.setItem('language', 'en'));
  await signIn(marko, MARKO);
  await expect(marko.getByRole('link', { name: 'Konferenca 2026' })).toHaveCount(0);
  await marko.goto(`/groups/${groupId}`);
  await expect(marko.getByRole('heading', { name: 'Group not found' }).first()).toBeVisible();

  await page.getByRole('link', { name: 'Deleted groups' }).click();
  await expect(row(page, 'Konferenca 2026')).toBeVisible();
  await row(page, 'Konferenca 2026').getByRole('button', { name: 'Restore' }).click();
  await expect(page.getByText('The group was restored.')).toBeVisible();

  await page.getByRole('link', { name: 'Groups' }).first().click();
  await expect(page.getByRole('link', { name: 'Konferenca 2026' })).toBeVisible();
  await marko.goto('/');
  await expect(marko.getByRole('link', { name: 'Konferenca 2026' })).toBeVisible();
});
