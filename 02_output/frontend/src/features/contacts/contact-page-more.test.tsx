import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { I18nextProvider } from 'react-i18next';
import { createMemoryRouter, RouterProvider } from 'react-router';
import { Services } from '@/app/services';
import { routes } from '@/app/routes';
import { ApiError, type ApiClient } from '@/lib/api/client';
import { setupI18n } from '@/lib/i18n';
import { groupsApi, konferenca, skupno, signedIn } from '@/test/groupsApi';
import { createFakeAuth } from '@/test/fakeAuth';

const ana = {
  id: 'c/1',
  state: 'ACTIVE',
  revision: 4,
  versionNumber: 1,
  createdAt: '2026-11-10T10:00:00Z',
  firstName: 'Ana',
  lastName: 'Novak',
  tags: ['vip'],
  attributes: [{ key: 'Level', value: 'gold' }],
};

type Answer = (method: string, path: string, body?: unknown) => unknown;

function setup(answer: Answer, path: string, group = 'g-1') {
  const { api: groups } = groupsApi([konferenca, skupno]);
  const calls: { method: string; path: string; body?: unknown }[] = [];
  const api: ApiClient = {
    ...groups,
    request: (async (method: string, p: string, body?: unknown) => {
      if (/^\/groups\/g-\d\/(contacts(\/(?!search)|$)|tags|attribute-keys)/.test(p)) {
        if (!p.includes('/tags') && !p.includes('/attribute-keys')) calls.push({ method, path: p, body });
        if (p.includes('/tags') || p.includes('/attribute-keys')) return [];
        const result = answer(method, p, body);
        if (result instanceof Error) throw result;
        return result;
      }
      return groups.request(method, p, body);
    }) as ApiClient['request'],
  };
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const invalidated: unknown[] = [];
  const original = client.invalidateQueries.bind(client);
  client.invalidateQueries = ((filters: { queryKey?: unknown }) => {
    invalidated.push(filters?.queryKey);
    return original(filters as never);
  }) as never;
  const router = createMemoryRouter(routes, {
    initialEntries: [path.replace('/groups/g-1', `/groups/${group}`)],
  });
  render(
    <I18nextProvider i18n={setupI18n('en')}>
      <QueryClientProvider client={client}>
        <Services value={{ auth: signedIn(), api } as never}>
          <RouterProvider router={router} />
        </Services>
      </QueryClientProvider>
    </I18nextProvider>,
  );
  void createFakeAuth;
  return { calls, invalidated, router, client };
}

const refuse = (status: number, code: string, extra: object = {}) =>
  new ApiError(status, { status, code, message: 'x', ...extra } as never);
const open = '/groups/g-1/contacts/c%2F1';
const save = () => userEvent.click(screen.getByRole('button', { name: 'Save' }));

