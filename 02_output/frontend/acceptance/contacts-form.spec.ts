import { ANA, MARKO, PETRA, dialog, expect, openGroup, openTab, row, scene, signIn, test } from './support';
import type { Page } from '@playwright/test';

/** The contact screen (WF-10) and Manage tags (WF-13): US-005 to US-008, the parts only the browser shows. */

async function newContact(page: Page, fields: Record<string, string>) {
  await page.getByRole('button', { name: 'Add contact' }).first().click();
  for (const [label, value] of Object.entries(fields))
    await page.getByLabel(label, { exact: true }).fill(value);
}

async function openContact(page: Page, lastName: string) {
  await row(page, lastName).getByRole('link', { name: lastName }).click();
}

test('AC-005-02 a contact with only a phone is refused with the reason and nothing is stored', async ({
  page,
}) => {
  const { owner, groupId } = await scene('Konferenca 2026');
  await openGroup(page, ANA, 'Konferenca 2026');
  await newContact(page, { Phone: '+386 40 123 456' });
  await page.getByRole('button', { name: 'Save', exact: true }).click();
  await expect(
    page.getByText('Enter at least one of first name, last name, e-mail or company.').first(),
  ).toBeVisible();
  expect((await owner.ok('POST', `/groups/${groupId}/contacts/search`, {})).total).toBe(0);
});

test('AC-005-04 an e-mail without a domain is refused naming the e-mail field', async ({ page }) => {
  const { owner, groupId } = await scene('Konferenca 2026');
  await openGroup(page, ANA, 'Konferenca 2026');
  await newContact(page, { 'Last name': 'Novak', 'E-mail': 'ana.novak@' });
  await page.getByRole('button', { name: 'Save', exact: true }).click();
  await expect(page.getByText('Check the e-mail address.')).toBeVisible();
  expect((await owner.ok('POST', `/groups/${groupId}/contacts/search`, {})).total).toBe(0);
});

test('AC-005-10 the country is chosen from the list and a free text is not offered', async ({ page }) => {
  await scene('Konferenca 2026');
  await openGroup(page, ANA, 'Konferenca 2026');
  await page.getByRole('button', { name: 'Add contact' }).first().click();
  const country = page.getByLabel('Country', { exact: true });
  await expect(country).toBeVisible();
  await country.selectOption({ label: 'Slovenia' });
  await expect(country.locator('option', { hasText: 'Slovenija' })).toHaveCount(0);
});

test('AC-005-07 the second of two saves is refused with "changed by someone else" and the new values are shown', async ({
  page,
}) => {
  const { owner, groupId, clients } = await scene('Konferenca 2026', [{ person: MARKO, role: 'EDITOR' }]);
  const contact = await owner.contact(groupId, {
    firstName: 'Ana',
    lastName: 'Novak',
    phone: '+386 40 123 456',
  });
  await openGroup(page, MARKO, 'Konferenca 2026');
  await openContact(page, 'Novak');
  await page.getByLabel('Phone').fill('+386 42 000 000');
  const other = clients[ANA.email]!;
  const opened = await other.ok('GET', `/groups/${groupId}/contacts/${contact.id}`);
  await other.ok('PUT', `/groups/${groupId}/contacts/${contact.id}`, { ...opened, phone: '+386 41 555 123' });
  await page.getByRole('button', { name: 'Save', exact: true }).click();
  await expect(page.getByText('This contact was changed by someone else.')).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Current values' })).toBeVisible();
  await expect(page.getByText('+386 41 555 123')).toBeVisible();
  expect((await other.ok('GET', `/groups/${groupId}/contacts/${contact.id}`)).phone).toBe('+386 41 555 123');
});

test('AC-005-08 a Viewer sees all values and no Save or Delete contact', async ({ page }) => {
  const { owner, groupId } = await scene('Konferenca 2026', [{ person: PETRA, role: 'VIEWER' }]);
  await owner.contact(groupId, { firstName: 'Ana', lastName: 'Novak', company: 'Acme' });
  await openGroup(page, PETRA, 'Konferenca 2026');
  await openContact(page, 'Novak');
  await expect(page.getByText('Acme')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Save', exact: true })).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Delete contact' })).toHaveCount(0);
});

test('AC-006-01 a tag typed on the contact is saved and shown as a chip', async ({ page }) => {
  const { owner, groupId } = await scene('Konferenca 2026');
  await owner.contact(groupId, { firstName: 'Ana', lastName: 'Novak' });
  await openGroup(page, ANA, 'Konferenca 2026');
  await openContact(page, 'Novak');
  await page.getByLabel('Tags', { exact: true }).fill('udelezenec2025');
  await page.keyboard.press('Enter');
  await page.getByRole('button', { name: 'Save', exact: true }).click();
  await page.reload();
  await expect(page.getByText('udelezenec2025')).toBeVisible();
});

