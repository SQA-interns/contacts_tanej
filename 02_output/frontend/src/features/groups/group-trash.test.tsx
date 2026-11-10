import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ApiClient } from '@/lib/api/client';
import type { components } from '@/lib/api/schema';
import { renderApp } from '@/test/renderApp';
import { failure, groupsApi, konferenca, poroka, signedIn } from '@/test/groupsApi';

type DeletedGroup = components['schemas']['DeletedGroup'];

/** The groups stand-in plus the delete, restore and Deleted groups calls of US-015. */
function trashApi(initial = [konferenca, poroka], options: { restoreFails?: boolean } = {}) {
  const base = groupsApi(initial);
  const deleted: DeletedGroup[] = [];
  const calls: string[] = [];
  const api: ApiClient = {
    ...base.api,
    request: (async (method: string, path: string, body?: { name?: string }) => {
      const trash = /^\/groups\/([^/]+)\/trash$/.exec(path);
      const restore = /^\/groups\/([^/]+)\/restore$/.exec(path);
      if (trash && method === 'POST') {
        calls.push(`POST ${path} ${JSON.stringify(body)}`);
        const index = base.groups.findIndex((g) => g.id === trash[1]);
        const group = base.groups[index]!;
        if (body?.name !== group.name) {
          throw Object.assign(failure(400, 'VALIDATION_FAILED'), {
            body: {
              status: 400,
              code: 'VALIDATION_FAILED',
              message: 'x',
              errors: [{ field: 'name', code: 'NAME_MISMATCH', message: 'x' }],
            },
          });
        }
        base.groups.splice(index, 1);
        deleted.push({ id: group.id, name: group.name, trashedAt: '2026-11-01T10:00:00Z', daysLeft: 30 });
        return undefined;
      }
      if (restore && method === 'POST') {
        calls.push(`POST ${path}`);
        if (options.restoreFails) throw failure(409, 'GROUP_NAME_TAKEN');
        const index = deleted.findIndex((g) => g.id === restore[1]);
        base.groups.push({ id: deleted[index]!.id, name: deleted[index]!.name, role: 'OWNER' });
        deleted.splice(index, 1);
        return undefined;
      }
      if (path === '/deleted-groups') return deleted.map((g) => ({ ...g }));
      return base.api.request(method as 'GET', path as '/groups', body as never);
    }) as ApiClient['request'],
  };
  return { api, calls };
}

