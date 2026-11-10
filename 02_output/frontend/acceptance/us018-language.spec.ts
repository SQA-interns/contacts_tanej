import { ANA, Client, expect, openGroup, openTab, scene, signIn, test } from './support';
import type { Page } from '@playwright/test';

/** US-018 Language: AC-018-01 to AC-018-07, EX-018-01 to EX-018-04. The header switch is drawn in every wireframe. */

const slovenian = (page: Page) => page.getByRole('button', { name: 'SL', exact: true });
const english = (page: Page) => page.getByRole('button', { name: 'EN', exact: true });

/** Rewrites the time of every History entry the API returns, so that the display can be checked. */
async function historyAt(page: Page, instant: string) {
  await page.route('**/api/groups/*/history?*', async (route) => {
    const response = await route.fetch();
    const body = await response.json();
    for (const item of body.items ?? []) item.createdAt = instant;
    await route.fulfill({ response, json: body });
  });
  await page.route('**/api/groups/*/history', async (route) => {
    const response = await route.fetch();
    const body = await response.json();
    for (const item of body.items ?? []) item.createdAt = instant;
    await route.fulfill({ response, json: body });
  });
}

test.describe('Slovenian by default', () => {
  test.use({ english: false, locale: 'fr-FR' });

  test('AC-018-01 and EX-018-01 the first visit shows Slovenian, whatever the browser language', async ({
    page,
  }) => {
    await page.goto('/');
    await expect(slovenian(page)).toHaveAttribute('aria-pressed', 'true');
    await expect(english(page)).toHaveAttribute('aria-pressed', 'false');
    await expect(page.locator('html')).toHaveAttribute('lang', 'sl');
    await expect(page.getByRole('heading', { name: 'Sign in' })).toHaveCount(0);
    // the first visit opens the sign-in screen: one e-mail field and at least two buttons in the main area
    await expect(page.getByRole('main').locator('input[type=email]')).toHaveCount(1);
    expect(await page.getByRole('main').getByRole('button').count()).toBeGreaterThanOrEqual(2);
  });
});

test.describe('the choice is remembered', () => {
  test.use({ english: false });

  test('AC-018-03 English chosen once is shown again on the next visit in the same browser', async ({
    page,
  }) => {
    await page.goto('/');
    await english(page).click();
    await expect(page.getByRole('heading', { name: 'Sign in' })).toBeVisible();
    await page.goto('/register');
    await expect(page.getByRole('heading', { name: 'Register' })).toBeVisible();
    await page.reload();
    await expect(page.getByRole('heading', { name: 'Register' })).toBeVisible();
  });
});

test('AC-018-02 switching the language changes the labels at once and keeps the typed text', async ({
  page,
}) => {
  const { groupId } = await scene('Konferenca 2026');
  await signIn(page, ANA);
  await page.goto(`/groups/${groupId}/contacts/new`);
  await page.getByLabel('First name').fill('Ana');
  await slovenian(page).click();
  await expect(page.getByLabel('First name')).toHaveCount(0);
  await english(page).click();
  await expect(page.getByLabel('First name')).toHaveValue('Ana');
});

test('AC-018-04 a time is shown as 2. 7. 2026 00:30 in Slovenian and 2026-07-02 00:30 in English', async ({
  page,
}) => {
  const { owner, groupId } = await scene('Konferenca 2026');
  await owner.contact(groupId, { lastName: 'Novak' });
  await historyAt(page, '2026-07-01T22:30:00Z');
  await openGroup(page, ANA, 'Konferenca 2026');
  await openTab(page, 'History');
  await expect(page.getByText('2026-07-02 00:30').first()).toBeVisible();
  await slovenian(page).click();
  await expect(page.getByText('2. 7. 2026 00:30').first()).toBeVisible();
  await english(page).click();
  await expect(page.getByText('2026-07-02 00:30').first()).toBeVisible();
});

test('EX-018-03 in winter the time is shown as 23:30', async ({ page }) => {
  const { owner, groupId } = await scene('Konferenca 2026');
  await owner.contact(groupId, { lastName: 'Novak' });
  await historyAt(page, '2026-01-15T22:30:00Z');
  await openGroup(page, ANA, 'Konferenca 2026');
  await openTab(page, 'History');
  await expect(page.getByText('2026-01-15 23:30').first()).toBeVisible();
  await slovenian(page).click();
  await expect(page.getByText('15. 1. 2026 23:30').first()).toBeVisible();
});

