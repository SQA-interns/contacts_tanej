import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ApiClient } from '@/lib/api/client';
import type { components } from '@/lib/api/schema';
import { groupsApi, konferenca, skupno, signedIn } from '@/test/groupsApi';
import { renderApp } from '@/test/renderApp';

type Entry = components['schemas']['HistoryEntry'];

const edited: Entry = {
  id: 'e-1',
  changeSetId: 's-1',
  eventType: 'CONTACT_EDITED',
  contactId: 'c-1',
  actor: { id: 'u-2', name: 'Marko Pirc', email: 'marko@example.com' },
  createdAt: '2026-07-01T22:30:00Z',
  erased: false,
  changes: [{ field: 'phone', oldValue: '+386 40 123 456', newValue: '+386 41 555 123' }],
};

function historyApi(items: Entry[]) {
  const { api: groups } = groupsApi([konferenca, skupno]);
  const seen: string[] = [];
  const api: ApiClient = {
    ...groups,
    request: (async (method: string, path: string, body?: unknown) => {
      if (path.includes('/history/actors')) {
        return [
          { id: 'u-1', name: 'Ana Novak' },
          { id: 'u-2', name: 'Marko Pirc' },
        ];
      }
      if (path.includes('/history')) {
        seen.push(path);
        const empty = path.includes('types=GROUP_DELETED');
        return { items: empty ? [] : items, total: empty ? 0 : items.length, page: 1, pageSize: 25 };
      }
      return groups.request(method, path, body);
    }) as ApiClient['request'],
  };
  return { api, seen };
}

describe('Group History (R-014-01 to R-014-08)', () => {
  it('offers the History tab to an Owner and shows the entries', async () => {
    const { api } = historyApi([edited]);
    renderApp('/groups/g-1', { auth: signedIn(), api });
    await userEvent.click(await screen.findByRole('link', { name: 'History' }));
    expect(await screen.findByText('marko@example.com')).toBeInTheDocument();
    expect(screen.getByRole('list', { name: 'Newest first' })).toHaveTextContent('Contact edited');
  });

  it('shows a Viewer no History tab', async () => {
    const { api } = historyApi([]);
    renderApp('/groups/g-3', { auth: signedIn(), api });
    await screen.findByRole('link', { name: 'Members' });
    expect(screen.queryByRole('link', { name: 'History' })).not.toBeInTheDocument();
  });

  it('sends the person, the event type and the days when the filter is applied', async () => {
    const { api, seen } = historyApi([edited]);
    renderApp('/groups/g-1/history', { auth: signedIn(), api });
    await screen.findByText('marko@example.com');
    await userEvent.selectOptions(await screen.findByLabelText('Person'), 'u-2');
    await userEvent.selectOptions(screen.getByLabelText('Event type'), 'CONTACT_EDITED');
    await userEvent.type(screen.getByLabelText('From'), '2026-07-02');
    await userEvent.type(screen.getByLabelText('To'), '2026-07-02');
    await userEvent.click(screen.getByRole('button', { name: 'Apply' }));
    await screen.findByText('marko@example.com');
    const last = seen[seen.length - 1]!;
    expect(last).toContain('actorId=u-2');
    expect(last).toContain('types=CONTACT_EDITED');
    expect(last).toContain('from=2026-07-02');
    expect(last).toContain('to=2026-07-02');
  });

  it('says "No events match" with "Clear filters" and clears them', async () => {
    const { api, seen } = historyApi([edited]);
    renderApp('/groups/g-1/history', { auth: signedIn(), api });
    await screen.findByText('marko@example.com');
    await userEvent.selectOptions(screen.getByLabelText('Event type'), 'GROUP_DELETED');
    await userEvent.click(screen.getByRole('button', { name: 'Apply' }));
    expect(await screen.findByRole('heading', { name: 'No events match' })).toBeInTheDocument();
    await userEvent.click(screen.getAllByRole('button', { name: 'Clear filters' })[0]!);
    expect(await screen.findByText('marko@example.com')).toBeInTheDocument();
    expect(seen[seen.length - 1]).not.toContain('types=');
  });

  it('refuses an end before the start with the reason and keeps the list', async () => {
    const { api, seen } = historyApi([edited]);
    renderApp('/groups/g-1/history', { auth: signedIn(), api });
    await screen.findByText('marko@example.com');
    const before = seen.length;
    await userEvent.type(screen.getByLabelText('From'), '2026-07-03');
    await userEvent.type(screen.getByLabelText('To'), '2026-07-02');
    await userEvent.click(screen.getByRole('button', { name: 'Apply' }));
    expect(screen.getByText('The end date must not be before the start date.')).toBeInTheDocument();
    expect(seen).toHaveLength(before);
    expect(screen.getByText('marko@example.com')).toBeInTheDocument();
  });

  it('names an erased contact with the erasure marker, not its name (BR-06)', async () => {
    const erased: Entry = { ...edited, eventType: 'CONTACT_ERASED', erased: true, changes: [] };
    const { api } = historyApi([erased]);
    renderApp('/groups/g-1/history', { auth: signedIn(), api });
    expect(await screen.findByRole('list', { name: 'Newest first' })).toHaveTextContent('[erased]');
  });

  it('shows the empty state of a group without History', async () => {
    const { api } = historyApi([]);
    renderApp('/groups/g-1/history', { auth: signedIn(), api });
    expect(await screen.findByRole('heading', { name: 'No History yet' })).toBeInTheDocument();
  });
});
