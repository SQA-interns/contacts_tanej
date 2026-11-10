import { join } from 'node:path';
import { ANA, Client, createAccount, dialog, expect, signIn, test, verifyEmail } from '../acceptance/support';

/**
 * NFR-11 Usability and DoD-P02: a new person needs no manual. (1) From the first sign-in, a new person
 * with no help creates a group and a first contact in under 120 seconds. (2) Every screen shows a clear
 * empty state and an error shows a clear message; screenshots for the review against the wireframes go to
 * ../logs/screenshots/states.
 */
test('DoD-P02 a new person creates a group and the first contact in under 120 seconds', async ({ page }) => {
  await createAccount(ANA);
  const started = Date.now();
  await signIn(page, ANA);
  await page.getByRole('button', { name: 'Create group' }).click();
  await page.getByLabel('Group name').fill('Konferenca 2026');
  await dialog(page).getByRole('button', { name: 'Create group' }).click();
  await page.getByRole('button', { name: 'Add contact' }).first().click();
  await page.getByLabel('First name', { exact: true }).fill('Ana');
  await page.getByLabel('Last name', { exact: true }).fill('Novak');
  await page.getByRole('button', { name: 'Save', exact: true }).click();
  await expect(page.getByRole('heading', { level: 1, name: 'Ana Novak' })).toBeVisible();
  const seconds = (Date.now() - started) / 1000;
  console.log(`DoD-P02 first contact after ${seconds.toFixed(1)} s`);
  expect(seconds).toBeLessThan(120);
});

test('DoD-P02 registering, verifying and signing in leads to the first group without help', async ({
  page,
}) => {
  await page.goto('/register');
  await page.getByLabel('Name').fill('Ana Novak');
  await page.getByLabel('E-mail').fill(ANA.email);
  await page.getByLabel('Password').fill(ANA.password);
  await page.getByRole('button', { name: 'Register', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Verify your e-mail' })).toBeVisible();
  await verifyEmail(ANA.email);
  await page.getByRole('button', { name: 'Sign out' }).click();
  await signIn(page, ANA);
  await expect(page.getByRole('heading', { name: 'Create a group' })).toBeVisible();
});

test('NFR-11 every screen has an empty state', async ({ page }) => {
  const owner = await Client.of(ANA);
  await signIn(page, ANA);
  await expect(page.getByRole('heading', { name: 'Create a group' })).toBeVisible();
  const shot = (name: string) =>
    page.screenshot({ path: join('..', 'logs', 'screenshots', 'states', `empty-${name}.png`) });
  await shot('groups');

  const groupId = await owner.group('Prazna skupina');
  await page.goto(`/groups/${groupId}`);
  await expect(page.getByRole('heading', { name: 'No contacts yet' })).toBeVisible();
  await shot('contacts');

  await page.getByLabel('Search').fill('zzzz');
  await expect(page.getByRole('heading', { name: 'No contacts match' })).toBeVisible();
  await shot('no-match');

  await page.goto(`/groups/${groupId}/trash`);
  await expect(page.getByText(/Trash is empty/)).toBeVisible();
  await shot('trash');

  await page.goto(`/groups/${groupId}/tags`);
  await expect(page.getByText(/No tags yet/)).toBeVisible();
  await shot('tags');

  await page.goto('/deleted-groups');
  await expect(page.getByText(/Groups you delete stay here for 30 days/)).toBeVisible();
  await shot('deleted-groups');

  await page.goto(`/groups/${groupId}/history`);
  await expect(page.getByText(/No History yet|Nothing here yet/).first()).toBeVisible();
  await shot('history');
});

test('NFR-11 an error is shown as a clear message with a way to try again', async ({ page }) => {
  const owner = await Client.of(ANA);
  const groupId = await owner.group('Skupina');
  await signIn(page, ANA);
  await expect(page.getByRole('link', { name: 'Skupina' })).toBeVisible();

  const failing = (route: import('@playwright/test').Route) =>
    ['fetch', 'xhr'].includes(route.request().resourceType()) &&
    /\/groups\/[^/?]+\/contacts/.test(route.request().url())
      ? route.fulfill({ status: 500, json: { code: 'INTERNAL_ERROR' } })
      : route.fallback();
  await page.route(/\/groups\//, failing);
  await page.goto(`/groups/${groupId}`);
  const message = page.getByText(/Something went wrong|The network is not available/).first();
  await expect(message).toBeVisible();
  await page.screenshot({ path: join('..', 'logs', 'screenshots', 'states', 'error-contacts.png') });
  await expect(page.getByRole('button', { name: 'Try again' })).toBeVisible();

  await page.unroute(/\/groups\//, failing);
  await page.goto('/groups/00000000-0000-0000-0000-000000000000');
  await page.screenshot({ path: join('..', 'logs', 'screenshots', 'states', 'error-no-group.png') });
  await expect(page.getByRole('main')).not.toBeEmpty();
});
