import type { Page } from '@playwright/test';
import { ANA, scene, signIn } from '../acceptance/support';

/** The screens of the main flows (sign-in, list, create or edit a contact, export) of NFR-08 to NFR-11. */
export interface Screen {
  name: string;
  /** Path under the group, or '' for the groups list; null for the signed-out screens. */
  path: string | null;
  open?: (page: Page) => Promise<void>;
}

export const SIGNED_OUT = [
  { name: 'sign-in', path: '/sign-in' },
  { name: 'register', path: '/register' },
] as const;

/** An owner with a group "Konferenca 2026" and four contacts, signed in on the groups list (or not signed in). */
export async function signedInScene(
  page: Page,
  signedIn = true,
): Promise<{ groupId: string; contactId: string }> {
  const { owner, groupId } = await scene('Konferenca 2026');
  const contacts = [];
  for (const [first, last, company] of [
    ['Ana', 'Novak', 'Acme'],
    ['Boris', 'Čuk', 'Šola d.o.o.'],
    ['Cene', 'Kos', 'Acme'],
    ['Dora', 'Zupan', 'Zavod'],
  ] as const)
    contacts.push(
      await owner.contact(groupId, {
        firstName: first,
        lastName: last,
        company,
        email: `${first.toLowerCase()}@example.com`,
        tags: ['udelezenec2026'],
      }),
    );
  if (signedIn) {
    await signIn(page, ANA);
    await page.getByRole('link', { name: 'Konferenca 2026' }).waitFor();
  }
  return { groupId, contactId: contacts[0]?.id ?? '' };
}

/** The signed-in screens of the main flows, as in the wireframes. */
export function mainScreens(groupId: string, contactId: string): Screen[] {
  const group = `/groups/${groupId}`;
  return [
    { name: 'groups', path: '/' },
    { name: 'contacts', path: group },
    {
      name: 'filter',
      path: group,
      open: async (page) => {
        await page.getByRole('button', { name: /^(Filter|Filtriraj)/ }).click();
      },
    },
    { name: 'contact-new', path: `${group}/contacts/new` },
    { name: 'contact-edit', path: `${group}/contacts/${contactId}` },
    { name: 'members', path: `${group}/members` },
    { name: 'history', path: `${group}/history` },
    { name: 'trash', path: `${group}/trash` },
  ];
}

export async function openScreen(page: Page, screen: Screen): Promise<void> {
  await page.goto(screen.path ?? '/');
  await page.getByRole('main').waitFor();
  await page.waitForLoadState('networkidle');
  await screen.open?.(page);
}
