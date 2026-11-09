import {
  ANA,
  BORIS,
  IVAN,
  PASSWORD,
  Client,
  createAccount,
  emailCodes,
  expect,
  signIn,
  signOut,
  test,
  verifyEmail,
  type Person,
} from './support';
import { EMULATOR_URL } from './support';
import type { Page } from '@playwright/test';

/** US-001 Register and sign in: AC-001-01 to AC-001-08, EX-001-01 to EX-001-07 (wireframes WF-01 to WF-04). */

async function register(page: Page, who: { name: string; email: string; password: string }) {
  await page.goto('/register');
  await page.getByLabel('Name').fill(who.name);
  await page.getByLabel('E-mail').fill(who.email);
  await page.getByLabel('Password').fill(who.password);
  await page.getByRole('button', { name: 'Register', exact: true }).click();
}

/** True while the emulator still knows no account that accepts this e-mail and password. */
async function canSignInWith(person: Person): Promise<boolean> {
  const response = await fetch(
    `${EMULATOR_URL}/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=fake-api-key`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: person.email, password: person.password, returnSecureToken: true }),
    },
  );
  return response.ok;
}

const verifyHeading = (page: Page) => page.getByRole('heading', { name: 'Verify your e-mail' });
const emptyState = (page: Page) => page.getByRole('button', { name: 'Create group' });

test('AC-001-01 registering creates the account, shows the verify screen and has Firebase send the e-mail', async ({
  page,
}) => {
  await register(page, { name: 'Ana Novak', email: ANA.email, password: 'Geslo1234' });
  await expect(verifyHeading(page)).toBeVisible();
  await expect.poll(async () => (await emailCodes(ANA.email, 'VERIFY_EMAIL')).length).toBeGreaterThan(0);
  await expect(page.getByRole('heading', { name: 'Groups' })).toHaveCount(0);
  expect(await canSignInWith(ANA)).toBe(true);
});

test('AC-001-02 an unverified user sees only the verify screen for the list and for a group address', async ({
  page,
}) => {
  const boris = await Client.of(BORIS);
  const groupId = await boris.group('Poroka');
  await createAccount(ANA, { verified: false });
  await signIn(page, ANA);
  await expect(verifyHeading(page)).toBeVisible();
  await page.goto('/');
  await expect(verifyHeading(page)).toBeVisible();
  await page.goto(`/groups/${groupId}`);
  await expect(verifyHeading(page)).toBeVisible();
  await expect(page.getByText('Poroka')).toHaveCount(0);
});

test('AC-001-03 a verified user without a group sees the empty state with the button to create a group', async ({
  page,
}) => {
  await createAccount(ANA);
  await signIn(page, ANA);
  await expect(emptyState(page)).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Create a group' })).toBeVisible();
});

test('AC-001-04 "Sign in with Google" signs the person in and shows the empty state', async ({ page }) => {
  await page.goto('/');
  const popupPromise = page.waitForEvent('popup');
  await page.getByRole('button', { name: 'Sign in with Google' }).click();
  const popup = await popupPromise;
  await popup.getByText('Add new account').click();
  await popup.getByRole('button', { name: 'Auto-generate user information' }).click();
  await popup.getByRole('button', { name: 'Sign in with Google.com' }).click();
  await expect(emptyState(page)).toBeVisible();
});

test('AC-001-05 a wrong password shows an error and the person stays on the sign-in screen', async ({
  page,
}) => {
  await createAccount(ANA);
  await signIn(page, { ...ANA, password: 'Napacno123' });
  await expect(page.getByRole('alert')).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Sign in' })).toBeVisible();
  await expect(emptyState(page)).toHaveCount(0);
});

test('AC-001-06 "Forgot password" starts the reset e-mail flow of Firebase', async ({ page }) => {
  await createAccount(ANA);
  await page.goto('/');
  await page.getByRole('link', { name: 'Forgot password?' }).click();
  await page.getByLabel('E-mail').fill(ANA.email);
  await page.getByRole('button', { name: 'Send link' }).click();
  await expect(page.getByRole('heading', { name: 'Check your e-mail' })).toBeVisible();
  await expect.poll(async () => (await emailCodes(ANA.email, 'PASSWORD_RESET')).length).toBeGreaterThan(0);
});

