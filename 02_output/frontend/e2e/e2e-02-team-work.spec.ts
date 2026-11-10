import {
  ANA,
  MARKO,
  PETRA,
  STACK_URL,
  createAccount,
  expect,
  openGroup,
  openTab,
  row,
  scene,
  signIn,
  test,
} from '../acceptance/support';

/**
 * E2E-02 J2 Team work (US-003, US-004, US-005, US-011, US-013, US-014): the Owner invites an Editor and a
 * Viewer, both sign in and see the group, the Editor edits a phone, the Owner sees the change in the
 * History with the old and new value, the Viewer has no Export, no History and no edit controls.
 */
test('E2E-02 J2 Team work', async ({ page, browser }) => {
  const { owner, groupId } = await scene('Konferenca 2026');
  await owner.contact(groupId, { firstName: 'Ana', lastName: 'Novak', phone: '+386 40 123 456' });
  await createAccount(MARKO);
  await createAccount(PETRA);

  await signIn(page, ANA);
  await page.getByRole('link', { name: 'Konferenca 2026' }).click();
  await openTab(page, 'Members');
  for (const [email, role] of [
    [MARKO.email, 'Editor'],
    [PETRA.email, 'Viewer'],
  ] as const) {
    await page.getByLabel('E-mail').fill(email);
    await page.getByLabel('Role', { exact: true }).selectOption({ label: role });
    await page.getByRole('button', { name: 'Invite' }).click();
    await expect(row(page, email)).toBeVisible();
  }

  const marko = await (await browser.newContext({ baseURL: STACK_URL })).newPage();
  await marko.context().addInitScript(() => localStorage.setItem('language', 'en'));
  await openGroup(marko, MARKO, 'Konferenca 2026');
  await row(marko, 'Novak').getByRole('link', { name: 'Novak' }).click();
  await marko.getByLabel('Phone').fill('+386 41 555 123');
  await marko.getByRole('button', { name: 'Save', exact: true }).click();
  await expect(marko.getByText('The contact was saved.')).toBeVisible();

  const petra = await (await browser.newContext({ baseURL: STACK_URL })).newPage();
  await petra.context().addInitScript(() => localStorage.setItem('language', 'en'));
  await openGroup(petra, PETRA, 'Konferenca 2026');
  await petra.getByLabel('Search').fill('novak');
  await expect(row(petra, 'Novak')).toBeVisible();
  await expect(petra.getByRole('button', { name: 'Export' })).toHaveCount(0);
  await expect(petra.getByRole('link', { name: 'History' })).toHaveCount(0);
  await expect(petra.getByRole('button', { name: 'Add contact' })).toHaveCount(0);
  await expect(petra.getByRole('checkbox')).toHaveCount(0);

  await openTab(page, 'History');
  await expect(page.getByText('Marko Pirc').first()).toBeVisible();
  await expect(page.getByText('+386 40 123 456')).toBeVisible();
  await expect(page.getByText('+386 41 555 123')).toBeVisible();
});
