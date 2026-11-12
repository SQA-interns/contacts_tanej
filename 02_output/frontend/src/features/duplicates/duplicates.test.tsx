import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ApiError, type ApiClient } from '@/lib/api/client';
import type { components } from '@/lib/api/schema';
import { groupsApi, konferenca, skupno, signedIn } from '@/test/groupsApi';
import { renderApp } from '@/test/renderApp';

type DuplicateSet = components['schemas']['DuplicateSet'];

const member = (id: string, lastName: string, firstName = '', email = '') => ({
  id,
  lastName,
  firstName,
  email,
  phone: '',
  company: '',
  city: '',
  country: '',
  tags: [],
  createdAt: '2026-11-10T10:00:00Z',
});

const certain: DuplicateSet = {
  kind: 'CERTAIN',
  reasons: ['SAME_EMAIL'],
  contacts: [member('c-1', 'Novak', 'Ana', 'ana@example.com'), member('c-2', 'Nova', '', 'ana@example.com')],
};
const probable: DuplicateSet = {
  kind: 'PROBABLE',
  reasons: ['SAME_NAME_AND_COMPANY', 'SAME_NAME_AND_PHONE'],
  contacts: [member('c-3', 'Horvat', 'Eva'), member('c-4', 'Horvat', 'Eva')],
};

function duplicatesApi(sets: DuplicateSet[], search?: () => Promise<unknown>) {
  const { api: groups } = groupsApi([konferenca, skupno]);
  const calls: { path: string; body?: unknown }[] = [];
  const api: ApiClient = {
    ...groups,
    request: (async (method: string, path: string, body?: unknown) => {
      if (path.endsWith('/duplicates/search')) {
        calls.push({ path });
        return search ? search() : { sets };
      }
      if (path.endsWith('/duplicates/dismissals')) {
        calls.push({ path, body });
        return undefined;
      }
      return groups.request(method, path, body);
    }) as ApiClient['request'],
  };
  return { api, calls };
}

const open = (sets: DuplicateSet[], search?: () => Promise<unknown>, group = 'g-1') => {
  const fake = duplicatesApi(sets, search);
  renderApp(`/groups/${group}/duplicates`, { auth: signedIn(), api: fake.api });
  return fake;
};

describe('Duplicates tab (R-021-01, R-021-07, R-021-09, R-021-11, R-021-12)', () => {
  it('searches only when the user presses "Find duplicates" (R-021-08)', async () => {
    const { calls } = open([certain]);
    expect(await screen.findByRole('heading', { name: 'Find duplicates' })).toBeInTheDocument();
    expect(calls).toHaveLength(0);
    await userEvent.click(screen.getAllByRole('button', { name: 'Find duplicates' })[0]!);
    expect(await screen.findByText('Certain')).toBeInTheDocument();
    expect(calls).toHaveLength(1);
  });

  it('shows each set with its kind, the reasons and its contacts', async () => {
    open([certain, probable]);
    await userEvent.click((await screen.findAllByRole('button', { name: 'Find duplicates' }))[0]!);
    const sets = await screen.findAllByRole('listitem');
    expect(within(sets[0]!).getByText('Certain')).toBeInTheDocument();
    expect(within(sets[0]!).getByText('Same e-mail address')).toBeInTheDocument();
    expect(within(sets[0]!).getByText('Ana Novak')).toBeInTheDocument();
    expect(within(sets[0]!).getByText('Nova')).toBeInTheDocument();
    expect(within(sets[1]!).getByText('Probable')).toBeInTheDocument();
    expect(within(sets[1]!).getByText('Same name and company, same name and phone')).toBeInTheDocument();
  });

  it('says "No duplicates found" when there is no set', async () => {
    open([]);
    await userEvent.click((await screen.findAllByRole('button', { name: 'Find duplicates' }))[0]!);
    expect(await screen.findByRole('heading', { name: 'No duplicates found' })).toBeInTheDocument();
  });

  it('hides a set after "Not a duplicate" and sends exactly its contacts', async () => {
    const { calls } = open([certain, probable]);
    await userEvent.click((await screen.findAllByRole('button', { name: 'Find duplicates' }))[0]!);
    const first = (await screen.findAllByRole('listitem'))[0]!;
    await userEvent.click(within(first).getByRole('button', { name: 'Not a duplicate' }));
    expect(screen.queryByText('Certain')).not.toBeInTheDocument();
    expect(screen.getByText('Probable')).toBeInTheDocument();
    const mark = calls.find((c) => c.path.endsWith('/dismissals'));
    expect(mark?.body).toEqual({ contactIds: ['c-1', 'c-2'] });
  });

  it('shows the error state with "Try again" when the search fails', async () => {
    open([], async () => {
      throw new ApiError(503, { status: 503, code: 'UNAVAILABLE', message: 'x' });
    });
    await userEvent.click((await screen.findAllByRole('button', { name: 'Find duplicates' }))[0]!);
    expect(await screen.findByRole('alert')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument();
  });

  it('shows a Viewer no Duplicates tab and no button (R-021-11)', async () => {
    open([], undefined, 'g-3');
    expect(await screen.findByRole('heading', { level: 1, name: 'Skupno' })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Duplicates' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Find duplicates' })).not.toBeInTheDocument();
  });

  it('has the Duplicates tab for an Owner', async () => {
    open([]);
    expect(await screen.findByRole('link', { name: 'Duplicates' })).toHaveAttribute(
      'href',
      '/groups/g-1/duplicates',
    );
  });
});
