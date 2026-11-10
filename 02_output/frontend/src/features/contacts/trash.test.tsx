import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ApiClient } from '@/lib/api/client';
import type { components } from '@/lib/api/schema';
import { groupsApi, konferenca, poroka, skupno, signedIn } from '@/test/groupsApi';
import { renderApp } from '@/test/renderApp';

type Item = components['schemas']['TrashedContactPage']['items'][number];

const novak: Item = {
  id: 'c-1',
  firstName: 'Ana',
  lastName: 'Novak',
  trashedAt: '2026-11-01T10:00:00Z',
  daysLeft: 30,
};

/** The groups stand-in plus the Trash of every group: the view, restore and Delete forever. */
function trashApi(items: Item[]) {
  const { api: groups, calls } = groupsApi([konferenca, poroka, skupno]);
  const list = [...items];
  const api: ApiClient = {
    ...groups,
    request: (async (method: string, path: string, body?: unknown) => {
      if (path.includes('/trash-contacts')) {
        return { items: list, total: list.length, page: 1, pageSize: 25 };
      }
      const act = /^\/groups\/g-\d\/contacts\/(c-\d)(\/restore)?$/.exec(path);
      if (act && (method === 'POST' || method === 'DELETE')) {
        calls.push(`${method} ${path}`);
        list.splice(
          list.findIndex((i) => i.id === act[1]),
          1,
        );
        return undefined;
      }
      return groups.request(method, path, body);
    }) as ApiClient['request'],
  };
  return { api, calls };
}

describe('Trash view (R-012-02, R-012-03, R-012-04, R-012-08)', () => {
  it('lists the contact with its days left and gives an Owner both actions', async () => {
    const { api } = trashApi([novak]);
    renderApp('/groups/g-1/trash', { auth: signedIn(), api });
    const row = await screen.findByRole('row', { name: /Ana Novak/ });
    expect(within(row).getByText('30')).toBeInTheDocument();
    expect(within(row).getByRole('button', { name: 'Restore' })).toBeInTheDocument();
    expect(within(row).getByRole('button', { name: 'Delete forever' })).toBeInTheDocument();
  });

  it('gives an Editor "Restore" but not "Delete forever"', async () => {
    const { api } = trashApi([novak]);
    renderApp('/groups/g-2/trash', { auth: signedIn(), api });
    const row = await screen.findByRole('row', { name: /Ana Novak/ });
    expect(within(row).getByRole('button', { name: 'Restore' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Delete forever' })).not.toBeInTheDocument();
  });

  it('shows a Viewer no Trash tab and no Trash view', async () => {
    const { api } = trashApi([novak]);
    renderApp('/groups/g-3/trash', { auth: signedIn(), api });
    await screen.findByRole('heading', { name: 'Skupno' });
    expect(screen.queryByRole('link', { name: 'Trash' })).not.toBeInTheDocument();
    expect(screen.queryByRole('row', { name: /Ana Novak/ })).not.toBeInTheDocument();
  });

  it('restores a contact and says so', async () => {
    const { api, calls } = trashApi([novak]);
    renderApp('/groups/g-1/trash', { auth: signedIn(), api });
    await userEvent.click(await screen.findByRole('button', { name: 'Restore' }));
    expect(await screen.findByText('The contact was restored.')).toBeInTheDocument();
    expect(calls).toContain('POST /groups/g-1/contacts/c-1/restore');
    expect(await screen.findByText(/Trash is empty/)).toBeInTheDocument();
  });

  it('erases only after the confirmation and keeps the contact on Cancel (AC-012-09)', async () => {
    const { api, calls } = trashApi([novak]);
    renderApp('/groups/g-1/trash', { auth: signedIn(), api });
    await userEvent.click(await screen.findByRole('button', { name: 'Delete forever' }));
    const dialog = screen.getByRole('dialog');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Cancel' }));
    expect(calls.filter((c) => !c.startsWith('GET'))).toEqual([]);
    expect(screen.getByRole('row', { name: /Ana Novak/ })).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Delete forever' }));
    await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Delete forever' }));
    expect(calls.filter((c) => !c.startsWith('GET'))).toEqual(['DELETE /groups/g-1/contacts/c-1']);
    expect(await screen.findByText(/Trash is empty/)).toBeInTheDocument();
  });
});
