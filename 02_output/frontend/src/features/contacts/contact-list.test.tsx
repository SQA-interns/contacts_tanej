import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { groupsApi, konferenca, skupno, signedIn, type ContactRow } from '@/test/groupsApi';
import { renderApp } from '@/test/renderApp';
import { pageNumbers } from './Pager';

function contact(i: number, extra: Partial<ContactRow> = {}): ContactRow {
  return {
    id: `c-${i}`,
    lastName: `Priimek${String(i).padStart(2, '0')}`,
    firstName: 'Ime',
    email: '',
    phone: '',
    company: '',
    city: '',
    country: '',
    tags: [],
    ...extra,
  };
}

const many = (n: number) => Array.from({ length: n }, (_, i) => contact(i));

describe('the contact list (R-009-01, R-009-02, R-009-09)', () => {
  it('shows the columns, the total, the tags and a link to the contact', async () => {
    const { api } = groupsApi([konferenca], {
      contacts: [contact(1, { email: 'ana@example.com', country: 'SI', tags: ['vip', 'gost'] })],
    });
    renderApp('/groups/g-1', { auth: signedIn(), api });
    expect(await screen.findByText('1 contact')).toBeInTheDocument();
    for (const name of ['Last name', 'First name', 'E-mail', 'Phone', 'Company', 'City', 'Country', 'Tags']) {
      expect(screen.getByRole('columnheader', { name: new RegExp(name) })).toBeInTheDocument();
    }
    expect(screen.getByRole('link', { name: 'Priimek01' })).toHaveAttribute(
      'href',
      '/groups/g-1/contacts/c-1',
    );
    expect(screen.getByText('ana@example.com')).toBeInTheDocument();
    expect(screen.getByText('Slovenia')).toBeInTheDocument();
    expect(screen.getByText('gost')).toBeInTheDocument();
  });

  it('cannot sort by the tags column', async () => {
    const { api } = groupsApi([konferenca], { contacts: many(2) });
    renderApp('/groups/g-1', { auth: signedIn(), api });
    await screen.findByText('2 contacts');
    expect(screen.getByRole('columnheader', { name: 'Tags' })).not.toContainElement(
      screen.queryByRole('button', { name: /Tags/ }),
    );
    expect(screen.queryByRole('button', { name: /Tags/ })).toBeNull();
  });

  it('pages with numbered buttons and asks for the page chosen', async () => {
    const { api, calls } = groupsApi([konferenca], { contacts: many(60) });
    renderApp('/groups/g-1', { auth: signedIn(), api });
    expect(await screen.findByText('60 contacts')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: '3' }));
    await waitFor(() => expect(calls.some((c) => c.includes('"page":3'))).toBe(true));
    expect(await screen.findByRole('link', { name: 'Priimek50' })).toBeInTheDocument();
  });
});

describe('search and sort (R-009-03, R-009-08, R-009-10)', () => {
  it('sends the search text and keeps it when the sort changes', async () => {
    const { api, calls } = groupsApi([konferenca], { contacts: many(30) });
    renderApp('/groups/g-1', { auth: signedIn(), api });
    await screen.findByText('30 contacts');
    await userEvent.type(screen.getByLabelText('Search'), 'priimek');
    await waitFor(() => expect(calls.some((c) => c.includes('"q":"priimek"'))).toBe(true));
    await userEvent.click(screen.getByRole('button', { name: /City/ }));
    await waitFor(() =>
      expect(calls.at(-1)).toContain('"q":"priimek","sort":{"field":"city","direction":"ASC"}'),
    );
    expect(screen.getByLabelText('Search')).toHaveValue('priimek');
    await userEvent.click(screen.getByRole('button', { name: /City/ }));
    await waitFor(() => expect(calls.at(-1)).toContain('"direction":"DESC"'));
  });

  it('goes back to page 1 when the search text changes', async () => {
    const { api, calls } = groupsApi([konferenca], { contacts: many(60) });
    renderApp('/groups/g-1', { auth: signedIn(), api });
    await screen.findByText('60 contacts');
    await userEvent.click(screen.getByRole('button', { name: '2' }));
    await waitFor(() => expect(calls.at(-1)).toContain('"page":2'));
    await userEvent.type(screen.getByLabelText('Search'), 'p');
    await waitFor(() => expect(calls.at(-1)).toContain('"q":"p","page":1'));
  });

  it('a Viewer has the same search and sort (R-009-10)', async () => {
    const { api } = groupsApi([skupno], { contacts: many(2) });
    renderApp('/groups/g-3', { auth: signedIn(), api });
    expect(await screen.findByText('2 contacts')).toBeInTheDocument();
    expect(screen.getByLabelText('Search')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Last name/ })).toBeInTheDocument();
  });
});

describe('the empty and no-match states (R-009-05, R-009-06)', () => {
  it('offers "Add contact" in an empty group to an Owner', async () => {
    const { api } = groupsApi([konferenca]);
    renderApp('/groups/g-1', { auth: signedIn(), api });
    expect(await screen.findByRole('heading', { name: 'No contacts yet' })).toBeInTheDocument();
    expect(screen.getByText('Add the first contact to this group.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Add contact' })).toBeInTheDocument();
  });

  it('shows a Viewer the text without the button', async () => {
    const { api } = groupsApi([skupno]);
    renderApp('/groups/g-3', { auth: signedIn(), api });
    expect(await screen.findByRole('heading', { name: 'No contacts yet' })).toBeInTheDocument();
    expect(screen.getByText('Nobody has added a contact to this group yet.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Add contact' })).toBeNull();
  });

  it('shows "No contacts match" and "Clear filters" restores the list', async () => {
    const { api } = groupsApi([konferenca], { contacts: [contact(1, { lastName: 'Novak' })] });
    renderApp('/groups/g-1', { auth: signedIn(), api });
    await screen.findByRole('link', { name: 'Novak' });
    await userEvent.type(screen.getByLabelText('Search'), 'zzzz');
    expect(await screen.findByRole('heading', { name: 'No contacts match' })).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Clear filters' }));
    expect(await screen.findByRole('link', { name: 'Novak' })).toBeInTheDocument();
    expect(screen.getByLabelText('Search')).toHaveValue('');
  });

  it('shows the error state when the list cannot be loaded', async () => {
    const { api } = groupsApi([konferenca]);
    const down = {
      ...api,
      request: (async (method: string, path: string, body?: unknown) => {
        if (path.endsWith('/contacts/search')) throw new Error('down');
        return api.request(method, path, body);
      }) as typeof api.request,
    };
    renderApp('/groups/g-1', { auth: signedIn(), api: down });
    const alert = await screen.findByRole('alert');
    expect(within(alert).getByRole('button', { name: 'Try again' })).toBeInTheDocument();
  });
});

describe('page numbers', () => {
  it('shows the first, the last and the neighbours of the current page', () => {
    expect(pageNumbers(1, 3)).toEqual([1, 2, 3]);
    expect(pageNumbers(10, 20)).toEqual([1, 8, 9, 10, 11, 12, 20]);
  });
});
