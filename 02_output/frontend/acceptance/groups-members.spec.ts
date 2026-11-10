import {
  ANA,
  BORIS,
  IVAN,
  MARKO,
  PETRA,
  Client,
  createAccount,
  dialog,
  expect,
  openGroup,
  openTab,
  row,
  scene,
  signIn,
  test,
  verifyEmail,
} from './support';

/**
 * The screens of US-002 (WF-05, WF-06), US-003 and US-004 (WF-07), US-015 (WF-05, WF-18) and US-016
 * (WF-19): what the API acceptance tests cannot show (messages, dialogs, missing controls).
 */

test('AC-002-01 creating a group in the dialog opens it empty and shows Ana as its Owner', async ({
  page,
}) => {
  await createAccount(ANA);
  await signIn(page, ANA);
  await page.getByRole('button', { name: 'Create group' }).click();
  await page.getByLabel('Group name').fill('Konferenca 2026');
  await dialog(page).getByRole('button', { name: 'Create group' }).click();
  await expect(page.getByRole('heading', { name: 'Konferenca 2026' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'No contacts yet' })).toBeVisible();
});

test('AC-002-03 the address of a group of someone else shows "Group not found" and nothing of the group', async ({
  page,
}) => {
  const boris = await Client.of(BORIS);
  const poroka = await boris.group('Poroka');
  await scene('Konferenca 2026');
  await signIn(page, ANA);
  await page.goto(`/groups/${poroka}`);
  await expect(page.getByRole('heading', { name: 'Group not found' }).first()).toBeVisible();
  await expect(page.getByText('Poroka')).toHaveCount(0);
});

test('AC-002-04 a name of spaces only shows the reason in the dialog and creates no group', async ({
  page,
}) => {
  await createAccount(ANA);
  await signIn(page, ANA);
  await page.getByRole('button', { name: 'Create group' }).click();
  await page.getByLabel('Group name').fill('   ');
  await dialog(page).getByRole('button', { name: 'Create group' }).click();
  await expect(dialog(page).getByRole('alert')).toBeVisible();
  await expect(dialog(page)).toBeVisible();
  const owner = await Client.of(ANA);
  expect(await owner.ok('GET', '/groups')).toEqual([]);
});

test('AC-002-05 a second group with the same name ignoring case is refused with the reason', async ({
  page,
}) => {
  await scene('Konferenca 2026');
  await signIn(page, ANA);
  await page.getByRole('button', { name: 'Create group' }).first().click();
  await page.getByLabel('Group name').fill('KONFERENCA 2026');
  await dialog(page).getByRole('button', { name: 'Create group' }).click();
  await expect(dialog(page).getByText('You already have a group with this name.')).toBeVisible();
  await expect(dialog(page).getByLabel('Group name')).toHaveValue('KONFERENCA 2026');
});

test('AC-003-08 an invited person with an unverified e-mail sees only the verify screen until verified', async ({
  page,
}) => {
  const { owner, groupId } = await scene('Konferenca 2026');
  await owner.ok('POST', `/groups/${groupId}/invitations`, { email: IVAN.email, role: 'VIEWER' });
  await createAccount(IVAN, { verified: false });
  await signIn(page, IVAN);
  await expect(page.getByRole('heading', { name: 'Verify your e-mail' })).toBeVisible();
  await expect(page.getByText('Konferenca 2026')).toHaveCount(0);
  await verifyEmail(IVAN.email);
  await page.getByRole('button', { name: 'Sign out' }).click();
  await signIn(page, IVAN);
  await expect(page.getByRole('link', { name: 'Konferenca 2026' })).toBeVisible();
});

test('AC-003-01 the Owner invites two people and the Members tab lists them as pending with role and expiry', async ({
  page,
}) => {
  await scene('Konferenca 2026');
  await openGroup(page, ANA, 'Konferenca 2026');
  await openTab(page, 'Members');
  for (const [email, role] of [
    ['marko@example.com', 'Editor'],
    ['petra@example.com', 'Viewer'],
  ] as const) {
    await page.getByLabel('E-mail').fill(email);
    await page.getByLabel('Role', { exact: true }).selectOption({ label: role });
    await page.getByRole('button', { name: 'Invite' }).click();
    await expect(row(page, email)).toContainText(role);
  }
  await expect(row(page, 'marko@example.com')).toContainText('Expires');
  await expect(page.getByRole('button', { name: 'Cancel invitation' })).toHaveCount(2);
});

