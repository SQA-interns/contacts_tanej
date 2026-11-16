import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ApiError, type ApiClient } from '@/lib/api/client';
import { groupsApi, konferenca, skupno, signedIn } from '@/test/groupsApi';
import { renderApp } from '@/test/renderApp';
import type { Contact } from '@/features/contacts/contactValues';
import { attributeConflicts, combinedTags, preselect } from './MergePage';

const contact = (id: string, createdAt: string, extra: Partial<Contact> = {}): Contact =>
  ({ id, state: 'ACTIVE', createdAt, revision: 3, versionNumber: 1, lastName: 'Novak', ...extra }) as Contact;

const bare = contact('x', '2026-06-01T10:00:00Z');
delete (bare as Partial<Contact>).lastName;

describe('merge helpers on contacts without tags or attributes', () => {
  it('treat missing lists as empty', () => {
    expect(attributeConflicts([bare, bare])).toEqual([]);
    expect(combinedTags([bare, bare])).toEqual([]);
    expect(
      attributeConflicts([bare, contact('y', bare.createdAt, { attributes: [{ key: 'k', value: 'v' }] })]),
    ).toEqual([]);
    expect(combinedTags([bare, contact('y', bare.createdAt, { tags: ['t'] })])).toEqual(['t']);
  });

  it('keeps the first spelling of a key and every value once', () => {
    const one = contact('1', bare.createdAt, { attributes: [{ key: 'Level', value: 'a' }] });
    const two = contact('2', bare.createdAt, {
      attributes: [
        { key: 'level', value: 'b' },
        { key: 'level', value: 'a' },
      ],
    });
    expect(attributeConflicts([one, two])).toEqual([{ key: 'Level', values: ['a', 'b'] }]);
  });

  it('preselects 0 when no contact has a value', () => {
    expect(Object.values(preselect([bare, bare])).every((n) => n === 0)).toBe(true);
  });
});

const a = contact('a', '2026-06-01T10:00:00Z', {
  company: 'Acme',
  country: 'SI',
  tags: ['x'],
  attributes: [{ key: 'Level', value: 'java' }],
});
const b = contact('b', '2026-06-02T10:00:00Z', {
  company: 'Beta',
  phone: '+386 41 555 123',
  country: 'AT',
  tags: ['X', 'y'],
  attributes: [{ key: 'level', value: 'python' }],
});
const c = contact('c', '2026-06-03T10:00:00Z', { tags: ['z'] });

function mergeApi(merge?: (body: unknown) => Promise<unknown>, byId: Record<string, Contact> = { a, b, c }) {
  const { api: groups } = groupsApi([konferenca, skupno]);
  const calls: { method: string; path: string; body?: unknown }[] = [];
  const api: ApiClient = {
    ...groups,
    request: (async (method: string, path: string, body?: unknown) => {
      if (path.startsWith('/groups/g-1/')) calls.push({ method, path, body });
      const one = /\/contacts\/([^/]+)$/.exec(path);
      if (one) return byId[decodeURIComponent(one[1]!)];
      if (path.endsWith('/merges')) return merge ? merge(body) : { ...a, id: 'a' };
      return groups.request(method, path, body);
    }) as ApiClient['request'],
  };
  return { api, calls };
}

const open = (query: string, merge?: (body: unknown) => Promise<unknown>, group = 'g-1') => {
  const fake = mergeApi(merge);
  renderApp(`/groups/${group}/duplicates/merge${query}`, { auth: signedIn(), api: fake.api });
  return fake;
};
const confirm = () => screen.getByRole('button', { name: /Confirm merge|Merging/ });
const refuse = (code: string, errors?: unknown) => async () => {
  throw new ApiError(400, { status: 400, code, message: 'x', errors } as never);
};

