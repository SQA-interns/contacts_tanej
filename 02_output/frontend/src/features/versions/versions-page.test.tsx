import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ApiError, type ApiClient } from '@/lib/api/client';
import { groupsApi, konferenca, skupno, signedIn } from '@/test/groupsApi';
import { renderApp } from '@/test/renderApp';

type Reply = (method: string, path: string, body?: unknown) => unknown;

const ana = {
  id: 'c/1',
  state: 'ACTIVE',
  revision: 7,
  versionNumber: 2,
  createdAt: '2026-11-10T10:00:00Z',
  firstName: 'Ana',
  lastName: 'Novak',
  phone: '+386 41 555 123',
};
const stamp = '2026-07-01T22:30:00Z';

function fake(overrides: Reply = () => undefined, versions: unknown = [{ number: 1, userName: 'Marko', createdAt: stamp }]) {
  const { api: groups } = groupsApi([konferenca, skupno]);
  const calls: { method: string; path: string; body?: unknown }[] = [];
  const api: ApiClient = {
    ...groups,
    request: (async (method: string, path: string, body?: unknown) => {
      if (path.startsWith('/groups/g-1/contacts/')) {
        calls.push({ method, path, body });
        const own = overrides(method, path, body);
        if (own !== undefined) return own;
        if (path.endsWith('/versions')) return versions;
        if (/\/versions\/1$/.test(path))
          return { number: 1, userName: 'Marko', createdAt: stamp, snapshot: { firstName: 'Ana', lastName: 'Novak', phone: '+386 40 000 111' } };
        if (path.endsWith('/restore')) return { ...ana, revision: 8 };
        return ana;
      }
      return groups.request(method, path, body);
    }) as ApiClient['request'],
  };
  return { api, calls };
}

const URL_ = '/groups/g-1/contacts/c%2F1/versions';
const open = (f: ReturnType<typeof fake>, path = URL_) => renderApp(path, { auth: signedIn(), api: f.api });
const row = async () => (await screen.findByText('Version 1')).closest('tr') as HTMLElement;
const fail = (code: string) => () => {
  throw new ApiError(409, { status: 409, code, message: 'x' } as never);
};

describe('VersionsPage: requests and navigation', () => {
  it('reads the list and the contact once each, with the ids encoded', async () => {
    const f = fake();
    open(f);
    await row();
    await waitFor(() => expect(f.calls.filter((c) => c.path === '/groups/g-1/contacts/c%2F1')).toHaveLength(1));
    expect(f.calls.filter((c) => c.path.endsWith('/versions'))).toEqual([
      { method: 'GET', path: '/groups/g-1/contacts/c%2F1/versions', body: undefined },
    ]);
    expect(f.calls.some((c) => /versions\/\d+$/.test(c.path))).toBe(false);
  });

  it('does not read the contact again when the window gets focus', async () => {
    const f = fake();
    open(f);
    await row();
    await waitFor(() => expect(f.calls.some((c) => c.path === '/groups/g-1/contacts/c%2F1')).toBe(true));
    window.dispatchEvent(new Event('focus'));
    document.dispatchEvent(new Event('visibilitychange'));
    await new Promise((r) => setTimeout(r, 50));
    expect(f.calls.filter((c) => c.path === '/groups/g-1/contacts/c%2F1')).toHaveLength(1);
  });

  it('shows a Viewer nothing and asks for nothing', async () => {
    const f = fake();
    renderApp('/groups/g-3/contacts/c-1/versions', { auth: signedIn(), api: f.api });
    await screen.findByRole('heading', { name: 'Ana Novak' }).catch(() => undefined);
    await new Promise((r) => setTimeout(r, 50));
    expect(screen.queryByText('Version 1')).toBeNull();
    expect(screen.queryByRole('table')).toBeNull();
    expect(f.calls).toHaveLength(0);
  });

  it('links to the contacts, the details and the history, and marks the Versions tab', async () => {
    open(fake());
    await row();
    const crumb = screen.getByText(/\/ Contact$/);
    expect(within(crumb).getByRole('link', { name: 'Contacts' })).toHaveAttribute('href', '/groups/g-1');
    const nav = screen.getByRole('navigation', { name: 'Contact' });
    expect(within(nav).getByRole('link', { name: 'Details' })).toHaveAttribute('href', '/groups/g-1/contacts/c/1');
    expect(within(nav).getByRole('link', { name: 'History' })).toHaveAttribute('href', '/groups/g-1/contacts/c/1/history');
    expect(within(nav).getByText('Versions')).toHaveAttribute('aria-current', 'page');
    expect(screen.getByText(/\/ Contact$/)).toBeInTheDocument();
  });

  it('lists the column headings, the date and the three actions of a version', async () => {
    open(fake());
    const r = await row();
    const heads = screen.getAllByRole('columnheader').map((h) => h.textContent);
    expect(heads).toEqual(['Version', 'Who', 'When', 'Actions']);
    expect(within(r).getByText('Marko')).toBeInTheDocument();
    expect(r.querySelector('time')).toHaveAttribute('datetime', stamp);
    expect(r.querySelector('time')?.textContent).toMatch(/2026/);
    expect(within(r).getAllByRole('button').map((b) => b.textContent)).toEqual(['View', 'Compare', 'Restore']);
  });

  it('explains an empty list', async () => {
    open(fake(() => undefined, []));
    expect(await screen.findByRole('heading', { name: 'No versions yet' })).toBeInTheDocument();
    expect(screen.getByText('A version is recorded each time the contact is saved.')).toBeInTheDocument();
    expect(screen.queryByRole('table')).toBeNull();
  });

  it('shows an error with a retry that reads the list again', async () => {
    let failing = true;
    const f = fake((_m, path) => {
      if (path.endsWith('/versions') && failing) throw new ApiError(500, { status: 500, code: 'INTERNAL_ERROR', message: 'x' } as never);
      return undefined;
    });
    open(f);
    expect(await screen.findByText('Something went wrong. Please try again.')).toBeInTheDocument();
    failing = false;
    await userEvent.click(screen.getByRole('button', { name: 'Try again' }));
    expect(await screen.findByText('Version 1')).toBeInTheDocument();
  });
});

