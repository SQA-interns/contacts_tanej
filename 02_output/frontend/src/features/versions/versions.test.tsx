import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ApiError, type ApiClient } from '@/lib/api/client';
import type { components } from '@/lib/api/schema';
import { groupsApi, konferenca, skupno, signedIn } from '@/test/groupsApi';
import { renderApp } from '@/test/renderApp';

type Contact = components['schemas']['Contact'];

const ana: Contact = {
  id: 'c-1',
  state: 'ACTIVE',
  revision: 5,
  versionNumber: 3,
  createdAt: '2026-11-10T10:00:00Z',
  firstName: 'Ana',
  lastName: 'Novak',
  phone: '+386 41 555 123',
  tags: ['a', 'b'],
  attributes: [],
};

const list = [3, 2, 1].map((number) => ({
  number,
  userName: 'Marko Pirc',
  createdAt: '2026-07-01T22:30:00Z',
}));

const snapshot1 = {
  firstName: 'Ana',
  lastName: 'Novak',
  phone: '+386 40 123 456',
  tags: ['a'],
  attributes: [],
};

function versionsApi(restore?: () => unknown) {
  const { api: groups } = groupsApi([konferenca, skupno]);
  const calls: { path: string; body?: unknown }[] = [];
  const api: ApiClient = {
    ...groups,
    request: (async (method: string, path: string, body?: unknown) => {
      if (path.endsWith('/restore')) {
        calls.push({ path, body });
        return restore ? restore() : { ...ana, phone: snapshot1.phone, tags: ['a'], revision: 6 };
      }
      if (/\/versions\/1$/.test(path))
        return { number: 1, userName: 'Marko Pirc', createdAt: list[2]!.createdAt, snapshot: snapshot1 };
      if (path.endsWith('/versions')) return list;
      if (/\/contacts\/c-1$/.test(path)) return ana;
      return groups.request(method, path, body);
    }) as ApiClient['request'],
  };
  return { api, calls };
}

const open = (path = '/groups/g-1/contacts/c-1/versions') =>
  renderApp(path, { auth: signedIn(), api: versionsApi().api });

describe('Versions tab (R-020-01 to R-020-03, R-020-07, R-020-09)', () => {
  it('lists the versions newest first with who and when', async () => {
    open();
    const rows = await screen.findAllByRole('row');
    expect(within(rows[1]!).getByText('Version 3')).toBeInTheDocument();
    expect(within(rows[3]!).getByText('Version 1')).toBeInTheDocument();
    expect(within(rows[1]!).getByText('Marko Pirc')).toBeInTheDocument();
  });

  it('opens a version read-only', async () => {
    open();
    const row = (await screen.findByText('Version 1')).closest('tr') as HTMLElement;
    await userEvent.click(within(row).getByRole('button', { name: 'View' }));
    expect(await screen.findByText('+386 40 123 456')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Save' })).not.toBeInTheDocument();
  });

  it('highlights only the rows that differ in the comparison', async () => {
    open();
    const row = (await screen.findByText('Version 1')).closest('tr') as HTMLElement;
    await userEvent.click(within(row).getByRole('button', { name: 'Compare' }));
    const phone = (await screen.findByRole('rowheader', { name: /Phone/ })).closest('tr') as HTMLElement;
    expect(within(phone).getByText('Changed')).toBeInTheDocument();
    expect(within(phone).getByText('+386 40 123 456')).toBeInTheDocument();
    expect(within(phone).getByText('+386 41 555 123')).toBeInTheDocument();
    const first = screen.getByRole('rowheader', { name: /First name/ }).closest('tr') as HTMLElement;
    expect(within(first).queryByText('Changed')).not.toBeInTheDocument();
    expect(screen.getByText('Highlighted rows differ.')).toBeInTheDocument();
  });

  it('shows a Viewer no Versions tab', async () => {
    renderApp('/groups/g-3/contacts/c-1', { auth: signedIn(), api: versionsApi().api });
    await screen.findByRole('heading', { name: 'Ana Novak' });
    expect(screen.queryByRole('link', { name: 'Versions' })).not.toBeInTheDocument();
  });
});

describe('Restore (R-020-04, R-020-06, R-020-08, R-020-09)', () => {
  async function askRestore(fake: ReturnType<typeof versionsApi>) {
    renderApp('/groups/g-1/contacts/c-1/versions', { auth: signedIn(), api: fake.api });
    const row = (await screen.findByText('Version 1')).closest('tr') as HTMLElement;
    await userEvent.click(within(row).getByRole('button', { name: 'Restore' }));
    return screen.getByRole('dialog');
  }

  it('changes nothing when the dialog is cancelled', async () => {
    const fake = versionsApi();
    const dialog = await askRestore(fake);
    await userEvent.click(within(dialog).getByRole('button', { name: 'Cancel' }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(fake.calls).toHaveLength(0);
  });

  it('sends the revision the tab opened with after the confirmation', async () => {
    const fake = versionsApi();
    const dialog = await askRestore(fake);
    await userEvent.click(within(dialog).getByRole('button', { name: 'Restore this version' }));
    expect(await screen.findByText('The version was restored.')).toBeInTheDocument();
    expect(fake.calls).toEqual([
      { path: '/groups/g-1/contacts/c-1/versions/1/restore', body: { revision: 5 } },
    ]);
  });

  it('says so when the version equals the current contact', async () => {
    const fake = versionsApi(() => {
      throw new ApiError(409, { status: 409, code: 'NOTHING_TO_CHANGE', message: 'x' });
    });
    const dialog = await askRestore(fake);
    await userEvent.click(within(dialog).getByRole('button', { name: 'Restore this version' }));
    expect(
      await screen.findByText('There is nothing to change: the contact already has these values.'),
    ).toBeInTheDocument();
  });

  it('tells the user to restore the contact from Trash first', async () => {
    const fake = versionsApi(() => {
      throw new ApiError(409, { status: 409, code: 'CONTACT_IN_TRASH', message: 'x' });
    });
    const dialog = await askRestore(fake);
    await userEvent.click(within(dialog).getByRole('button', { name: 'Restore this version' }));
    expect(await screen.findByText(/Restore it from Trash first/)).toBeInTheDocument();
  });

  it('shows a conflict as the usual conflict text', async () => {
    const fake = versionsApi(() => {
      throw new ApiError(409, { status: 409, code: 'CONFLICT', message: 'x' });
    });
    const dialog = await askRestore(fake);
    await userEvent.click(within(dialog).getByRole('button', { name: 'Restore this version' }));
    expect(await screen.findByText('This contact was changed by someone else.')).toBeInTheDocument();
  });
});
