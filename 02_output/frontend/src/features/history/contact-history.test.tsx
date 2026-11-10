import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ApiClient } from '@/lib/api/client';
import type { components } from '@/lib/api/schema';
import { groupsApi, konferenca, skupno, signedIn } from '@/test/groupsApi';
import { renderApp } from '@/test/renderApp';

type Entry = components['schemas']['HistoryEntry'];
type Contact = components['schemas']['Contact'];

const ana: Contact = {
  id: 'c-1',
  state: 'ACTIVE',
  revision: 2,
  versionNumber: 2,
  createdAt: '2026-11-10T10:00:00Z',
  firstName: 'Ana',
  lastName: 'Novak',
  tags: [],
  attributes: [],
};

const edited: Entry = {
  id: 'e-1',
  changeSetId: 's-1',
  eventType: 'CONTACT_EDITED',
  contactId: 'c-1',
  actor: { id: 'u-2', name: 'Marko Pirc', email: 'marko@example.com' },
  createdAt: '2026-07-01T22:30:00Z',
  erased: false,
  changes: [
    { field: 'phone', oldValue: '+386 40 123 456', newValue: '+386 41 555 123' },
    { field: 'company', oldValue: '', newValue: 'Acme' },
  ],
};

function historyApi(items: Entry[]) {
  const { api: groups } = groupsApi([konferenca, skupno]);
  const api: ApiClient = {
    ...groups,
    request: (async (method: string, path: string, body?: unknown) => {
      if (path.includes('/history')) return { items, total: items.length, page: 1, pageSize: 25 };
      if (/\/contacts\/c-1$/.test(path)) return ana;
      return groups.request(method, path, body);
    }) as ApiClient['request'],
  };
  return api;
}

describe('Contact History tab (R-013-07, R-013-09, R-013-12)', () => {
  it('shows who, when, the field and the old and new value, in the project time zone', async () => {
    renderApp('/groups/g-1/contacts/c-1/history', { auth: signedIn(), api: historyApi([edited]) });
    expect(await screen.findByText('Marko Pirc')).toBeInTheDocument();
    expect(screen.getByText('marko@example.com')).toBeInTheDocument();
    expect(screen.getByText('2026-07-02 00:30')).toBeInTheDocument();
    expect(screen.getByText('Phone')).toBeInTheDocument();
    expect(screen.getByText('+386 40 123 456')).toBeInTheDocument();
    expect(screen.getByText('+386 41 555 123')).toBeInTheDocument();
  });

  it('shows an old value that was empty as "empty", not as missing', async () => {
    renderApp('/groups/g-1/contacts/c-1/history', { auth: signedIn(), api: historyApi([edited]) });
    const company = (await screen.findByText('Company')).closest('li') as HTMLElement;
    expect(within(company).getByText('empty')).toBeInTheDocument();
    expect(within(company).getByText('Acme')).toBeInTheDocument();
  });

  it('shows the erasure marker instead of the values of an erased contact', async () => {
    const erased: Entry = { ...edited, erased: true, changes: [{ field: 'phone' }] };
    renderApp('/groups/g-1/contacts/c-1/history', { auth: signedIn(), api: historyApi([erased]) });
    expect(await screen.findAllByText('[erased]')).toHaveLength(2);
    expect(screen.queryByText('+386 41 555 123')).not.toBeInTheDocument();
  });

  it('says so when there is no History yet', async () => {
    renderApp('/groups/g-1/contacts/c-1/history', { auth: signedIn(), api: historyApi([]) });
    expect(await screen.findByText('No History yet')).toBeInTheDocument();
  });

  it('offers a History tab to an Owner and none to a Viewer', async () => {
    renderApp('/groups/g-1/contacts/c-1', { auth: signedIn(), api: historyApi([]) });
    await userEvent.click(await screen.findByRole('link', { name: 'History' }));
    expect(await screen.findByText('No History yet')).toBeInTheDocument();
  });

  it('shows a Viewer no History tab', async () => {
    renderApp('/groups/g-3/contacts/c-1', { auth: signedIn(), api: historyApi([]) });
    await screen.findByRole('heading', { name: 'Ana Novak' });
    expect(screen.queryByRole('link', { name: 'History' })).not.toBeInTheDocument();
  });
});
