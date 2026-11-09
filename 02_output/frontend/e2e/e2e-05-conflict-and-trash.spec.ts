import {
  ANA,
  BORIS,
  MARKO,
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
 * E2E-05 J5 Conflict and Trash (US-005, US-012, US-017): two Editors open the same contact, the first
 * saves and the second is refused with "this contact was changed by someone else" and sees the new
 * values; an Editor deletes a contact, sees it in Trash and restores it; an Owner deletes it and uses
 * "Delete forever".
 */
test('E2E-05 J5 Conflict and Trash', async ({ page, browser }) => {
  const { owner, groupId } = await scene('Konferenca 2026', [
    { person: MARKO, role: 'EDITOR' },
    { person: BORIS, role: 'EDITOR' },
  ]);
  await owner.contact(groupId, { firstName: 'Ana', lastName: 'Novak', phone: '+386 40 123 456' });

  await openGroup(page, MARKO, 'Konferenca 2026');
  await row(page, 'Novak').getByRole('link', { name: 'Novak' }).click();
  const boris = await (await browser.newContext({ baseURL: STACK_URL })).newPage();
  await boris.context().addInitScript(() => localStorage.setItem('language', 'en'));
  await openGroup(boris, BORIS, 'Konferenca 2026');
  await row(boris, 'Novak').getByRole('link', { name: 'Novak' }).click();

  await page.getByLabel('Phone').fill('+386 41 555 123');
  await page.getByRole('button', { name: 'Save', exact: true }).click();
  await expect(page.getByText('The contact was saved.')).toBeVisible();

  await boris.getByLabel('Company').fill('Acme');
  await boris.getByRole('button', { name: 'Save', exact: true }).click();
  await expect(boris.getByText('This contact was changed by someone else.')).toBeVisible();
  await expect(boris.getByText('+386 41 555 123')).toBeVisible();

  await page.getByRole('button', { name: 'Delete contact' }).click();
  await dialog(page).getByRole('button', { name: 'Delete contact' }).click();
  await openTab(page, 'Trash');
  await expect(row(page, 'Novak')).toBeVisible();
  await row(page, 'Novak').getByRole('button', { name: 'Restore' }).click();
  await openTab(page, 'Contacts');
  await expect(row(page, 'Novak')).toBeVisible();

  const ana = await (await browser.newContext({ baseURL: STACK_URL })).newPage();
  await ana.context().addInitScript(() => localStorage.setItem('language', 'en'));
  await openGroup(ana, ANA, 'Konferenca 2026');
  await row(ana, 'Novak').getByRole('link', { name: 'Novak' }).click();
  await ana.getByRole('button', { name: 'Delete contact' }).click();
  await dialog(ana).getByRole('button', { name: 'Delete contact' }).click();
  await openTab(ana, 'Trash');
  await row(ana, 'Novak').getByRole('button', { name: 'Delete forever' }).click();
  await dialog(ana).getByRole('button', { name: 'Delete forever' }).click();
  await expect(row(ana, 'Novak')).toHaveCount(0);
});