test('AC-003-03 and AC-003-04 inviting a member or a pending address shows the message and changes nothing', async ({
  page,
}) => {
  const { owner, groupId } = await scene('Konferenca 2026', [{ person: MARKO, role: 'EDITOR' }]);
  await owner.ok('POST', `/groups/${groupId}/invitations`, { email: PETRA.email, role: 'VIEWER' });
  await openGroup(page, ANA, 'Konferenca 2026');
  await openTab(page, 'Members');
  await page.getByLabel('E-mail').fill('Marko@Example.com');
  await page.getByRole('button', { name: 'Invite' }).click();
  await expect(page.getByText('This person is already a member of the group.')).toBeVisible();
  await page.getByLabel('E-mail').fill('petra@example.com');
  await page.getByRole('button', { name: 'Invite' }).click();
  await expect(page.getByText('An invitation for this address is already pending.')).toBeVisible();
});

test('AC-003-05 the 21st invitation shows the message about the limit of 20', async ({ page }) => {
  const { owner, groupId } = await scene('Konferenca 2026');
  for (let i = 1; i <= 20; i++)
    await owner.ok('POST', `/groups/${groupId}/invitations`, { email: `p${i}@example.com`, role: 'VIEWER' });
  await openGroup(page, ANA, 'Konferenca 2026');
  await openTab(page, 'Members');
  await expect(page.getByText('The limit of 20 pending invitations is reached.')).toBeVisible();
});

test('AC-003-10 an invalid e-mail is refused as invalid and nothing is created', async ({ page }) => {
  await scene('Konferenca 2026');
  await openGroup(page, ANA, 'Konferenca 2026');
  await openTab(page, 'Members');
  await page.getByLabel('E-mail').fill('marko@@example');
  await page.getByRole('button', { name: 'Invite' }).click();
  await expect(page.getByRole('alert').first()).toBeVisible();
  await expect(page.getByRole('button', { name: 'Cancel invitation' })).toHaveCount(0);
});

test('AC-004-01 a Viewer sees the members and no controls to manage them or to edit', async ({ page }) => {
  const { owner, groupId } = await scene('Konferenca 2026', [{ person: MARKO, role: 'EDITOR' }]);
  await owner.ok(
    'PUT',
    `/groups/${groupId}/members/${(await owner.ok('GET', `/groups/${groupId}/members`)).find((m: { email: string }) => m.email === MARKO.email).userId}`,
    { role: 'VIEWER' },
  );
  await openGroup(page, MARKO, 'Konferenca 2026');
  await expect(page.getByRole('button', { name: 'Add contact' })).toHaveCount(0);
  await openTab(page, 'Members');
  await expect(page.getByRole('button', { name: 'Invite' })).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Remove' })).toHaveCount(0);
  await expect(
    page.getByRole('navigation', { name: 'Group' }).getByRole('link', { name: 'History' }),
  ).toHaveCount(0);
});

test('AC-004-03 the only Owner who tries to leave gets the last-Owner message and stays Owner', async ({
  page,
}) => {
  await scene('Konferenca 2026', [{ person: MARKO, role: 'EDITOR' }]);
  await openGroup(page, ANA, 'Konferenca 2026');
  await openTab(page, 'Members');
  await page.getByRole('button', { name: 'Leave group' }).click();
  await dialog(page).getByRole('button', { name: 'Leave group' }).click();
  await expect(
    page.getByText('A group needs at least one Owner. Make another member an Owner first.'),
  ).toBeVisible();
  await expect(row(page, 'Ana Novak')).toContainText('Owner');
});

test('AC-004-05 a Viewer leaves the group and it disappears from the list', async ({ page }) => {
  await scene('Konferenca 2026', [{ person: PETRA, role: 'VIEWER' }]);
  await openGroup(page, PETRA, 'Konferenca 2026');
  await openTab(page, 'Members');
  await page.getByRole('button', { name: 'Leave group' }).click();
  await dialog(page).getByRole('button', { name: 'Leave group' }).click();
  await expect(page.getByRole('link', { name: 'Konferenca 2026' })).toHaveCount(0);
});

