import type { Page } from '@playwright/test';
import { expect, test } from '../acceptance/support';
import { mainScreens, openScreen, signedInScene, SIGNED_OUT } from './nfr-support';

/**
 * NFR-10 Localisation: the language switch changes the texts of each screen of the main flows at once
 * (AC-018-01 to AC-018-07 are the story's own tests). On every screen the page text differs between
 * the two languages, the html `lang` follows, and no translation key is shown as text.
 */
const slovenian = (page: Page) => page.getByRole('button', { name: 'SL', exact: true });
const english = (page: Page) => page.getByRole('button', { name: 'EN', exact: true });

/** Text a person reads: the page text plus the accessible names of controls, without the typed data. */
async function readable(page: Page): Promise<string> {
  return page.evaluate(() => {
    const labels = [...document.querySelectorAll('[aria-label],[placeholder],[title]')].flatMap((e) => [
      e.getAttribute('aria-label') ?? '',
      e.getAttribute('placeholder') ?? '',
      e.getAttribute('title') ?? '',
    ]);
    return `${document.body.innerText}\n${labels.join('\n')}`;
  });
}

/** A translation key such as `contacts.list.title` shown as text would mean a missing entry. */
function rawKeys(text: string): string[] {
  return text.split(/\s+/).filter((w) => /^[a-z][a-zA-Z]*(\.[a-z][a-zA-Z0-9]*){2,}$/.test(w));
}

async function checkSwitch(page: Page, name: string, open?: (p: Page) => Promise<void>): Promise<void> {
  // A dialog makes the header inert, so the language is switched first and the dialog opened after.
  const read = async () => {
    await open?.(page);
    const text = await readable(page);
    if (open) await page.keyboard.press('Escape');
    return text;
  };
  await english(page).click();
  await expect(page.locator('html')).toHaveAttribute('lang', 'en');
  const inEnglish = await read();
  await slovenian(page).click();
  await expect(page.locator('html')).toHaveAttribute('lang', 'sl');
  const inSlovenian = await read();
  expect(inSlovenian, `${name}: the text changes with the language`).not.toEqual(inEnglish);
  expect(rawKeys(inEnglish), `${name}: raw keys in English`).toEqual([]);
  expect(rawKeys(inSlovenian), `${name}: raw keys in Slovenian`).toEqual([]);
}

test('NFR-10 the switch changes every signed-out screen', async ({ page }) => {
  for (const screen of SIGNED_OUT) {
    await page.goto(screen.path);
    await page.getByRole('main').waitFor();
    await checkSwitch(page, screen.name);
  }
});

test('NFR-10 the switch changes every screen of the main flows', async ({ page }) => {
  const { groupId, contactId } = await signedInScene(page);
  for (const screen of mainScreens(groupId, contactId)) {
    await openScreen(page, { ...screen, open: undefined });
    await checkSwitch(page, screen.name, screen.open);
  }
});

test.describe('reload', () => {
  test.use({ english: false });
  test('NFR-10 the language survives a reload on a signed-in screen', async ({ page }) => {
    await page.goto('/');
    await english(page).click(); // the sign-in helper of the harness uses the English labels
    const { groupId } = await signedInScene(page);
    await page.goto(`/groups/${groupId}`);
    await slovenian(page).click();
    await page.reload();
    await expect(slovenian(page)).toHaveAttribute('aria-pressed', 'true');
    await expect(page.locator('html')).toHaveAttribute('lang', 'sl');
  });
});