test('AC-006-03 a tag with a space is refused naming the tag and nothing is stored', async ({ page }) => {
  const { owner, groupId } = await scene('Konferenca 2026');
  const contact = await owner.contact(groupId, { firstName: 'Ana', lastName: 'Novak' });
  await openGroup(page, ANA, 'Konferenca 2026');
  await openContact(page, 'Novak');
  await page.getByLabel('Tags', { exact: true }).fill('neka značka');
  await page.keyboard.press('Enter');
  await page.getByRole('button', { name: 'Save', exact: true }).click();
  await expect(page.getByText('Tag names use letters, digits, - and _.')).toBeVisible();
  expect((await owner.ok('GET', `/groups/${groupId}/contacts/${contact.id}`)).tags).toEqual([]);
});

test('AC-006-07 a Viewer sees the tags and has no control to change them', async ({ page }) => {
  const { owner, groupId } = await scene('Konferenca 2026', [{ person: PETRA, role: 'VIEWER' }]);
  await owner.contact(groupId, { lastName: 'Novak', tags: ['udelezenec2025'] });
  await openGroup(page, PETRA, 'Konferenca 2026');
  await openContact(page, 'Novak');
  await expect(page.getByText('udelezenec2025')).toBeVisible();
  await expect(page.getByLabel('Tags', { exact: true })).toHaveCount(0);
});

test('AC-007-02 renaming a tag onto another tag shows the reason and changes nothing', async ({ page }) => {
  const { owner, groupId } = await scene('Konferenca 2026');
  await owner.contact(groupId, { lastName: 'A', tags: ['udelezenec2025'] });
  await owner.contact(groupId, { lastName: 'B', tags: ['gost'] });
  await openGroup(page, ANA, 'Konferenca 2026');
  await openTab(page, 'Tags');
  await row(page, 'gost').getByRole('button', { name: 'Rename' }).click();
  await dialog(page).getByLabel('Tag name').fill('UDELEZENEC2025');
  await dialog(page).getByRole('button', { name: 'Rename' }).click();
  await expect(dialog(page).getByText('A tag with this name already exists.')).toBeVisible();
  const tags = await owner.ok('GET', `/groups/${groupId}/tags`);
  expect(tags.map((t: { name: string }) => t.name).sort()).toEqual(['gost', 'udelezenec2025']);
});

test('AC-007-07 a Viewer has no Tags tab and no rename or delete control', async ({ page }) => {
  const { owner, groupId } = await scene('Konferenca 2026', [{ person: PETRA, role: 'VIEWER' }]);
  await owner.contact(groupId, { lastName: 'A', tags: ['old'] });
  await openGroup(page, PETRA, 'Konferenca 2026');
  await expect(
    page.getByRole('navigation', { name: 'Group' }).getByRole('link', { name: 'Tags' }),
  ).toHaveCount(0);
  await page.goto(`/groups/${groupId}/tags`);
  await expect(page.getByRole('button', { name: 'Rename' })).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Delete', exact: true })).toHaveCount(0);
});

test('AC-008-02 an attribute key equal ignoring case is refused and the contact is unchanged', async ({
  page,
}) => {
  const { owner, groupId } = await scene('Konferenca 2026');
  const contact = await owner.contact(groupId, {
    lastName: 'Novak',
    attributes: [{ key: 'kompetenca', value: 'java' }],
  });
  await openGroup(page, ANA, 'Konferenca 2026');
  await openContact(page, 'Novak');
  await page.getByRole('button', { name: 'Add attribute' }).click();
  await page.getByLabel('Key').last().fill('Kompetenca');
  await page.getByLabel('Value').last().fill('python');
  await page.getByRole('button', { name: 'Save', exact: true }).click();
  await expect(page.getByRole('alert').first()).toBeVisible();
  expect((await owner.ok('GET', `/groups/${groupId}/contacts/${contact.id}`)).attributes).toHaveLength(1);
});

test('AC-008-03 keys used in the group are suggested and a different key may be typed', async ({ page }) => {
  const { owner, groupId } = await scene('Konferenca 2026');
  await owner.contact(groupId, { lastName: 'A', attributes: [{ key: 'kompetenca', value: 'java' }] });
  const second = await owner.contact(groupId, { lastName: 'Novak' });
  await openGroup(page, ANA, 'Konferenca 2026');
  await openContact(page, 'Novak');
  await page.getByRole('button', { name: 'Add attribute' }).click();
  await page.getByLabel('Key').last().fill('kom');
  await expect(page.getByRole('option', { name: 'kompetenca' })).toBeVisible();
  await page.getByLabel('Key').last().fill('komp2');
  await page.getByLabel('Value').last().fill('x');
  await page.getByRole('button', { name: 'Save', exact: true }).click();
  await expect
    .poll(
      async () => (await owner.ok('GET', `/groups/${groupId}/contacts/${second.id}`)).attributes?.[0]?.key,
    )
    .toBe('komp2');
});

test('AC-008-07 a Viewer sees the attributes and no control to change them', async ({ page }) => {
  const { owner, groupId } = await scene('Konferenca 2026', [{ person: PETRA, role: 'VIEWER' }]);
  await owner.contact(groupId, { lastName: 'Novak', attributes: [{ key: 'kompetenca', value: 'java' }] });
  await signIn(page, PETRA);
  await page.getByRole('link', { name: 'Konferenca 2026' }).click();
  await openContact(page, 'Novak');
  await expect(page.getByText('kompetenca')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Add attribute' })).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Remove attribute' })).toHaveCount(0);
});
