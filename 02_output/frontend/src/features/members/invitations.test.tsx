import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ApiClient } from '@/lib/api/client';
import type { components } from '@/lib/api/schema';
import { failure, groupsApi, konferenca, poroka, signedIn } from '@/test/groupsApi';
import { renderApp } from '@/test/renderApp';

type Invitation = components['schemas']['Invitation'];

const base = '/groups/g-1/invitations';

function invitation(n: number, role: Invitation['role'] = 'VIEWER'): Invitation {
  return {
    id: `i-${n}`,
    email: `p${n}@example.com`,
    role,
    createdAt: '2026-11-01T10:00:00Z',
    expiresAt: '2026-11-15T10:00:00Z',
  };
}

/** The groups stand-in with the invitations of group g-1 and the refusals of the server. */
function membersApi(pending: Invitation[], refuse?: { status: number; code: 'ALREADY_MEMBER' }) {
  const { api: groups, calls } = groupsApi([konferenca, poroka]);
  const list = [...pending];
  const api: ApiClient = {
    ...groups,
    request: (async (method: string, path: string, body?: { email: string; role: Invitation['role'] }) => {
      if (path !== base) return groups.request(method, path, body);
      calls.push(`${method} ${path} ${body ? JSON.stringify(body) : ''}`.trim());
      if (method === 'GET') return list.map((i) => ({ ...i }));
      if (refuse) throw failure(refuse.status, refuse.code);
      if (body?.email.includes('@@')) {
        const { ApiError } = await import('@/lib/api/client');
        throw new ApiError(400, {
          status: 400,
          code: 'VALIDATION_FAILED',
          message: 'x',
          errors: [{ field: 'email', code: 'INVALID_FORMAT', message: 'x' }],
        });
      }
      list.push({ ...invitation(list.length + 1, body?.role), email: body?.email ?? '' });
      return list[list.length - 1];
    }) as ApiClient['request'],
  };
  const del = api.request;
  api.request = (async (method: string, path: string, body?: unknown) => {
    const one = /^\/groups\/g-1\/invitations\/(.+)$/.exec(path);
    if (method === 'DELETE' && one) {
      calls.push(`DELETE ${path}`);
      list.splice(
        list.findIndex((i) => i.id === one[1]),
        1,
      );
      return undefined;
    }
    return del(method, path, body);
  }) as ApiClient['request'];
  return { api, calls };
}

describe('invitations on the Members tab (R-003-01, R-003-04, R-003-07, R-003-08, R-003-11)', () => {
  it('lists the pending invitations with role and expiry and cancels one', async () => {
    const { api, calls } = membersApi([invitation(1, 'EDITOR'), invitation(2)]);
    renderApp('/groups/g-1/members', { auth: signedIn(), api });
    const row = await screen.findByRole('row', { name: /p1@example.com/ });
    expect(within(row).getByText('Editor')).toBeInTheDocument();
    expect(within(row).getByText(/^Expires 2026-11-15 11:00$/)).toBeInTheDocument();
    await userEvent.click(within(row).getByRole('button', { name: 'Cancel invitation' }));
    await screen.findByText('p2@example.com');
    expect(screen.queryByText('p1@example.com')).toBeNull();
    expect(calls).toContain(`DELETE ${base}/i-1`);
  });

  it('shows the empty state when nothing is pending', async () => {
    const { api } = membersApi([]);
    renderApp('/groups/g-1/members', { auth: signedIn(), api });
    expect(await screen.findByText('No pending invitations.')).toBeInTheDocument();
  });

  it('invites with the chosen role, lists the invitation and clears the address', async () => {
    const { api, calls } = membersApi([]);
    renderApp('/groups/g-1/members', { auth: signedIn(), api });
    await userEvent.type(await screen.findByLabelText(/E-mail/), 'marko@example.com');
    await userEvent.selectOptions(screen.getByLabelText(/Role/), 'Viewer');
    await userEvent.click(screen.getByRole('button', { name: 'Invite' }));
    expect(await screen.findByRole('row', { name: /marko@example.com/ })).toBeInTheDocument();
    expect(calls).toContain(`POST ${base} {"email":"marko@example.com","role":"VIEWER"}`);
    expect(screen.getByLabelText(/E-mail/)).toHaveValue('');
  });

  it('shows the reason of a refusal and keeps what was typed', async () => {
    const { api } = membersApi([], { status: 409, code: 'ALREADY_MEMBER' });
    renderApp('/groups/g-1/members', { auth: signedIn(), api });
    await userEvent.type(await screen.findByLabelText(/E-mail/), 'Marko@Example.com');
    await userEvent.click(screen.getByRole('button', { name: 'Invite' }));
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'This person is already a member of the group.',
    );
    expect(screen.getByLabelText(/E-mail/)).toHaveValue('Marko@Example.com');
  });

  it('shows an invalid address as invalid', async () => {
    const { api } = membersApi([]);
    renderApp('/groups/g-1/members', { auth: signedIn(), api });
    await userEvent.type(await screen.findByLabelText(/E-mail/), 'marko@@example');
    await userEvent.click(screen.getByRole('button', { name: 'Invite' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Enter a valid e-mail address.');
  });

  it('tells that the limit of 20 is reached', async () => {
    const { api } = membersApi(Array.from({ length: 20 }, (_, i) => invitation(i + 1)));
    renderApp('/groups/g-1/members', { auth: signedIn(), api });
    expect(await screen.findByText(/The limit of 20 pending invitations is reached\./)).toBeInTheDocument();
  });

  it('shows an Editor neither the form nor the invitations and does not ask for them', async () => {
    const { api, calls } = membersApi([invitation(1)]);
    renderApp('/groups/g-2/members', { auth: signedIn(), api });
    await screen.findByRole('heading', { level: 1, name: 'Poroka' });
    expect(screen.queryByRole('button', { name: 'Invite' })).toBeNull();
    expect(screen.queryByText('Pending invitations')).toBeNull();
    expect(calls.some((c) => c.includes('/invitations'))).toBe(false);
  });
});
