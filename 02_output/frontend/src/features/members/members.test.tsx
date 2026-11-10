import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ApiClient } from '@/lib/api/client';
import type { components } from '@/lib/api/schema';
import { failure, groupsApi, konferenca, poroka, signedIn, skupno } from '@/test/groupsApi';
import { renderApp } from '@/test/renderApp';

type Member = components['schemas']['Member'];

const ana: Member = { userId: 'u-ana', name: 'Ana Novak', email: 'ana.novak@example.com', role: 'OWNER' };
const marko: Member = { userId: 'u-marko', name: 'Marko Pirc', email: 'marko@example.com', role: 'EDITOR' };
const petra: Member = { userId: 'u-petra', name: 'Petra Kos', email: 'petra@example.com', role: 'VIEWER' };

/** The groups stand-in with the members of group g-1 and the refusals the server may give. */
function membersApi(members: Member[], refuse?: { status: number; code: 'LAST_OWNER' | 'NOT_FOUND' }) {
  const { api: groups, calls } = groupsApi([konferenca, poroka, skupno]);
  const list = members.map((m) => ({ ...m }));
  const api: ApiClient = {
    ...groups,
    request: (async (method: string, path: string, body?: { role: Member['role'] }) => {
      const base = '/groups/g-1/members';
      const one = /^\/groups\/g-1\/members\/(.+)$/.exec(path);
      if (path === base || one || path === '/groups/g-1/leave' || path === '/groups/g-1/invitations') {
        calls.push(`${method} ${path} ${body ? JSON.stringify(body) : ''}`.trim());
      }
      if (path === '/groups/g-1/invitations') return [];
      if (path === base) return list.map((m) => ({ ...m }));
      if (one && method !== 'GET' && refuse) throw failure(refuse.status, refuse.code);
      if (path === '/groups/g-1/leave' && refuse) throw failure(refuse.status, refuse.code);
      if (one && method === 'PUT') {
        const member = list.find((m) => m.userId === one[1]);
        if (member && body) member.role = body.role;
        return member;
      }
      if (one && method === 'DELETE') {
        list.splice(
          list.findIndex((m) => m.userId === one[1]),
          1,
        );
        return undefined;
      }
      if (path === '/groups/g-1/leave') return undefined;
      return groups.request(method, path, body);
    }) as ApiClient['request'],
  };
  return { api, calls };
}

describe('the member list of the Members tab (R-004-01, R-004-02, R-004-04, R-004-06, R-004-07)', () => {
  it('lists every member with name, e-mail and role and gives an Owner the controls', async () => {
    const { api } = membersApi([ana, marko, petra]);
    renderApp('/groups/g-1/members', { auth: signedIn(), api });
    const row = await screen.findByRole('row', { name: /Marko Pirc/ });
    expect(within(row).getByText('marko@example.com')).toBeInTheDocument();
    expect(within(row).getByRole('combobox', { name: 'Role of Marko Pirc' })).toHaveValue('EDITOR');
    expect(within(row).getByRole('button', { name: 'Remove' })).toBeInTheDocument();
    const own = screen.getByRole('row', { name: /Ana Novak/ });
    expect(within(own).getByRole('button', { name: 'Leave group' })).toBeInTheDocument();
    expect(within(own).queryByRole('button', { name: 'Remove' })).not.toBeInTheDocument();
  });

  it('shows an Editor or Viewer the list without role selects and without Remove', async () => {
    const { api, calls } = membersApi([ana, marko, petra]);
    renderApp('/groups/g-2/members', { auth: signedIn(), api });
    await screen.findByRole('heading', { name: 'Poroka' });
    expect(calls.some((c) => c.includes('/groups/g-2/invitations'))).toBe(false);
    expect(screen.queryByRole('button', { name: 'Remove' })).not.toBeInTheDocument();
    expect(screen.queryByRole('combobox', { name: /Role of/ })).not.toBeInTheDocument();
  });

  it('changes a role with the select and sends it to the server', async () => {
    const { api, calls } = membersApi([ana, marko]);
    renderApp('/groups/g-1/members', { auth: signedIn(), api });
    const select = await screen.findByRole('combobox', { name: 'Role of Marko Pirc' });
    await userEvent.selectOptions(select, 'VIEWER');
    expect(calls).toContain('PUT /groups/g-1/members/u-marko {"role":"VIEWER"}');
    expect(await screen.findByRole('combobox', { name: 'Role of Marko Pirc' })).toHaveValue('VIEWER');
  });

  it('removes a member only after the confirmation', async () => {
    const { api, calls } = membersApi([ana, marko]);
    renderApp('/groups/g-1/members', { auth: signedIn(), api });
    const row = await screen.findByRole('row', { name: /Marko Pirc/ });
    await userEvent.click(within(row).getByRole('button', { name: 'Remove' }));
    const dialog = await screen.findByRole('dialog', { name: 'Remove member' });
    expect(calls.some((c) => c.startsWith('DELETE'))).toBe(false);
    await userEvent.click(within(dialog).getByRole('button', { name: 'Remove' }));
    await screen.findByText('Ana Novak');
    expect(screen.queryByText('Marko Pirc')).not.toBeInTheDocument();
    expect(calls).toContain('DELETE /groups/g-1/members/u-marko');
  });

  it('cancelling the confirmation removes nobody', async () => {
    const { api, calls } = membersApi([ana, marko]);
    renderApp('/groups/g-1/members', { auth: signedIn(), api });
    const row = await screen.findByRole('row', { name: /Marko Pirc/ });
    await userEvent.click(within(row).getByRole('button', { name: 'Remove' }));
    await userEvent.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Cancel' }));
    expect(calls.some((c) => c.startsWith('DELETE'))).toBe(false);
  });

  it('leaving the group goes to the group list', async () => {
    const { api, calls } = membersApi([ana, marko]);
    renderApp('/groups/g-1/members', { auth: signedIn(), api });
    await userEvent.click(await screen.findByRole('button', { name: 'Leave group' }));
    await userEvent.click(
      within(await screen.findByRole('dialog')).getByRole('button', { name: 'Leave group' }),
    );
    await screen.findByRole('heading', { name: 'Groups' });
    expect(calls).toContain('POST /groups/g-1/leave');
  });

  it('shows the last-Owner message and keeps the Owner when leaving is refused', async () => {
    const { api } = membersApi([ana, marko], { status: 409, code: 'LAST_OWNER' });
    renderApp('/groups/g-1/members', { auth: signedIn(), api });
    await userEvent.click(await screen.findByRole('button', { name: 'Leave group' }));
    await userEvent.click(
      within(await screen.findByRole('dialog')).getByRole('button', { name: 'Leave group' }),
    );
    expect(
      await screen.findByText('A group needs at least one Owner. Make another member an Owner first.'),
    ).toBeInTheDocument();
    expect(within(screen.getByRole('row', { name: /Ana Novak/ })).getByText('Owner')).toBeInTheDocument();
  });

  it('shows the group list when the server says the group is gone (EX-004-03)', async () => {
    const { api } = membersApi([ana, marko], { status: 404, code: 'NOT_FOUND' });
    renderApp('/groups/g-1/members', { auth: signedIn(), api });
    const row = await screen.findByRole('row', { name: /Marko Pirc/ });
    await userEvent.click(within(row).getByRole('button', { name: 'Remove' }));
    await userEvent.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Remove' }));
    await screen.findByRole('heading', { name: 'Groups' });
  });
});