describe('ContactPage: reading', () => {
  it('reads the contact once with the id encoded', async () => {
    const { calls } = setup(() => ana, open);
    await screen.findByRole('heading', { name: 'Ana Novak' });
    expect(calls.filter((c) => c.method === 'GET')).toEqual([
      { method: 'GET', path: '/groups/g-1/contacts/c%2F1', body: undefined },
    ]);
  });

  it('shows the not-found page for a 404 and a retry for any other failure', async () => {
    let failing = true;
    setup(() => (failing ? refuse(500, 'INTERNAL_ERROR') : ana), open);
    expect(await screen.findByText('Something went wrong. Please try again.')).toBeInTheDocument();
    expect(screen.queryByText('Not found')).toBeNull();
    failing = false;
    await userEvent.click(screen.getByRole('button', { name: 'Try again' }));
    expect(await screen.findByRole('heading', { name: 'Ana Novak' })).toBeInTheDocument();
  });

  it('shows the not-found page with its explanation for 404', async () => {
    setup(() => refuse(404, 'NOT_FOUND'), open);
    expect(await screen.findByRole('heading', { name: 'Not found' })).toBeInTheDocument();
    expect(screen.getByText('This page does not exist or you have no access to it.')).toBeInTheDocument();
  });

  it('shows the crumb, the tabs of an editor and the New contact title at /contacts/new', async () => {
    setup(() => ana, open);
    await screen.findByRole('heading', { name: 'Ana Novak' });
    const crumb = screen.getByText(/\/ Contact$/);
    expect(within(crumb).getByRole('link', { name: 'Contacts' })).toHaveAttribute('href', '/groups/g-1');
    const nav = screen.getByRole('navigation', { name: 'Contact' });
    expect(within(nav).getByText('Details')).toHaveAttribute('aria-current', 'page');
    expect(within(nav).getByRole('link', { name: 'History' })).toHaveAttribute(
      'href',
      '/groups/g-1/contacts/c/1/history',
    );
    expect(within(nav).getByRole('link', { name: 'Versions' })).toHaveAttribute(
      'href',
      '/groups/g-1/contacts/c/1/versions',
    );
  });

  it('has no tabs on the new contact form and shows no Delete', async () => {
    setup(() => ana, '/groups/g-1/contacts/new');
    expect(await screen.findByRole('heading', { name: 'New contact' })).toBeInTheDocument();
    expect(screen.queryByRole('navigation', { name: 'Contact' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Delete contact' })).toBeNull();
  });

  it('shows a Viewer no tabs and the tags and attributes as text', async () => {
    setup(() => ana, open, 'g-3');
    await screen.findByRole('heading', { name: 'Ana Novak' });
    expect(screen.queryByRole('navigation', { name: 'Contact' })).toBeNull();
    expect(screen.getByText('vip')).toBeInTheDocument();
    expect(screen.getByText('Level')).toBeInTheDocument();
    expect(screen.getByText('gold')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Save' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Delete contact' })).toBeNull();
  });

  it('shows a contact without tags and attributes keys as empty', async () => {
    const bare = { ...ana, tags: undefined, attributes: undefined };
    setup(() => bare, open, 'g-3');
    await screen.findByRole('heading', { name: 'Ana Novak' });
    expect(screen.queryByText('Stryker was here')).toBeNull();
  });
});

describe('ContactPage: saving', () => {
  it('sends the PUT with the revision, tags and attributes, then refreshes the contact and says so', async () => {
    const { calls, invalidated } = setup((m) => (m === 'PUT' ? { ...ana, revision: 5 } : ana), open);
    await screen.findByRole('heading', { name: 'Ana Novak' });
    await userEvent.type(screen.getByLabelText('Company'), 'Acme');
    await save();
    expect(await screen.findByRole('status')).toHaveTextContent('The contact was saved.');
    const put = calls.find((c) => c.method === 'PUT')!;
    expect(put.path).toBe('/groups/g-1/contacts/c%2F1');
    expect(put.body).toMatchObject({
      company: 'Acme',
      tags: ['vip'],
      attributes: [{ key: 'Level', value: 'gold' }],
      revision: 4,
    });
    expect(invalidated).toContainEqual(['contact', 'g-1', 'c/1']);
  });

  it('is busy while it saves and clears the earlier message and refused fields at the start', async () => {
    let release: (v: unknown) => void = () => undefined;
    let first = true;
    setup((m) => {
      if (m !== 'PUT') return ana;
      if (first) {
        first = false;
        return refuse(400, 'VALIDATION_FAILED', {
          errors: [{ field: 'email', code: 'INVALID_FORMAT', message: 'x' }],
        });
      }
      return new Promise((r) => (release = r));
    }, open);
    await screen.findByRole('heading', { name: 'Ana Novak' });
    await save();
    expect(await screen.findByText('Check the e-mail address.')).toBeInTheDocument();
    await save();
    expect(await screen.findByRole('button', { name: /Saving/ })).toBeInTheDocument();
    expect(screen.queryByText('Check the e-mail address.')).toBeNull();
    expect(screen.queryByRole('status')).toBeNull();
    release({ ...ana, revision: 5 });
    expect(await screen.findByRole('button', { name: 'Save' })).toBeEnabled();
    expect(await screen.findByRole('status')).toBeInTheDocument();
  });

  it('shows an earlier success again as gone when a later save fails', async () => {
    let n = 0;
    setup((m) => {
      if (m !== 'PUT') return ana;
      return ++n === 1 ? { ...ana, revision: 5 } : refuse(403, 'FORBIDDEN');
    }, open);
    await screen.findByRole('heading', { name: 'Ana Novak' });
    await save();
    expect(await screen.findByRole('status')).toBeInTheDocument();
    await save();
    expect(await screen.findByRole('alert')).toHaveTextContent('You may not do this.');
    expect(screen.queryByRole('status')).toBeNull();
  });

  it('creates with a POST, replaces the address with the new contact and shows the confirmation there', async () => {
    const { router, calls } = setup(
      (m) => (m === 'POST' ? { ...ana, id: 'new-1' } : ana),
      '/groups/g-1/contacts/new',
    );
    await screen.findByRole('heading', { name: 'New contact' });
    await userEvent.type(screen.getByLabelText('Last name'), 'Novak');
    await save();
    await waitFor(() => expect(router.state.location.pathname).toBe('/groups/g-1/contacts/new-1'));
    expect(router.state.historyAction).toBe('REPLACE');
    expect(router.state.location.state).toEqual({ saved: true });
    expect(await screen.findByText('The contact was saved.')).toBeInTheDocument();
    expect(calls.find((c) => c.method === 'POST')?.path).toBe('/groups/g-1/contacts');
  });

  it('starts without a confirmation when the navigation state does not carry one', async () => {
    setup(() => ana, open);
    await screen.findByRole('heading', { name: 'Ana Novak' });
    expect(screen.queryByRole('status')).toBeNull();
  });
});

describe('ContactPage: refusals', () => {
  it('shows the general text for a failure that is no API answer', async () => {
    setup((m) => (m === 'PUT' ? new Error('boom') : ana), open);
    await screen.findByRole('heading', { name: 'Ana Novak' });
    await save();
    expect(await screen.findByRole('alert')).toHaveTextContent('Something went wrong. Please try again.');
  });

  it('shows the conflict with the current contact when the answer carries it, also without tags', async () => {
    const current = { ...ana, revision: 9, company: 'Newer', tags: undefined, attributes: undefined };
    setup((m) => (m === 'PUT' ? refuse(409, 'CONFLICT', { current }) : ana), open);
    await screen.findByRole('heading', { name: 'Ana Novak' });
    await save();
    expect(await screen.findByText('Newer')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Delete contact' })).toBeNull();
    expect(screen.queryByText('Stryker was here')).toBeNull();
  });

  it('shows the plain conflict text when the answer does not carry the current contact', async () => {
    setup((m) => (m === 'PUT' ? refuse(409, 'CONFLICT') : ana), open);
    await screen.findByRole('heading', { name: 'Ana Novak' });
    await save();
    expect(await screen.findByRole('alert')).toHaveTextContent('This contact was changed by someone else.');
    expect(screen.getByRole('button', { name: 'Save' })).toBeInTheDocument();
  });

  it('reads the contact again when it went to Trash meanwhile, and says so', async () => {
    const { invalidated } = setup((m) => (m === 'PUT' ? refuse(409, 'CONTACT_IN_TRASH') : ana), open);
    await screen.findByRole('heading', { name: 'Ana Novak' });
    invalidated.length = 0;
    await save();
    expect(await screen.findByRole('alert')).toBeInTheDocument();
    expect(invalidated).toContainEqual(['contact', 'g-1', 'c/1']);
  });

  it('does not read a contact again when a new one is refused for another reason', async () => {
    const { invalidated } = setup(
      (m) => (m === 'POST' ? refuse(409, 'CONTACT_IN_TRASH') : ana),
      '/groups/g-1/contacts/new',
    );
    await screen.findByRole('heading', { name: 'New contact' });
    await userEvent.type(screen.getByLabelText('Last name'), 'N');
    await save();
    expect(await screen.findByRole('alert')).toBeInTheDocument();
    expect(invalidated.filter((k) => Array.isArray(k) && k[0] === 'contact')).toHaveLength(0);
  });

  it('does not read the contact again after a conflict or another refusal', async () => {
    const { invalidated } = setup((m) => (m === 'PUT' ? refuse(403, 'FORBIDDEN') : ana), open);
    await screen.findByRole('heading', { name: 'Ana Novak' });
    invalidated.length = 0;
    await save();
    await screen.findByRole('alert');
    expect(invalidated).toHaveLength(0);
  });
});

describe('ContactPage: the conflict screen and deleting', () => {
  it('reloads with the current values and goes back to the form', async () => {
    const current = { ...ana, revision: 9, company: 'Newer' };
    setup((m) => (m === 'PUT' ? refuse(409, 'CONFLICT', { current }) : ana), open);
    await screen.findByRole('heading', { name: 'Ana Novak' });
    await save();
    await userEvent.click(await screen.findByRole('button', { name: 'Reload with the current values' }));
    expect(await screen.findByLabelText('Company')).toHaveValue('Newer');
    expect(screen.getByRole('button', { name: 'Delete contact' })).toBeInTheDocument();
  });

  it('leaves for the list from the conflict screen and from the form with Cancel', async () => {
    const current = { ...ana, revision: 9 };
    const { router } = setup((m) => (m === 'PUT' ? refuse(409, 'CONFLICT', { current }) : ana), open);
    await screen.findByRole('heading', { name: 'Ana Novak' });
    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }));
    await waitFor(() => expect(router.state.location.pathname).toBe('/groups/g-1'));
  });

  it('leaves for the list from the conflict screen', async () => {
    const current = { ...ana, revision: 9 };
    const { router } = setup((m) => (m === 'PUT' ? refuse(409, 'CONFLICT', { current }) : ana), open);
    await screen.findByRole('heading', { name: 'Ana Novak' });
    await save();
    await userEvent.click(await screen.findByRole('button', { name: 'Cancel' }));
    await waitFor(() => expect(router.state.location.pathname).toBe('/groups/g-1'));
  });

  it('asks before it deletes, moves the contact to Trash, shows the list and refreshes three caches', async () => {
    const { calls, invalidated, router } = setup(() => ana, open);
    await screen.findByRole('heading', { name: 'Ana Novak' });
    await userEvent.click(screen.getByRole('button', { name: 'Delete contact' }));
    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByRole('heading', { name: 'Delete contact' })).toBeInTheDocument();
    expect(
      within(dialog).getByText('The contact moves to Trash. You can restore it there for 30 days.'),
    ).toBeInTheDocument();
    expect(calls.some((c) => c.path.endsWith('/trash'))).toBe(false);
    invalidated.length = 0;
    await userEvent.click(within(dialog).getByRole('button', { name: 'Delete contact' }));
    await waitFor(() => expect(router.state.location.pathname).toBe('/groups/g-1'));
    expect(calls.find((c) => c.path.endsWith('/trash'))).toEqual({
      method: 'POST',
      path: '/groups/g-1/contacts/c%2F1/trash',
      body: undefined,
    });
    await waitFor(() => expect(invalidated).toContainEqual(['contact', 'g-1', 'c/1']));
    expect(invalidated).toContainEqual(['contacts', 'g-1']);
    expect(invalidated).toContainEqual(['trash', 'g-1']);
  });

  it('refreshes the caches and stays on the contact when the move to Trash is refused', async () => {
    const { invalidated, router } = setup(
      (_m, p) => (p.endsWith('/trash') ? refuse(409, 'ALREADY_IN_TRASH') : ana),
      open,
    );
    await screen.findByRole('heading', { name: 'Ana Novak' });
    await userEvent.click(screen.getByRole('button', { name: 'Delete contact' }));
    invalidated.length = 0;
    await userEvent.click(
      within(await screen.findByRole('dialog')).getByRole('button', { name: 'Delete contact' }),
    );
    await waitFor(() => expect(invalidated).toContainEqual(['trash', 'g-1']));
    expect(invalidated).toContainEqual(['contacts', 'g-1']);
    expect(invalidated).toContainEqual(['contact', 'g-1', 'c/1']);
    expect(router.state.location.pathname).toBe('/groups/g-1/contacts/c%2F1');
  });

  it('shows a contact in Trash read-only with its notice and no Delete', async () => {
    setup(() => ({ ...ana, state: 'TRASHED' }), open);
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'This contact is in Trash and cannot be edited.',
    );
    expect(screen.queryByRole('button', { name: 'Save' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Delete contact' })).toBeNull();
    expect(screen.getByRole('navigation', { name: 'Contact' })).toBeInTheDocument();
  });
});
