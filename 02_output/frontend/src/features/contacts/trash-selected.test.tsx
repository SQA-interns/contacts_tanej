import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ApiClient } from '@/lib/api/client';
import { groupsApi, konferenca, signedIn } from '@/test/groupsApi';
import { renderApp } from '@/test/renderApp';

const rows = [
  { id: 'c-1', lastName: 'Novak', firstName: 'Ana', tags: [] },
  { id: 'c-2', lastName: 'Kos', firstName: 'Cene', tags: [] },
];

/** A group with two contacts; records every trash request. */
function listApi() {
  const base = groupsApi([konferenca]).api;
  const trashed: string[] = [];
  const api: ApiClient = {
    ...base,
    request: (async (method: string, path: string, body?: unknown) => {
      if (method === 'POST' && path === '/groups/g-1/contacts/search') {
        return { items: rows, total: rows.length, page: 1, pageSize: 25 };
      }
      const trash = /^\/groups\/g-1\/contacts\/(c-\d)\/trash$/.exec(path);
      if (method === 'POST' && trash) {
        trashed.push(trash[1]!);
        return undefined;
      }
      return base.request(method, path, body as never);
    }) as ApiClient['request'],
  };
  return { api, trashed };
}

/** The first render of the whole app can take longer than the default second on a busy machine. */
const SLOW = { timeout: 5000 };

async function confirm() {
  const dialog = await screen.findByRole('dialog');
  expect(dialog).toHaveTextContent('Selected contacts: ');
  await userEvent.click(within(dialog).getByRole('button', { name: 'Move to trash' }));
}

describe('"Move to trash" for the ticked contacts (post-run)', () => {
  it('is disabled while nothing is ticked', async () => {
    const { api } = listApi();
    renderApp('/groups/g-1', { auth: signedIn(), api });
    expect(await screen.findByRole('button', { name: 'Move to trash' }, SLOW)).toBeDisabled();
  });

  it('moves only the ticked contacts, after the confirmation', async () => {
    const spy = listApi();
    renderApp('/groups/g-1', { auth: signedIn(), api: spy.api });
    const boxes = await screen.findAllByRole('checkbox', { name: 'Select contact' }, SLOW);
    await userEvent.click(boxes[0]!);
    await userEvent.click(screen.getByRole('button', { name: 'Move to trash' }));
    expect(spy.trashed).toEqual([]);
    await confirm();
    await waitFor(() => expect(spy.trashed).toEqual(['c-1']));
    // The selection is cleared afterwards (the toast itself is tested in toast.test.tsx).
    expect(await screen.findByText('Nothing selected')).toBeInTheDocument();
  });

  it('moves every contact of the filter for "select all results"', async () => {
    const spy = listApi();
    renderApp('/groups/g-1', { auth: signedIn(), api: spy.api });
    await userEvent.click(await screen.findByRole('button', { name: 'Select all 2 results' }, SLOW));
    await userEvent.click(screen.getByRole('button', { name: 'Move to trash' }));
    await confirm();
    await waitFor(() => expect(spy.trashed).toEqual(['c-1', 'c-2']));
  });
});
