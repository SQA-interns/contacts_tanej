import {
  ANA,
  MARKO,
  PETRA,
  STACK_URL,
  dialog,
  expect,
  openGroup,
  openTab,
  row,
  scene,
  test,
} from '../acceptance/support';

/**
 * E2E-07 J7 Leaving (US-004, US-013, US-016): the only Owner tries to leave and is refused; after handing
 * over to another Owner she deletes her account, her name stays in the History and her e-mail is removed;
 * the Owner removes a member whose next request for the group is answered "not found" and for whom the
 * group disappears from his list.
 */
test('E2E-07 J7 Leaving', async ({ page, browser }) => {
  const { owner, groupId } = await scene('Konferenca 2026', [
    { person: MARKO, role: 'EDITOR' },
    { person: PETRA, role: 'VIEWER' },
  ]);
  await owner.contact(groupId, { firstName: 'Eva', lastName: 'Horvat' });

  await openGroup(page, ANA, 'Konferenca 2026');
  await openTab(page, 'Members');
  await page.getByRole('button', { name: 'Leave group' }).click();
  await dialog(page).getByRole('button', { name: 'Leave group' }).click();
  await expect(
    page.getByText('A group needs at least one Owner. Make another member an Owner first.'),
  ).toBeVisible();

  await row(page, 'Marko Pirc').getByLabel('Role').selectOption({ label: 'Owner' });
  await expect(row(page, 'Marko Pirc')).toContainText('Owner');
  await page.getByRole('link', { name: 'Account' }).click();
  await page.getByRole('button', { name: 'Delete account' }).first().click();
  await dialog(page).getByRole('button', { name: 'Delete account' }).click();
  await expect(page.getByRole('heading', { name: 'Sign in' })).toBeVisible();

  const marko = await (await browser.newContext({ baseURL: STACK_URL })).newPage();
  await marko.context().addInitScript(() => localStorage.setItem('language', 'en'));
  await openGroup(marko, MARKO, 'Konferenca 2026');
  await openTab(marko, 'History');
  await expect(marko.getByText('Ana Novak').first()).toBeVisible();
  await expect(marko.getByText('ana.novak@example.com')).toHaveCount(0);

  const petra = await (await browser.newContext({ baseURL: STACK_URL })).newPage();
  await petra.context().addInitScript(() => localStorage.setItem('language', 'en'));
  await openGroup(petra, PETRA, 'Konferenca 2026');

  await openTab(marko, 'Members');
  await row(marko, 'Petra Kos').getByRole('button', { name: 'Remove' }).click();
  await dialog(marko).getByRole('button', { name: 'Remove' }).click();
  await expect(row(marko, 'Petra Kos')).toHaveCount(0);

  await petra.reload();
  await expect(petra.getByRole('heading', { name: 'Group not found' }).first()).toBeVisible();
  await petra.getByRole('link', { name: 'Groups' }).first().click();
  await expect(petra.getByRole('link', { name: 'Konferenca 2026' })).toHaveCount(0);
});