describe('MergePage: what it asks for', () => {
  it('reads every contact of ids once, with encoded ids, and links back to the duplicates', async () => {
    const fake = open('?ids=b,a');
    await screen.findByText('Survivor (the oldest, stays)');
    expect(
      fake.calls
        .filter((x) => x.method === 'GET')
        .map((x) => x.path)
        .sort(),
    ).toEqual(['/groups/g-1/contacts/a', '/groups/g-1/contacts/b']);
    const crumb = screen.getByText(/\/ Merge$/);
    expect(within(crumb).getByRole('link', { name: 'Duplicates' })).toHaveAttribute(
      'href',
      '/groups/g-1/duplicates',
    );
  });

  it('asks for nothing and shows an error when fewer than two ids are given', async () => {
    for (const query of ['', '?ids=a', '?ids=a,,']) {
      const fake = open(query);
      expect(await screen.findByText('Something went wrong. Please try again.')).toBeInTheDocument();
      expect(screen.getByRole('heading', { name: 'Merge contacts' })).toBeInTheDocument();
      expect(screen.queryByText('Survivor (the oldest, stays)')).toBeNull();
      expect(fake.calls).toHaveLength(0);
      document.body.innerHTML = '';
    }
  });

  it('asks for nothing from a Viewer', async () => {
    const fake = open('?ids=a,b', undefined, 'g-3');
    expect(await screen.findByRole('heading', { level: 1, name: 'Skupno' })).toBeInTheDocument();
    await new Promise((r) => setTimeout(r, 50));
    expect(fake.calls).toHaveLength(0);
    expect(screen.queryByText(/\/ Merge$/)).toBeNull();
  });

  it('shows an error with a retry when a contact cannot be read', async () => {
    let failing = true;
    const { api: groups } = groupsApi([konferenca]);
    const api: ApiClient = {
      ...groups,
      request: (async (method: string, path: string, body?: unknown) => {
        if (/\/contacts\/[ab]$/.test(path)) {
          if (failing)
            throw new ApiError(500, { status: 500, code: 'INTERNAL_ERROR', message: 'x' } as never);
          return path.endsWith('/a') ? a : b;
        }
        return groups.request(method, path, body);
      }) as ApiClient['request'],
    };
    renderApp('/groups/g-1/duplicates/merge?ids=a,b', { auth: signedIn(), api });
    expect(await screen.findByText('Something went wrong. Please try again.')).toBeInTheDocument();
    failing = false;
    await userEvent.click(screen.getByRole('button', { name: 'Try again' }));
    expect(await screen.findByText('Survivor (the oldest, stays)')).toBeInTheDocument();
  });
});

describe('MergePage: the choices', () => {
  it('names the oldest contact the survivor and the others as joining, oldest first', async () => {
    open('?ids=c,b,a');
    await screen.findByText('Survivor (the oldest, stays)');
    const group = screen.getByRole('group', { name: 'Fields' });
    expect(Array.from(group.querySelectorAll('strong')).map((n) => n.textContent)).toEqual([
      'Survivor (the oldest, stays)',
      'Contact to join',
      'Contact to join',
    ]);
  });

  it('preselects the first non-empty value per field, shows the country by name and an empty one as a dash', async () => {
    open('?ids=a,b');
    await screen.findByText('Survivor (the oldest, stays)');
    const group = screen.getByRole('group', { name: 'Fields' });
    const rowOf = (label: string) =>
      within(group).getByText(label, { selector: 'span.font-semibold' }).parentElement as HTMLElement;
    const country = within(rowOf('Country')).getAllByRole('radio');
    expect(country.map((r) => (r as HTMLInputElement).checked)).toEqual([true, false]);
    expect(within(rowOf('Country')).getByText('Slovenia')).toBeInTheDocument();
    expect(within(rowOf('Country')).getByText('Austria')).toBeInTheDocument();
    const phone = within(rowOf('Phone')).getAllByRole('radio') as HTMLInputElement[];
    expect(phone.map((r) => r.checked)).toEqual([false, true]);
    expect(within(rowOf('Phone')).getByLabelText('empty')).toHaveTextContent('–');
    expect(within(rowOf('Phone')).getByText('+386 41 555 123')).toBeInTheDocument();
    const company = within(rowOf('Company')).getAllByRole('radio') as HTMLInputElement[];
    expect(company.map((r) => r.checked)).toEqual([true, false]);
  });

  it('keeps a pick per field and sends exactly what is picked', async () => {
    const fake = open('?ids=b,a');
    await screen.findByText('Survivor (the oldest, stays)');
    const group = screen.getByRole('group', { name: 'Fields' });
    const rowOf = (label: string) =>
      within(group).getByText(label, { selector: 'span.font-semibold' }).parentElement as HTMLElement;
    await userEvent.click(within(rowOf('Company')).getAllByRole('radio')[1]!);
    expect(
      (within(rowOf('Company')).getAllByRole('radio') as HTMLInputElement[]).map((r) => r.checked),
    ).toEqual([false, true]);
    expect(
      (within(rowOf('Country')).getAllByRole('radio') as HTMLInputElement[]).map((r) => r.checked),
    ).toEqual([true, false]);
    await userEvent.click(screen.getByRole('radio', { name: 'python' }));
    await userEvent.click(confirm());
    await waitFor(() => expect(fake.calls.some((x) => x.method === 'POST')).toBe(true));
    const post = fake.calls.find((x) => x.method === 'POST')!;
    expect(post.path).toBe('/groups/g-1/merges');
    const body = post.body as {
      fields: Record<string, string>;
      contacts: unknown[];
      attributeChoices: unknown[];
    };
    expect(body.fields.company).toBe('Beta');
    expect(body.fields.country).toBe('SI');
    expect(body.fields.phone).toBe('+386 41 555 123');
    expect(body.contacts).toEqual([
      { id: 'a', revision: 3 },
      { id: 'b', revision: 3 },
    ]);
    expect(body.attributeChoices).toEqual([{ key: 'Level', value: 'python' }]);
  });

  it('lists the combined tags once each and the conflicts with their values; none when there are none', async () => {
    open('?ids=a,b');
    await screen.findByText('Survivor (the oldest, stays)');
    expect(screen.getByRole('heading', { name: 'Combined tags' })).toBeInTheDocument();
    expect(screen.getAllByRole('listitem').map((li) => li.textContent)).toEqual(['x', 'y']);
    expect(screen.getByRole('heading', { name: 'Attribute conflicts' })).toBeInTheDocument();
    expect(screen.getByText('Same key, different values. Choose one per key.')).toBeInTheDocument();
    const fieldset = screen.getByText('Level', { selector: 'legend' }).closest('fieldset') as HTMLElement;
    const radios = within(fieldset).getAllByRole('radio') as HTMLInputElement[];
    expect(radios.map((r) => r.name)).toEqual(['attribute-level', 'attribute-level']);
    expect(radios.map((r) => r.checked)).toEqual([false, false]);
    await userEvent.click(radios[0]!);
    expect(radios.map((r) => r.checked)).toEqual([true, false]);
    await userEvent.click(radios[1]!);
    expect(radios.map((r) => r.checked)).toEqual([false, true]);
  });

  it('shows no conflict section and enables Confirm at once when no key conflicts', async () => {
    open('?ids=a,c');
    await screen.findByText('Survivor (the oldest, stays)');
    expect(screen.queryByRole('heading', { name: 'Attribute conflicts' })).toBeNull();
    expect(confirm()).toBeEnabled();
  });

  it('needs one choice per key before Confirm is enabled', async () => {
    const two = {
      a: contact('a', a.createdAt, {
        attributes: [
          { key: 'One', value: '1' },
          { key: 'Two', value: '2' },
        ],
      }),
      b: contact('b', b.createdAt, {
        attributes: [
          { key: 'one', value: 'x' },
          { key: 'two', value: 'y' },
        ],
      }),
    };
    const { api } = mergeApi(undefined, two);
    renderApp('/groups/g-1/duplicates/merge?ids=a,b', { auth: signedIn(), api });
    await screen.findByText('Survivor (the oldest, stays)');
    expect(confirm()).toBeDisabled();
    await userEvent.click(screen.getByRole('radio', { name: '1' }));
    expect(confirm()).toBeDisabled();
    await userEvent.click(screen.getByRole('radio', { name: 'y' }));
    expect(confirm()).toBeEnabled();
  });
});