test('AC-018-05 Slovenia is shown as "Slovenija" in Slovenian and "Slovenia" in English', async ({
  page,
}) => {
  const { owner, groupId } = await scene('Konferenca 2026');
  await owner.contact(groupId, { lastName: 'Novak', country: 'SI' });
  await openGroup(page, ANA, 'Konferenca 2026');
  await expect(page.getByRole('row').filter({ hasText: 'Novak' })).toContainText('Slovenia');
  await slovenian(page).click();
  await expect(page.getByRole('row').filter({ hasText: 'Novak' })).toContainText('Slovenija');
  await english(page).click();
  await expect(page.getByRole('row').filter({ hasText: 'Novak' })).toContainText('Slovenia');
});

test('AC-018-06 the order of Čuk, Cerar, Sever, Šuster is the same in the English and the Slovenian UI', async ({
  page,
}) => {
  const { owner, groupId } = await scene('Konferenca 2026');
  for (const lastName of ['Šuster', 'Sever', 'Čuk', 'Cerar']) await owner.contact(groupId, { lastName });
  await openGroup(page, ANA, 'Konferenca 2026');
  const order = async () => (await page.getByRole('row').allTextContents()).join('|');
  const expected = ['Cerar', 'Čuk', 'Sever', 'Šuster'];
  for (const language of [english, slovenian]) {
    await language(page).click();
    const text = await order();
    const positions = expected.map((name) => text.indexOf(name));
    expect(positions.every((p) => p >= 0)).toBe(true);
    expect([...positions].sort((a, b) => a - b)).toEqual(positions);
  }
});

test('AC-018-07 tags and attributes are unchanged when the language changes', async ({ page }) => {
  const { owner, groupId } = await scene('Konferenca 2026');
  const contact = await owner.contact(groupId, {
    lastName: 'Novak',
    tags: ['udelezenec2025'],
    attributes: [{ key: 'kompetenca', value: 'spletnoProgramiranje' }],
  });
  await signIn(page, ANA);
  await page.goto(`/groups/${groupId}/contacts/${contact.id}`);
  await expect(page.getByText('udelezenec2025')).toBeVisible();
  await expect(page.getByLabel('Key')).toHaveValue('kompetenca');
  await expect(page.getByLabel('Value')).toHaveValue('spletnoProgramiranje');
  await slovenian(page).click();
  await expect(page.getByText('udelezenec2025')).toBeVisible();
  await english(page).click();
  await expect(page.getByText('udelezenec2025')).toBeVisible();
  await expect(page.getByLabel('Key')).toHaveValue('kompetenca');
  await expect(page.getByLabel('Value')).toHaveValue('spletnoProgramiranje');
});

test('EX-018-02 a server error is shown in the active language', async ({ page }) => {
  const { owner } = await scene('Konferenca 2026');
  expect(owner).toBeDefined();
  await signIn(page, ANA);
  // the dialog is modal and hides the header switch, so the language is chosen before it opens
  await slovenian(page).click();
  await page.getByRole('button', { name: 'Ustvari skupino' }).first().click();
  await page.getByLabel('Ime skupine').fill('Konferenca 2026');
  await page.getByRole('dialog').getByRole('button', { name: 'Ustvari skupino' }).click();
  await expect(page.getByText('Skupino s tem imenom že imate.')).toBeVisible();
  const alertText = await page.getByRole('dialog').innerText();
  expect(alertText).not.toContain('You already have a group with this name.');
  expect(alertText).not.toMatch(/error\.|GROUP_NAME_TAKEN/);
});

test('EX-018-04 no screen shows a text key instead of a text, in either language', async ({ page }) => {
  await Client.of(ANA);
  const keyLike =
    /\b(auth|common|error|groups|contacts|members|nav|account|history|versions|duplicates)\.[A-Za-z]+/;
  for (const path of ['/sign-in', '/register']) {
    for (const language of [english, slovenian]) {
      await page.goto(path);
      await language(page).click();
      expect(await page.locator('body').innerText()).not.toMatch(keyLike);
    }
  }
  await signIn(page, ANA);
  for (const language of [english, slovenian]) {
    await language(page).click();
    expect(await page.locator('body').innerText()).not.toMatch(keyLike);
  }
});