describe('delete group dialog (R-015-01, R-015-02, R-015-07, AC-015-01, AC-015-02)', () => {
  async function openDelete() {
    const row = (await screen.findByRole('link', { name: 'Konferenca 2026' })).closest('tr') as HTMLElement;
    await userEvent.click(within(row).getByRole('button', { name: 'Delete group' }));
    return screen.getByRole('dialog', { name: 'Delete group' });
  }

  it('offers Delete group to an Owner only', async () => {
    const { api } = trashApi();
    renderApp('/', { auth: signedIn(), api });
    await screen.findByRole('link', { name: 'Konferenca 2026' });
    expect(screen.getAllByRole('button', { name: 'Delete group' })).toHaveLength(1);
  });

  it('deletes the group when the exact name is typed and removes it from the list', async () => {
    const { api, calls } = trashApi();
    renderApp('/', { auth: signedIn(), api });
    const dialog = await openDelete();
    await userEvent.type(within(dialog).getByLabelText(/^Type the group name to confirm/), 'Konferenca 2026');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Delete group' }));
    await screen.findByRole('link', { name: 'Poroka' });
    expect(screen.queryByRole('link', { name: 'Konferenca 2026' })).toBeNull();
    expect(calls).toEqual(['POST /groups/g-1/trash {"name":"Konferenca 2026"}']);
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('keeps the dialog open and the typed text when the name differs in case', async () => {
    const { api } = trashApi();
    renderApp('/', { auth: signedIn(), api });
    const dialog = await openDelete();
    const input = within(dialog).getByLabelText(/^Type the group name to confirm/);
    await userEvent.type(input, 'konferenca 2026');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Delete group' }));
    expect(await within(dialog).findByRole('alert')).toHaveTextContent('does not match');
    expect(input).toHaveValue('konferenca 2026');
    expect(screen.getByRole('link', { name: 'Konferenca 2026', hidden: true })).toBeInTheDocument();
  });

  it('closes without deleting on Cancel', async () => {
    const { api, calls } = trashApi();
    renderApp('/', { auth: signedIn(), api });
    const dialog = await openDelete();
    await userEvent.click(within(dialog).getByRole('button', { name: 'Cancel' }));
    expect(screen.queryByRole('dialog')).toBeNull();
    expect(calls).toEqual([]);
  });
});

describe('Deleted groups (R-015-04, R-015-05, R-015-10, AC-015-08)', () => {
  const trashed: DeletedGroup = {
    id: 'g-9',
    name: 'Stara skupina',
    trashedAt: '2026-11-01T10:00:00Z',
    daysLeft: 18,
  };

  function withDeleted(options: { restoreFails?: boolean } = {}) {
    const stand = trashApi([konferenca], options);
    const inner = stand.api.request;
    let listed = [trashed];
    const api: ApiClient = {
      ...stand.api,
      request: (async (method: string, path: string, body?: unknown) => {
        if (path === '/deleted-groups') return listed.map((g) => ({ ...g }));
        if (path === '/groups/g-9/restore') {
          stand.calls.push('POST /groups/g-9/restore');
          if (options.restoreFails) throw failure(409, 'GROUP_NAME_TAKEN');
          listed = [];
          return undefined;
        }
        return inner(method as 'GET', path as '/groups', body as never);
      }) as ApiClient['request'],
    };
    return { api, calls: stand.calls };
  }

  it('lists each group in Trash with the days left', async () => {
    const { api } = withDeleted();
    renderApp('/deleted-groups', { auth: signedIn(), api });
    const row = (await screen.findByText('Stara skupina')).closest('tr') as HTMLElement;
    expect(within(row).getByText('18')).toBeInTheDocument();
    expect(within(row).getByRole('button', { name: 'Restore' })).toBeInTheDocument();
  });

  it('shows an empty state when nothing is in Trash', async () => {
    const { api } = trashApi();
    renderApp('/deleted-groups', { auth: signedIn(), api });
    expect(await screen.findByText(/Groups you delete stay here for 30 days/)).toBeInTheDocument();
  });

  it('restores a group and says so', async () => {
    const { api, calls } = withDeleted();
    renderApp('/deleted-groups', { auth: signedIn(), api });
    await userEvent.click(await screen.findByRole('button', { name: 'Restore' }));
    expect(await screen.findByRole('status')).toHaveTextContent('The group was restored.');
    expect(calls).toContain('POST /groups/g-9/restore');
    expect(screen.queryByText('Stara skupina')).toBeNull();
  });

  it('explains a name clash and keeps the group in the list', async () => {
    const { api } = withDeleted({ restoreFails: true });
    renderApp('/deleted-groups', { auth: signedIn(), api });
    await userEvent.click(await screen.findByRole('button', { name: 'Restore' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Rename that group first');
    expect(screen.getByText('Stara skupina')).toBeInTheDocument();
  });

  it('shows an error with Try again when the list cannot be loaded', async () => {
    const { api } = trashApi();
    const broken: ApiClient = {
      ...api,
      request: (async (method: string, path: string) => {
        if (path === '/deleted-groups') throw failure(500, 'INTERNAL_ERROR');
        return api.request(method as 'GET', path as '/groups');
      }) as ApiClient['request'],
    };
    renderApp('/deleted-groups', { auth: signedIn(), api: broken });
    expect(await screen.findByRole('alert')).toHaveTextContent('Something went wrong');
    expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument();
  });
});