test('AC-001-07 a password of 7 characters is refused with the reason and creates no account', async ({
  page,
}) => {
  await register(page, { name: 'Ana Novak', email: ANA.email, password: 'Geslo12' });
  await expect(page.getByText('The password needs at least 8 characters.')).toBeVisible();
  await expect(verifyHeading(page)).toHaveCount(0);
  expect(await canSignInWith({ ...ANA, password: 'Geslo12' })).toBe(false);
});

test('AC-001-08 the session survives a reload and "Sign out" shows the sign-in screen', async ({ page }) => {
  await createAccount(ANA);
  await signIn(page, ANA);
  await expect(emptyState(page)).toBeVisible();
  await page.reload();
  await expect(emptyState(page)).toBeVisible();
  await signOut(page);
  await expect(page.getByRole('heading', { name: 'Sign in' })).toBeVisible();
});

test('EX-001-01 a password of exactly 8 characters is accepted', async ({ page }) => {
  await register(page, { name: 'Ana Novak', email: ANA.email, password: 'Geslo123' });
  await expect(verifyHeading(page)).toBeVisible();
});

test('EX-001-02 a password of 7 characters is refused', async ({ page }) => {
  await register(page, { name: 'Ana Novak', email: ANA.email, password: 'Geslo12' });
  await expect(verifyHeading(page)).toHaveCount(0);
  await expect(page.getByLabel('Password')).toBeVisible();
});

test('EX-001-03 an empty name and a name of 101 characters are refused and create no account', async ({
  page,
}) => {
  await register(page, { name: '', email: ANA.email, password: PASSWORD });
  await expect(page.getByText('Enter your name.')).toBeVisible();
  await expect(verifyHeading(page)).toHaveCount(0);
  await register(page, { name: 'x'.repeat(101), email: ANA.email, password: PASSWORD });
  await expect(verifyHeading(page)).toHaveCount(0);
  expect(await canSignInWith(ANA)).toBe(false);
});

test('EX-001-04 a name of exactly 100 characters is accepted', async ({ page }) => {
  await register(page, { name: 'x'.repeat(100), email: ANA.email, password: PASSWORD });
  await expect(verifyHeading(page)).toBeVisible();
});

test('EX-001-05 an e-mail that already has an account shows the refusal of Firebase and changes nothing', async ({
  page,
}) => {
  await createAccount(ANA);
  await register(page, { name: 'Druga Ana', email: ANA.email, password: 'Drugo12345' });
  await expect(page.getByText('An account with this e-mail address already exists.')).toBeVisible();
  expect(await canSignInWith(ANA)).toBe(true);
  expect(await canSignInWith({ ...ANA, password: 'Drugo12345' })).toBe(false);
});

test('EX-001-06 "Send again" makes Firebase send the verification e-mail again', async ({ page }) => {
  await createAccount(IVAN, { verified: false });
  await signIn(page, IVAN);
  await expect(verifyHeading(page)).toBeVisible();
  const before = (await emailCodes(IVAN.email, 'VERIFY_EMAIL')).length;
  await page.getByRole('button', { name: 'Send again' }).click();
  await expect(page.getByText('We sent the message again.')).toBeVisible();
  await expect
    .poll(async () => (await emailCodes(IVAN.email, 'VERIFY_EMAIL')).length)
    .toBeGreaterThan(before);
});

test('EX-001-07 verifying in another tab and reloading continues to the empty state', async ({ page }) => {
  await register(page, { name: 'Ana Novak', email: ANA.email, password: PASSWORD });
  await expect(verifyHeading(page)).toBeVisible();
  await verifyEmail(ANA.email);
  await page.reload();
  await expect(emptyState(page)).toBeVisible();
});