test('EX-004-03 a removed member whose browser still shows the group gets "not found" and the group list', async ({
  page,
}) => {
  const { owner, groupId } = await scene('Konferenca 2026', [{ person: MARKO, role: 'EDITOR' }]);
  await openGroup(page, MARKO, 'Konferenca 2026');
  const members = await owner.ok('GET', `/groups/${groupId}/members`);
  const marko = members.find((m: { email: string }) => m.email === MARKO.email);
  await owner.ok('DELETE', `/groups/${groupId}/members/${marko.userId}`);
  await page.reload();
  await expect(page.getByRole('heading', { name: 'Group not found' }).first()).toBeVisible();
  await page.getByRole('link', { name: 'Groups' }).first().click();
  await expect(page.getByRole('link', { name: 'Konferenca 2026' })).toHaveCount(0);
});

test('AC-015-01 deleting a group by typing its name hides it and lists it in Deleted groups', async ({
  page,
}) => {
  await scene('Konferenca 2026', [{ person: MARKO, role: 'EDITOR' }]);
  await signIn(page, ANA);
  await row(page, 'Konferenca 2026').getByRole('button', { name: 'Delete group' }).click();
  await dialog(page).getByLabel('Type the group name to confirm').fill('Konferenca 2026');
  await dialog(page).getByRole('button', { name: 'Delete group' }).click();
  await expect(page.getByRole('link', { name: 'Konferenca 2026' })).toHaveCount(0);
  await page.getByRole('link', { name: 'Deleted groups' }).click();
  await expect(row(page, 'Konferenca 2026')).toBeVisible();
  await expect(row(page, 'Konferenca 2026')).toContainText('30');
});

test('AC-015-02 a name typed in another case deletes nothing', async ({ page }) => {
  await scene('Konferenca 2026');
  await signIn(page, ANA);
  await row(page, 'Konferenca 2026').getByRole('button', { name: 'Delete group' }).click();
  await dialog(page).getByLabel('Type the group name to confirm').fill('konferenca 2026');
  await dialog(page).getByRole('button', { name: 'Delete group' }).click();
  await expect(dialog(page)).toBeVisible();
  await page.keyboard.press('Escape');
  await expect(page.getByRole('link', { name: 'Konferenca 2026' })).toBeVisible();
});

test('AC-016-02 the only Owner who deletes the account is refused and the group is named', async ({
  page,
}) => {
  await scene('Konferenca 2026');
  await signIn(page, ANA);
  await page.getByRole('link', { name: 'Account' }).click();
  await page.getByRole('button', { name: 'Delete account' }).first().click();
  await dialog(page).getByRole('button', { name: 'Delete account' }).click();
  await expect(page.getByText('You are the only Owner of these groups.')).toBeVisible();
  await expect(page.getByText('Konferenca 2026')).toBeVisible();
});

test('AC-016-05 cancelling the confirmation deletes nothing', async ({ page }) => {
  await createAccount(PETRA);
  await signIn(page, PETRA);
  await page.getByRole('link', { name: 'Account' }).click();
  await page.getByRole('button', { name: 'Delete account' }).first().click();
  await dialog(page).getByRole('button', { name: 'Cancel' }).click();
  await expect(dialog(page)).toHaveCount(0);
  await page.reload();
  await expect(page.getByRole('heading', { name: 'Account' })).toBeVisible();
  const again = await Client.of(PETRA);
  expect((await again.call('GET', '/me')).status).toBe(200);
});

test('AC-016-06 an account deletion without a recent sign-in asks to sign in again first', async ({
  page,
}) => {
  await createAccount(BORIS);
  await signIn(page, BORIS);
  await page.getByRole('link', { name: 'Account' }).click();
  // the sign-in of the session is older than five minutes: the token of the emulator is aged by the test
  await page.route('**/api/me', async (route) => {
    if (route.request().method() === 'DELETE') {
      await route.fulfill({
        status: 403,
        json: { status: 403, code: 'RECENT_LOGIN_REQUIRED', message: 'Sign in again.' },
      });
    } else {
      await route.continue();
    }
  });
  await page.getByRole('button', { name: 'Delete account' }).first().click();
  await dialog(page).getByRole('button', { name: 'Delete account' }).click();
  await expect(page.getByRole('heading', { name: 'Sign in again to do this' })).toBeVisible();
});
