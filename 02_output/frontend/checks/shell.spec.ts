import AxeBuilder from '@axe-core/playwright';
import { expect, test } from '@playwright/test';
import { join } from 'node:path';

/** The three widths of UI-22, named mobile, tablet, desktop. */
const WIDTHS = [
  { name: 'mobile', width: 375, height: 812 },
  { name: 'tablet', width: 768, height: 1024 },
  { name: 'desktop', width: 1440, height: 900 },
] as const;

for (const { name, width, height } of WIDTHS) {
  test(`shell at ${name}: no horizontal scroll and no serious accessibility violation (NFR-08, NFR-09)`, async ({
    page,
    browserName,
  }) => {
    await page.setViewportSize({ width, height });
    await page.goto('/');
    await expect(page.getByRole('heading', { level: 1, name: 'Dobrodošli' })).toBeVisible();

    const overflow = await page.evaluate(
      () => document.documentElement.scrollWidth > document.documentElement.clientWidth,
    );
    expect(overflow).toBe(false);

    const result = await new AxeBuilder({ page })
      .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
      .analyze();
    const serious = result.violations.filter((v) => v.impact === 'serious' || v.impact === 'critical');
    expect(serious.map((v) => v.id)).toEqual([]);

    await page.screenshot({ path: join('..', 'logs', 'screenshots', `shell-${browserName}-${name}.png`) });
  });
}

test('the language switch changes all texts at once and the choice survives a reload (R-018-02, R-018-03)', async ({
  page,
}) => {
  await page.goto('/');
  await page.getByRole('button', { name: 'EN' }).click();
  await expect(page.getByRole('heading', { level: 1, name: 'Welcome' })).toBeVisible();
  await page.reload();
  await expect(page.getByRole('heading', { level: 1, name: 'Welcome' })).toBeVisible();
});
