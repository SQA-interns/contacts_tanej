import { ANA, dialog, expect, openTab, row, test, verifyEmail } from '../acceptance/support';

/**
 * E2E-01 J1 First use (US-001, US-002, US-005, US-006, US-008, US-009): a new person registers, verifies
 * the e-mail, signs in, sees the empty state, creates the group "Konferenca 2026", adds the contact Ana
 * Novak with an attribute and a tag, and finds her with the search box.
 */
test('E2E-01 J1 First use', async ({ page }) => {
  await page.goto('/register');
  await page.getByLabel('Name').fill('Ana Novak');
  await page.getByLabel('E-mail').fill(ANA.email);
  await page.getByLabel('Password').fill('Geslo1234');
  await page.getByRole('button', { name: 'Register', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Verify your e-mail' })).toBeVisible();

  await verifyEmail(ANA.email);
  await page.getByRole('button', { name: 'Sign out' }).click();
  await page.getByLabel('E-mail').fill(ANA.email);
  await page.getByLabel('Password').fill('Geslo1234');
  await page.getByRole('button', { name: 'Sign in', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Create a group' })).toBeVisible();

  await page.getByRole('button', { name: 'Create group' }).click();
  await page.getByLabel('Group name').fill('Konferenca 2026');
  await dialog(page).getByRole('button', { name: 'Create group' }).click();
  await expect(page.getByRole('heading', { name: 'Konferenca 2026' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'No contacts yet' })).toBeVisible();

  await page.getByRole('button', { name: 'Add contact' }).first().click();
  await page.getByLabel('First name', { exact: true }).fill('Ana');
  await page.getByLabel('Last name', { exact: true }).fill('Novak');
  await page.getByLabel('E-mail', { exact: true }).fill('ana.novak@example.com');
  await page.getByLabel('Company', { exact: true }).fill('Acme');
  await page.getByLabel('Tags', { exact: true }).fill('udelezenec2025');
  await page.keyboard.press('Enter');
  await page.getByRole('button', { name: 'Add attribute' }).click();
  await page.getByLabel('Key').last().fill('kompetenca');
  await page.getByLabel('Value').last().fill('spletnoProgramiranje');
  await page.getByRole('button', { name: 'Save', exact: true }).click();
  await expect(page.getByText('The contact was saved.')).toBeVisible();

  await openTab(page, 'Contacts');
  await page.getByLabel('Search').fill('udelez');
  await expect(row(page, 'Novak')).toBeVisible();
  await page.getByLabel('Search').fill('spletno');
  await expect(row(page, 'Novak')).toContainText('Acme');
  await page.getByLabel('Search').fill('skerlj');
  await expect(page.getByRole('heading', { name: 'No contacts match' })).toBeVisible();
});
