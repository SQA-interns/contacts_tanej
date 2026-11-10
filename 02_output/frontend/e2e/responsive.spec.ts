import type { Page } from '@playwright/test';
import { join } from 'node:path';
import { expect, test } from '../acceptance/support';
import { mainScreens, openScreen, signedInScene, SIGNED_OUT } from './nfr-support';

/**
 * NFR-08 Responsiveness: from 320 px width without horizontal scrolling for viewing, searching and
 * filtering. Screenshots at 320, 768 and 1280 px of the list, filter and contact screens go to
 * ../logs/screenshots/responsive.
 */
const WIDTHS = [
  { width: 320, height: 640 },
  { width: 768, height: 1024 },
  { width: 1280, height: 800 },
] as const;

/** Empty when the page fits; otherwise the elements that stick out on the right, for the message. */
async function overflow(page: Page): Promise<string[]> {
  return page.evaluate(() => {
    const width = document.documentElement.clientWidth;
    if (document.documentElement.scrollWidth <= width) return [];
    return [...document.body.querySelectorAll('*')]
      .filter((e) => e.getBoundingClientRect().right > width + 1)
      .slice(0, 5)
      .map(
        (e) =>
          `${e.tagName.toLowerCase()}.${String(e.className).slice(0, 60)} right=${Math.round(e.getBoundingClientRect().right)}`,
      );
  });
}

for (const { width, height } of WIDTHS) {
  test(`NFR-08 signed-out screens at ${width} px have no horizontal scroll`, async ({
    page,
    browserName,
  }) => {
    await page.setViewportSize({ width, height });
    for (const screen of SIGNED_OUT) {
      await page.goto(screen.path);
      await page.getByRole('main').waitFor();
      expect(await overflow(page), `${screen.name} at ${width}`).toEqual([]);
      await page.screenshot({
        path: join('..', 'logs', 'screenshots', 'responsive', `${screen.name}-${browserName}-${width}.png`),
      });
    }
  });

  test(`NFR-08 signed-in screens at ${width} px have no horizontal scroll`, async ({ page, browserName }) => {
    await page.setViewportSize({ width, height });
    const { groupId, contactId } = await signedInScene(page);
    for (const screen of mainScreens(groupId, contactId)) {
      await openScreen(page, screen);
      expect.soft(await overflow(page), `${screen.name} at ${width}`).toEqual([]);
      await page.screenshot({
        path: join('..', 'logs', 'screenshots', 'responsive', `${screen.name}-${browserName}-${width}.png`),
      });
    }
  });
}
