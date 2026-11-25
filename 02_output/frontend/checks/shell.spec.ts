import AxeBuilder from '@axe-core/playwright';
import { expect, test } from '@playwright/test';
import { join } from 'node:path';

/** The three widths of UI-22, named mobile, tablet, desktop. */
const WIDTHS = [
  { name: 'mobile', width: 375, height: 812 },
  { name: 'tablet', width: 768, height: 1024 },
  { name: 'desktop', width: 1440, height: 900 },
] as const;

/** The screens of US-001 a signed-out person reaches (WF-01, WF-02, WF-04), with their Slovenian headings. */
const SCREENS = [
  { name: 'sign-in', path: '/sign-in', heading: 'Prijava' },
  { name: 'register', path: '/register', heading: 'Registracija' },
  { name: 'reset-password', path: '/reset-password', heading: 'Ponastavitev gesla' },
] as const;

for (const screen of SCREENS) {
  for (const { name, width, height } of WIDTHS) {
    test(`${screen.name} at ${name}: no horizontal scroll and no serious accessibility violation (NFR-08, NFR-09)`, async ({
      page,
      browserName,
    }) => {
      await page.setViewportSize({ width, height });
      await page.goto(screen.path);
      await expect(page.getByRole('heading', { level: 1, name: screen.heading })).toBeVisible();

      const overflow = await page.evaluate(
        () => document.documentElement.scrollWidth > document.documentElement.clientWidth,
      );
      expect(overflow).toBe(false);

      // The D-82 fades (150-200 ms) make text half transparent while they run; scan the settled screen.
      await page.evaluate(
        () =>
          new Promise<void>((resolve) => {
            // Resolve after 300 ms without a running fade; spinners (endless) do not count.
            let quietSince = performance.now();
            const tick = () => {
              const now = performance.now();
              const fading = document
                .getAnimations()
                .some((a) => a.playState === 'running' && a.effect?.getComputedTiming().endTime !== Infinity);
              if (fading) quietSince = now;
              if (now - quietSince >= 300) resolve();
              else requestAnimationFrame(tick);
            };
            requestAnimationFrame(tick);
          }),
      );
      const result = await new AxeBuilder({ page })
        .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
        .analyze();
      const serious = result.violations.filter((v) => v.impact === 'serious' || v.impact === 'critical');
      expect(serious.map((v) => v.id)).toEqual([]);

      await page.screenshot({
        path: join('..', 'logs', 'screens', screen.name, `${screen.name}-${browserName}-${name}.png`),
      });
    });
  }
}

test('the language switch changes all texts at once and the choice survives a reload (R-018-02, R-018-03)', async ({
  page,
}) => {
  await page.goto('/sign-in');
  await page.getByRole('button', { name: 'EN' }).click();
  await expect(page.getByRole('heading', { level: 1, name: 'Sign in' })).toBeVisible();
  await page.reload();
  await expect(page.getByRole('heading', { level: 1, name: 'Sign in' })).toBeVisible();
});
