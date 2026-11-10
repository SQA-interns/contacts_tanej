import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ApiError, type ApiClient } from '@/lib/api/client';
import { renderApp } from '@/test/renderApp';
import { failure, groupsApi, konferenca, poroka, signedIn, skupno } from '@/test/groupsApi';

describe('groups list (R-002-04, AC-002-02, UI-14)', () => {
  it('shows each group as a link with the role of the person', async () => {
    const { api } = groupsApi([konferenca, poroka, skupno]);
    renderApp('/', { auth: signedIn(), api });
    const row = (await screen.findByRole('link', { name: 'Konferenca 2026' })).closest('tr');
    expect(within(row as HTMLElement).getByText('Owner')).toBeInTheDocument();
    expect(
      within(screen.getByRole('link', { name: 'Poroka' }).closest('tr') as HTMLElement).getByText('Editor'),
    ).toBeInTheDocument();
    expect(
      within(screen.getByRole('link', { name: 'Skupno' }).closest('tr') as HTMLElement).getByText('Viewer'),
    ).toBeInTheDocument();
  });

  it('shows the roles in Slovenian when that language is active', async () => {
    const { api } = groupsApi([konferenca, skupno]);
    renderApp('/', { auth: signedIn(), api, language: 'sl' });
    await screen.findByRole('link', { name: 'Konferenca 2026' });
    expect(screen.getByText('Lastnik')).toBeInTheDocument();
    expect(screen.getByText('Bralec')).toBeInTheDocument();
  });

  it('shows the loading state, then an error with "Try again" that loads again', async () => {
    let fail = true;
    const { api } = groupsApi([konferenca]);
    const flaky: ApiClient = {
      ...api,
      request: (async (method: string, path: string, body?: unknown) => {
        if (fail) throw failure(500, 'INTERNAL_ERROR');
        return api.request(method, path, body);
      }) as ApiClient['request'],
    };
    renderApp('/', { auth: signedIn(), api: flaky });
    expect(await screen.findByRole('alert')).toHaveTextContent('Something went wrong');
    fail = false;
    await userEvent.click(screen.getByRole('button', { name: 'Try again' }));
    expect(await screen.findByRole('link', { name: 'Konferenca 2026' })).toBeInTheDocument();
  });

  it('offers a Create group button above the list', async () => {
    const { api } = groupsApi([konferenca]);
    renderApp('/', { auth: signedIn(), api });
    await screen.findByRole('link', { name: 'Konferenca 2026' });
    expect(screen.getByRole('button', { name: 'Create group' })).toBeInTheDocument();
  });
});