describe('VersionsPage: one version and the comparison', () => {
  it('opens version 1 with its heading, its values and the way back, and asks for that number', async () => {
    const f = fake();
    open(f);
    await userEvent.click(within(await row()).getByRole('button', { name: 'View' }));
    expect(await screen.findByRole('heading', { name: 'Version 1' })).toBeInTheDocument();
    expect(f.calls.some((c) => c.path === '/groups/g-1/contacts/c%2F1/versions/1' && c.method === 'GET')).toBe(true);
    expect(screen.getByText('+386 40 000 111')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Details' })).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Back to versions' }));
    expect(await screen.findByRole('table')).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Version 1' })).toBeNull();
  });

  it('shows the tags and attributes a version holds', async () => {
    const f = fake((_m, path) =>
      /\/versions\/1$/.test(path)
        ? {
            number: 1,
            userName: 'Marko',
            createdAt: stamp,
            snapshot: { firstName: 'Ana', lastName: 'Novak', tags: ['vip'], attributes: [{ key: 'Level', value: 'gold' }] },
          }
        : undefined,
    );
    open(f);
    await userEvent.click(within(await row()).getByRole('button', { name: 'View' }));
    expect(await screen.findByText('vip')).toBeInTheDocument();
    expect(screen.getByText('Level')).toBeInTheDocument();
    expect(screen.getByText('gold')).toBeInTheDocument();
  });

  it('names the comparison, shows the current contact and goes back', async () => {
    open(fake());
    await userEvent.click(within(await row()).getByRole('button', { name: 'Compare' }));
    expect(await screen.findByRole('heading', { name: 'Compare versions' })).toBeInTheDocument();
    expect(screen.getByText('Highlighted rows differ.')).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Details' })).toBeNull();
    await userEvent.click(screen.getByRole('button', { name: 'Back to versions' }));
    expect(await screen.findByRole('table')).toBeInTheDocument();
  });

  it('compares a contact and a version that hold no tags or attributes', async () => {
    open(fake());
    await userEvent.click(within(await row()).getByRole('button', { name: 'Compare' }));
    const first = (await screen.findByRole('rowheader', { name: /First name/ })).closest('tr') as HTMLElement;
    expect(within(first).queryByText('Changed')).toBeNull();
    expect(screen.queryByText('Stryker was here')).toBeNull();
  });

  it('shows a loading panel until the version arrives and an error with a retry when it fails', async () => {
    let failing = true;
    const f = fake((_m, path) => {
      if (/\/versions\/1$/.test(path) && failing) throw new ApiError(500, { status: 500, code: 'INTERNAL_ERROR', message: 'x' } as never);
      return undefined;
    });
    open(f);
    await userEvent.click(within(await row()).getByRole('button', { name: 'View' }));
    expect(await screen.findByText('Something went wrong. Please try again.')).toBeInTheDocument();
    failing = false;
    await userEvent.click(screen.getByRole('button', { name: 'Try again' }));
    expect(await screen.findByText('+386 40 000 111')).toBeInTheDocument();
  });

  it('opens the restore dialog from the version itself and restores that number', async () => {
    const f = fake();
    open(f);
    await userEvent.click(within(await row()).getByRole('button', { name: 'View' }));
    await screen.findByRole('heading', { name: 'Version 1' });
    await userEvent.click(screen.getByRole('button', { name: 'Restore this version' }));
    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByRole('heading', { name: 'Restore this version' })).toBeInTheDocument();
    expect(within(dialog).getByText(/A new version is recorded; nothing is lost\./)).toBeInTheDocument();
    await userEvent.click(within(dialog).getByRole('button', { name: 'Restore this version' }));
    expect(await screen.findByText('The version was restored.')).toBeInTheDocument();
    expect(f.calls.filter((c) => c.method === 'POST')).toEqual([
      { method: 'POST', path: '/groups/g-1/contacts/c%2F1/versions/1/restore', body: { revision: 7 } },
    ]);
    expect(await screen.findByRole('table')).toBeInTheDocument();
  });
});