describe('MergePage: the outcome', () => {
  it('shows "Merging …" while the request runs, then opens the survivor and refreshes the data', async () => {
    let release: (v: unknown) => void = () => undefined;
    const fake = open('?ids=a,c', () => new Promise((resolve) => (release = resolve)));
    await screen.findByText('Survivor (the oldest, stays)');
    await userEvent.click(confirm());
    expect(await screen.findByRole('button', { name: 'Merging …' })).toBeDisabled();
    release({ ...a, id: 'a' });
    await screen.findByRole('heading', { level: 1, name: 'Konferenca 2026' });
    expect(fake.calls.some((x) => x.path === '/groups/g-1/contacts/a' && x.method === 'GET')).toBe(true);
  });

  it('names the attribute limit and the tag limit, and uses the usual text for other refusals', async () => {
    const tooMany = (field: string) =>
      refuse('VALIDATION_FAILED', [{ field, code: 'TOO_MANY', message: 'x' }]);
    open('?ids=a,c', tooMany('attributes[3]'));
    await screen.findByText('Survivor (the oldest, stays)');
    await userEvent.click(confirm());
    expect(await screen.findByRole('alert')).toHaveTextContent('A contact has at most 50 attributes.');
    document.body.innerHTML = '';
    open('?ids=a,c', tooMany('tags[1]'));
    await screen.findByText('Survivor (the oldest, stays)');
    await userEvent.click(confirm());
    expect(await screen.findByRole('alert')).toHaveTextContent('A contact has at most 50 tags.');
    document.body.innerHTML = '';
    open('?ids=a,c', refuse('CONFLICT'));
    await screen.findByText('Survivor (the oldest, stays)');
    await userEvent.click(confirm());
    expect(await screen.findByRole('alert')).toHaveTextContent('This contact was changed by someone else.');
    document.body.innerHTML = '';
    open('?ids=a,c', refuse('VALIDATION_FAILED', [{ field: 'phone', code: 'OTHER', message: 'x' }]));
    await screen.findByText('Survivor (the oldest, stays)');
    await userEvent.click(confirm());
    expect(await screen.findByRole('alert')).not.toHaveTextContent('at most 50');
  });

  it('shows the generic text for a failure that is no ApiError', async () => {
    open('?ids=a,c', async () => {
      throw new Error('boom');
    });
    await screen.findByText('Survivor (the oldest, stays)');
    await userEvent.click(confirm());
    const alert = await screen.findByRole('alert');
    expect(alert.textContent).not.toBe('');
    expect(alert).not.toHaveTextContent('boom');
  });

  it('leaves the screen on Cancel', async () => {
    open('?ids=a,c');
    await screen.findByText('Survivor (the oldest, stays)');
    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }));
    await waitFor(() => expect(screen.queryByRole('heading', { name: 'Merge contacts' })).toBeNull());
  });
});