describe('create group dialog (R-002-01, R-002-02, AC-002-01, AC-002-04, AC-002-05)', () => {
  async function openDialog() {
    await userEvent.click(await screen.findByRole('button', { name: 'Create group' }));
    return screen.getByRole('dialog');
  }

  it('creates the group from the empty state and opens it with the name as heading', async () => {
    const { api, calls } = groupsApi();
    renderApp('/', { auth: signedIn(), api });
    const dialog = await openDialog();
    await userEvent.type(within(dialog).getByLabelText(/^Group name/), '  Konferenca 2026 ');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Create group' }));
    expect(await screen.findByRole('heading', { name: 'Konferenca 2026' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'No contacts yet' })).toBeInTheDocument();
    expect(screen.queryByRole('dialog')).toBeNull();
    expect(calls).toContain('POST /groups {"name":"Konferenca 2026"}');
  });

  it('refuses a name of spaces only with the reason and sends nothing', async () => {
    const { api, calls } = groupsApi();
    renderApp('/', { auth: signedIn(), api });
    const dialog = await openDialog();
    await userEvent.type(within(dialog).getByLabelText(/^Group name/), '   ');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Create group' }));
    expect(await within(dialog).findByRole('alert')).toHaveTextContent('Enter a group name.');
    expect(calls.some((c) => c.startsWith('POST'))).toBe(false);
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });

  it('accepts exactly 100 characters and refuses 101 with the reason', async () => {
    const { api, calls } = groupsApi();
    renderApp('/', { auth: signedIn(), api });
    const dialog = await openDialog();
    const input = within(dialog).getByLabelText(/^Group name/);
    await userEvent.click(input);
    await userEvent.paste('a'.repeat(101));
    await userEvent.click(within(dialog).getByRole('button', { name: 'Create group' }));
    expect(await within(dialog).findByRole('alert')).toHaveTextContent('at most 100 characters');
    expect(calls.some((c) => c.startsWith('POST'))).toBe(false);
    await userEvent.clear(input);
    await userEvent.paste('a'.repeat(100));
    await userEvent.click(within(dialog).getByRole('button', { name: 'Create group' }));
    await waitFor(() => expect(calls.some((c) => c.startsWith('POST'))).toBe(true));
  });

  it('keeps the dialog and the typed name when the name is taken, and shows the reason', async () => {
    const { api } = groupsApi([konferenca]);
    renderApp('/', { auth: signedIn(), api });
    const dialog = await openDialog();
    await userEvent.type(within(dialog).getByLabelText(/^Group name/), 'KONFERENCA 2026');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Create group' }));
    expect(await within(dialog).findByText('You already have a group with this name.')).toBeInTheDocument();
    expect(within(dialog).getByLabelText(/^Group name/)).toHaveValue('KONFERENCA 2026');
    expect(within(dialog).getByLabelText(/^Group name/)).toHaveAttribute('aria-invalid', 'true');
  });

  it('shows the text of the server refusal of the name field', async () => {
    const { api } = groupsApi();
    const refusing: ApiClient = {
      ...api,
      request: (async (method: string, path: string) => {
        if (method === 'POST') {
          throw new ApiError(400, {
            status: 400,
            code: 'VALIDATION_FAILED',
            message: 'x',
            errors: [{ field: 'name', code: 'TOO_LONG', message: 'x' }],
          });
        }
        return path === '/groups' ? [konferenca] : undefined;
      }) as ApiClient['request'],
    };
    renderApp('/', { auth: signedIn(), api: refusing });
    const dialog = await openDialog();
    await userEvent.type(within(dialog).getByLabelText(/^Group name/), 'Poroka');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Create group' }));
    expect(await within(dialog).findByRole('alert')).toHaveTextContent('at most 100 characters');
  });

  it('shows the text of any other refusal in the dialog', async () => {
    const { api } = groupsApi();
    const down: ApiClient = {
      ...api,
      request: (async (method: string, path: string) => {
        if (method === 'POST') throw failure(429, 'RATE_LIMITED');
        return path === '/groups' ? [konferenca] : undefined;
      }) as ApiClient['request'],
    };
    renderApp('/', { auth: signedIn(), api: down });
    const dialog = await openDialog();
    await userEvent.type(within(dialog).getByLabelText(/^Group name/), 'Poroka');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Create group' }));
    expect(await within(dialog).findByRole('alert')).toHaveTextContent('Too many requests');
  });

  it('blocks a second click while the request runs (UI-12)', async () => {
    let release: () => void = () => undefined;
    const hold = new Promise<void>((resolve) => (release = resolve));
    const { api, calls } = groupsApi([], { hold });
    renderApp('/', { auth: signedIn(), api });
    const dialog = await openDialog();
    await userEvent.type(within(dialog).getByLabelText(/^Group name/), 'Poroka');
    const submit = within(dialog).getByRole('button', { name: 'Create group' });
    await userEvent.click(submit);
    await waitFor(() => expect(submit).toBeDisabled());
    await userEvent.click(submit);
    release();
    await screen.findByRole('heading', { name: 'Poroka' });
    expect(calls.filter((c) => c.startsWith('POST'))).toHaveLength(1);
  });

  it('closes with Cancel and sends nothing; the next opening starts empty', async () => {
    const { api, calls } = groupsApi([konferenca]);
    renderApp('/', { auth: signedIn(), api });
    let dialog = await openDialog();
    await userEvent.type(within(dialog).getByLabelText(/^Group name/), 'Poroka');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Cancel' }));
    expect(screen.queryByRole('dialog')).toBeNull();
    dialog = await openDialog();
    expect(within(dialog).getByLabelText(/^Group name/)).toHaveValue('');
    expect(calls.some((c) => c.startsWith('POST'))).toBe(false);
  });
});
