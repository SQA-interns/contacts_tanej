import AxeBuilder from '@axe-core/playwright';
import type { Page } from '@playwright/test';
import { ANA, dialog, expect, test } from '../acceptance/support';
import { mainScreens, openScreen, signedInScene, SIGNED_OUT } from './nfr-support';

/**
 * NFR-09 Accessibility: WCAG 2.1 level AA on each main screen in both themes and both languages, and a
 * keyboard-only run of the main flow with a visible focus. "Both themes" are the light and the dark
 * appearance (prefers-color-scheme) of the theme in use; the wireframe theme is the one in `index.html`.
 */
const LANGUAGES = ['sl', 'en'] as const;
const SCHEMES = ['light', 'dark'] as const;

async function violations(page: Page): Promise<string[]> {
  const result = await new AxeBuilder({ page })
    .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
    .analyze();
  return result.violations.map((v) => `${v.id} (${v.impact}) x${v.nodes.length}`);
}

for (const language of LANGUAGES) {
  for (const scheme of SCHEMES) {
    test(`NFR-09 axe on every main screen, ${language}, ${scheme}`, async ({ page }) => {
      test.setTimeout(120_000); // eleven screens, each scanned; WebKit is the slowest
      await page.emulateMedia({ colorScheme: scheme });
      const found: string[] = [];
      for (const screen of SIGNED_OUT) {
        await page.goto(screen.path);
        await page.evaluate((l) => localStorage.setItem('language', l), language);
        await page.goto(screen.path);
        await page.getByRole('main').waitFor();
        found.push(...(await violations(page)).map((v) => `${screen.name}: ${v}`));
      }
      await page.evaluate(() => localStorage.setItem('language', 'en'));
      const { groupId, contactId } = await signedInScene(page);
      await page.evaluate((l) => localStorage.setItem('language', l), language);
      for (const screen of mainScreens(groupId, contactId)) {
        await openScreen(page, screen);
        found.push(...(await violations(page)).map((v) => `${screen.name}: ${v}`));
      }
      expect(found).toEqual([]);
    });
  }
}

test('NFR-09 keyboard only: sign in, open the list, create a contact, with a visible focus', async ({
  page,
}) => {
  const { groupId } = await signedInScene(page, false);
  await page.goto('/sign-in');
  await page.getByLabel('E-mail').focus();
  await page.keyboard.type(ANA.email);
  await page.keyboard.press('Tab');
  await page.keyboard.type(ANA.password);
  await page.keyboard.press('Enter');
  await expect(page.getByRole('link', { name: 'Konferenca 2026' })).toBeVisible();

  // Tab to the group link: every stop shows an outline or a ring that is not "none".
  const focusVisible = async () =>
    page.evaluate(() => {
      const el = document.activeElement as HTMLElement | null;
      if (!el || el === document.body) return false;
      const s = getComputedStyle(el);
      return (s.outlineStyle !== 'none' && parseFloat(s.outlineWidth) > 0) || s.boxShadow !== 'none';
    });
  let reached = false;
  for (let i = 0; i < 40 && !reached; i++) {
    await page.keyboard.press('Tab');
    expect(await focusVisible(), `focus visible at tab stop ${i + 1}`).toBe(true);
    reached = await page.evaluate(
      () => document.activeElement?.textContent?.includes('Konferenca 2026') ?? false,
    );
  }
  expect(reached).toBe(true);
  await page.keyboard.press('Enter');
  await expect(page.getByRole('heading', { name: 'Konferenca 2026' })).toBeVisible();

  await page.goto(`/groups/${groupId}/contacts/new`);
  await page.getByLabel('First name', { exact: true }).focus();
  await page.keyboard.type('Petra');
  await page.keyboard.press('Tab');
  await page.keyboard.type('Horvat');
  const save = page.getByRole('button', { name: 'Save', exact: true });
  for (let i = 0; i < 40; i++) {
    if (await save.evaluate((el) => el === document.activeElement)) break;
    await page.keyboard.press('Tab');
  }
  await expect(save).toBeFocused();
  await page.keyboard.press('Enter');
  await expect(page.getByRole('heading', { level: 1, name: 'Petra Horvat' })).toBeVisible();
});

test('NFR-09 a dialog traps the focus and Escape closes it', async ({ page }) => {
  await signedInScene(page);
  await page.getByRole('link', { name: 'Konferenca 2026' }).click();
  await page.getByRole('button', { name: 'Filter' }).click();
  await expect(dialog(page)).toBeVisible();
  for (let i = 0; i < 15; i++) {
    await page.keyboard.press('Tab');
    expect(await dialog(page).evaluate((d) => d.contains(document.activeElement))).toBe(true);
  }
  await page.keyboard.press('Escape');
  await expect(dialog(page)).toBeHidden();
});
