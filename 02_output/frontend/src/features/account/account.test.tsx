import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ApiError, type ApiClient } from '@/lib/api/client';
import { groupsApi, signedIn } from '@/test/groupsApi';
import { renderApp } from '@/test/renderApp';

/** The groups stand-in plus the answer of `DELETE /me`. */
function accountApi(answer?: ApiError) {
  const { api: groups, calls } = groupsApi([]);
  const api: ApiClient = {
    ...groups,
    request: (async (method: string, path: string, body?: unknown) => {
      if (path === '/me' && method === 'DELETE') {
        calls.push('DELETE /me');
        if (answer) throw answer;
        return undefined;
      }
      return groups.request(method, path, body);
    }) as ApiClient['request'],
  };
  return { api, calls };
}

async function openDialog() {
  await userEvent.click(await screen.findByRole('button', { name: 'Delete account' }));
  return screen.findByRole('dialog', { name: 'Delete account' });
}

describe('the Account screen (R-016-01, R-016-02, R-016-07)', () => {
  it('shows name and e-mail and deletes nothing when the dialog is cancelled', async () => {
    const { api, calls } = accountApi();
    const auth = signedIn();
    renderApp('/account', { auth, api });
    expect(await screen.findByText('ana.novak@example.com')).toBeInTheDocument();
    const dialog = await openDialog();
    await userEvent.click(within(dialog).getByRole('button', { name: 'Cancel' }));
    expect(calls).not.toContain('DELETE /me');
    expect(auth.calls).not.toContain('deleteAccount');
  });

  it('deletes the product data first and then the Firebase account', async () => {
    const { api, calls } = accountApi();
    const auth = signedIn();
    renderApp('/account', { auth, api });
    const dialog = await openDialog();
    await userEvent.click(within(dialog).getByRole('button', { name: 'Delete account' }));
    await screen.findByRole('heading', { name: 'Sign in' });
    expect(calls).toContain('DELETE /me');
    expect(auth.calls).toContain('deleteAccount');
  });

  it('names the groups the person alone owns and keeps the account', async () => {
    const refusal = new ApiError(409, {
      status: 409,
      code: 'SOLE_OWNER',
      message: 'x',
      groups: [
        { id: 'g-1', name: 'Konferenca 2026' },
        { id: 'g-2', name: 'Poroka' },
      ],
    });
    const { api } = accountApi(refusal);
    const auth = signedIn();
    renderApp('/account', { auth, api });
    const dialog = await openDialog();
    await userEvent.click(within(dialog).getByRole('button', { name: 'Delete account' }));
    expect(await screen.findByText(/You are the only Owner of these groups/)).toBeInTheDocument();
    expect(screen.getByText('Konferenca 2026')).toBeInTheDocument();
    expect(screen.getByText('Poroka')).toBeInTheDocument();
    expect(auth.calls).not.toContain('deleteAccount');
  });

  it('asks to sign in again when the sign-in is too old', async () => {
    const stale = new ApiError(403, { status: 403, code: 'RECENT_LOGIN_REQUIRED', message: 'x' });
    const { api } = accountApi(stale);
    const auth = signedIn();
    renderApp('/account', { auth, api });
    const dialog = await openDialog();
    await userEvent.click(within(dialog).getByRole('button', { name: 'Delete account' }));
    const again = await screen.findByRole('dialog', { name: 'Sign in again to do this' });
    await userEvent.click(within(again).getByRole('button', { name: 'Sign in' }));
    expect(auth.calls).toContain('signOut');
    expect(auth.calls).not.toContain('deleteAccount');
  });
});