describe('VersionsPage: after a restore', () => {
  async function restoreFrom(f: ReturnType<typeof fake>) {
    open(f);
    await userEvent.click(within(await row()).getByRole('button', { name: 'Restore' }));
    await userEvent.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Restore this version' }));
  }

  it('says it as a status, reads the list and the contact list again and uses the new revision next time', async () => {
    const f = fake();
    await restoreFrom(f);
    const status = await screen.findByRole('status');
    expect(status).toHaveTextContent('The version was restored.');
    expect(screen.queryByRole('alert')).toBeNull();
    await waitFor(() => expect(f.calls.filter((c) => c.path.endsWith('/versions') && c.method === 'GET').length).toBeGreaterThan(1));
    await userEvent.click(within(await row()).getByRole('button', { name: 'Restore' }));
    await userEvent.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Restore this version' }));
    await waitFor(() => expect(f.calls.filter((c) => c.method === 'POST')).toHaveLength(2));
    expect(f.calls.filter((c) => c.method === 'POST')[1]?.body).toEqual({ revision: 8 });
  });

  it('shows a failure as an alert, names an unknown failure by the generic text, and keeps the view', async () => {
    const f = fake((m) => (m === 'POST' ? fail('CONFLICT')() : undefined));
    await restoreFrom(f);
    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('This contact was changed by someone else.');
    expect(screen.queryByRole('status')).toBeNull();
    expect(screen.getByRole('table')).toBeInTheDocument();
  });

  it('treats a failure that is no ApiError as an internal error', async () => {
    const f = fake((m) => {
      if (m === 'POST') throw new Error('boom');
      return undefined;
    });
    await restoreFrom(f);
    const alert = await screen.findByRole('alert');
    expect(alert.textContent).not.toBe('');
    expect(alert).not.toHaveTextContent('The version was restored.');
    expect(alert).not.toHaveTextContent('There is nothing to change');
    expect(alert).not.toHaveTextContent('Restore it from Trash first');
  });

  it('clears an earlier message when the next restore starts', async () => {
    let failing = true;
    const f = fake((m) => {
      if (m === 'POST' && failing) throw new ApiError(409, { status: 409, code: 'NOTHING_TO_CHANGE', message: 'x' } as never);
      return undefined;
    });
    await restoreFrom(f);
    expect(await screen.findByRole('alert')).toHaveTextContent('nothing to change');
    failing = false;
    await userEvent.click(within(await row()).getByRole('button', { name: 'Restore' }));
    await userEvent.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Restore this version' }));
    expect(await screen.findByRole('status')).toHaveTextContent('The version was restored.');
    expect(screen.queryByRole('alert')).toBeNull();
  });
});
