import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ApiError, type ApiClient } from '@/lib/api/client';
import type { components } from '@/lib/api/schema';
import { COUNTRY_CODES } from '@/lib/countries';
import { groupsApi, konferenca, poroka, signedIn, skupno } from '@/test/groupsApi';
import { renderApp } from '@/test/renderApp';

type Contact = components['schemas']['Contact'];
type ErrorResponse = components['schemas']['ErrorResponse'];

const ana: Contact = {
  id: 'c-1',
  state: 'ACTIVE',
  revision: 1,
  versionNumber: 1,
  createdAt: '2026-11-10T10:00:00Z',
  firstName: 'Ana',
  lastName: 'Novak',
  email: 'ana.novak@example.com',
  company: 'Acme',
  phone: '+386 40 123 456',
  country: 'SI',
  tags: [],
  attributes: [],
};

function refusal(status: number, body: Partial<ErrorResponse>) {
  return new ApiError(status, { status, code: 'INTERNAL_ERROR', message: 'x', ...body });
}

/** The groups stand-in plus the contact endpoints of group g-1, answering as the test says. */
function contactsApi(
  answer: (method: string, path: string, body?: unknown) => unknown,
  suggestions: components['schemas']['Tag'][] = [],
  keys: string[] = [],
) {
  const { api: groups, calls } = groupsApi([konferenca, poroka, skupno]);
  const api: ApiClient = {
    ...groups,
    request: (async (method: string, path: string, body?: unknown) => {
      if (
        path.startsWith('/groups/g-') &&
        !path.endsWith('/contacts/search') &&
        (path.includes('/contacts') || path.includes('/tags') || path.includes('/attribute-keys'))
      ) {
        calls.push(`${method} ${path} ${body ? JSON.stringify(body) : ''}`.trim());
        if (path.includes('/tags')) return suggestions;
        if (path.includes('/attribute-keys')) return keys;
        const result = answer(method, path, body);
        if (result instanceof Error) throw result;
        return result;
      }
      return groups.request(method, path, body);
    }) as ApiClient['request'],
  };
  return { api, calls };
}

describe('the Add contact button (R-005-01, R-005-09)', () => {
  it('is offered to an Owner and an Editor and not to a Viewer', async () => {
    const { api } = contactsApi(() => undefined);
    renderApp('/groups/g-1', { auth: signedIn(), api });
    expect(await screen.findByRole('button', { name: 'Add contact' })).toBeInTheDocument();
  });

  it('is not offered to a Viewer', async () => {
    const { api } = contactsApi(() => undefined);
    renderApp('/groups/g-3', { auth: signedIn(), api });
    await screen.findByRole('heading', { name: 'Skupno' });
    expect(screen.queryByRole('button', { name: 'Add contact' })).not.toBeInTheDocument();
  });
});

describe('the new contact form (R-005-01, R-005-03, EX-005-10)', () => {
  it('shows the 11 fields with the country as a list of names, never as free text (AC-005-10)', async () => {
    const { api } = contactsApi(() => undefined);
    renderApp('/groups/g-1/contacts/new', { auth: signedIn(), api });
    for (const label of ['First name', 'Last name', 'E-mail', 'Company', 'Job title', 'Phone', 'Street']) {
      expect(await screen.findByLabelText(label)).toBeInTheDocument();
    }
    for (const label of ['Postal code', 'City', 'Notes'])
      expect(screen.getByLabelText(label)).toBeInTheDocument();
    const country = screen.getByLabelText('Country');
    expect(country.tagName).toBe('SELECT');
    expect(screen.getByRole('option', { name: 'Slovenia' })).toHaveValue('SI');
    expect(screen.queryByRole('option', { name: 'Slovenija' })).not.toBeInTheDocument();
    expect(COUNTRY_CODES).toHaveLength(249);
  });

  it('sends the typed values and opens the new contact after the save (R-005-01)', async () => {
    const { api, calls } = contactsApi((method) => (method === 'POST' ? { ...ana, id: 'c-9' } : ana));
    renderApp('/groups/g-1/contacts/new', { auth: signedIn(), api });
    await userEvent.type(await screen.findByLabelText('Last name'), 'Novak');
    await userEvent.type(screen.getByLabelText('E-mail'), 'ANA.Novak@Example.com');
    await userEvent.selectOptions(screen.getByLabelText('Country'), 'SI');
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    await screen.findByRole('heading', { name: 'Ana Novak', level: 1 });
    const post = calls.find((c) => c.startsWith('POST /groups/g-1/contacts'));
    expect(JSON.parse(post!.replace('POST /groups/g-1/contacts ', ''))).toMatchObject({
      lastName: 'Novak',
      email: 'ANA.Novak@Example.com',
      country: 'SI',
    });
  });

  it('names every refused field with its reason and keeps what was typed (R-005-03, EX-005-10)', async () => {
    const { api } = contactsApi(() =>
      refusal(400, {
        code: 'VALIDATION_FAILED',
        errors: [
          { field: 'email', code: 'INVALID_FORMAT', message: 'x' },
          { field: 'country', code: 'UNKNOWN_COUNTRY', message: 'x' },
        ],
      }),
    );
    renderApp('/groups/g-1/contacts/new', { auth: signedIn(), api });
    await userEvent.type(await screen.findByLabelText('E-mail'), 'ana.novak@');
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    expect(await screen.findByText('Check the e-mail address.')).toBeInTheDocument();
    expect(screen.getByText('Choose a country from the list.')).toBeInTheDocument();
    expect(screen.getByLabelText('E-mail')).toHaveValue('ana.novak@');
    expect(screen.getByLabelText('E-mail')).toBeInvalid();
  });

  it('explains the one-of-four rule at the form and at the first name (R-005-02, AC-005-02)', async () => {
    const { api } = contactsApi(() =>
      refusal(400, {
        code: 'VALIDATION_FAILED',
        errors: [{ field: 'firstName', code: 'REQUIRED_ONE_OF', message: 'x' }],
      }),
    );
    renderApp('/groups/g-1/contacts/new', { auth: signedIn(), api });
    await userEvent.type(await screen.findByLabelText('Phone'), '+386 40 123 456');
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    const texts = await screen.findAllByText(
      'Enter at least one of first name, last name, e-mail or company.',
    );
    expect(texts.length).toBeGreaterThanOrEqual(1);
    expect(screen.getByLabelText('First name')).toBeInvalid();
  });
});

describe('opening and saving a contact (R-005-06, R-005-07)', () => {
  it('shows the contact in a form and saves it with the revision it was opened with', async () => {
    const { api, calls } = contactsApi((method) => (method === 'PUT' ? { ...ana, revision: 2 } : ana));
    renderApp('/groups/g-1/contacts/c-1', { auth: signedIn(), api });
    const phone = await screen.findByLabelText('Phone');
    expect(phone).toHaveValue('+386 40 123 456');
    await userEvent.clear(phone);
    await userEvent.type(phone, '+386 41 555 123');
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    expect(await screen.findByRole('status', { name: '' })).toHaveTextContent('The contact was saved.');
    const put = calls.find((c) => c.startsWith('PUT'));
    expect(JSON.parse(put!.replace('PUT /groups/g-1/contacts/c-1 ', ''))).toMatchObject({
      phone: '+386 41 555 123',
      revision: 1,
    });
  });

  it('shows a Viewer the values as text, with no form, no Save (R-005-09, AC-005-08)', async () => {
    const { api } = contactsApi(() => ana);
    renderApp('/groups/g-3/contacts/c-1', { auth: signedIn(), api });
    expect(await screen.findByText('Acme')).toBeInTheDocument();
    expect(screen.getByText('Slovenia')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Save' })).not.toBeInTheDocument();
    expect(screen.queryByLabelText('Phone')).not.toBeInTheDocument();
  });

  it('shows a contact in Trash read-only with the notice (R-005-10)', async () => {
    const { api } = contactsApi(() => ({ ...ana, state: 'TRASHED' }));
    renderApp('/groups/g-1/contacts/c-1', { auth: signedIn(), api });
    expect(await screen.findByText(/This contact is in Trash and cannot be edited/)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Save' })).not.toBeInTheDocument();
  });

  it('tells the user the contact went to Trash while editing (EX-005-12)', async () => {
    let trashed = false;
    const { api } = contactsApi((method) => {
      if (method === 'PUT') {
        trashed = true;
        return refusal(409, { code: 'CONTACT_IN_TRASH' });
      }
      return trashed ? { ...ana, state: 'TRASHED', revision: 2 } : ana;
    });
    renderApp('/groups/g-1/contacts/c-1', { auth: signedIn(), api });
    await userEvent.type(await screen.findByLabelText('Company'), 'x');
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    expect(await screen.findByText(/This contact is in Trash and cannot be edited/)).toBeInTheDocument();
    await waitFor(() => expect(screen.queryByRole('button', { name: 'Save' })).not.toBeInTheDocument());
  });

  it('shows the not-found page for a contact that is not there (EX-005-11)', async () => {
    const { api } = contactsApi(() => refusal(404, { code: 'NOT_FOUND' }));
    renderApp('/groups/g-1/contacts/c-404', { auth: signedIn(), api });
    expect(await screen.findByRole('heading', { name: 'Not found' })).toBeInTheDocument();
  });
});

describe('the tags of a contact (R-006-01, R-006-06, R-006-09, R-006-10)', () => {
  const tagged: Contact = { ...ana, tags: ['udelezenec2025', 'neObvescaj'] };

  function putBody(calls: string[]) {
    const put = calls.find((c) => c.startsWith('PUT'));
    return JSON.parse(put!.replace('PUT /groups/g-1/contacts/c-1 ', ''));
  }

  it('shows the tags as chips and removes one with its button (R-006-06)', async () => {
    const { api, calls } = contactsApi((method) => (method === 'PUT' ? tagged : tagged));
    renderApp('/groups/g-1/contacts/c-1', { auth: signedIn(), api });
    const list = await screen.findByRole('list', { name: 'Tags of this contact' });
    expect(list).toHaveTextContent('udelezenec2025');
    await userEvent.click(screen.getByRole('button', { name: 'Remove tag neObvescaj' }));
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    await screen.findByRole('status');
    expect(putBody(calls).tags).toEqual(['udelezenec2025']);
  });

  it('adds a tag with Enter and sends all tags with the save (R-006-01)', async () => {
    const { api, calls } = contactsApi(() => ana);
    renderApp('/groups/g-1/contacts/c-1', { auth: signedIn(), api });
    await userEvent.type(await screen.findByLabelText('Tags'), 'udelezenec2025{Enter}');
    expect(screen.getByLabelText('Tags')).toHaveValue('');
    expect(screen.getByRole('list', { name: 'Tags of this contact' })).toHaveTextContent('udelezenec2025');
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    await screen.findByRole('status');
    expect(putBody(calls).tags).toEqual(['udelezenec2025']);
  });

  it('takes a tag that was typed but not confirmed along with the save', async () => {
    const { api, calls } = contactsApi(() => ana);
    renderApp('/groups/g-1/contacts/c-1', { auth: signedIn(), api });
    await userEvent.type(await screen.findByLabelText('Tags'), 'gost');
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    await screen.findByRole('status');
    expect(putBody(calls).tags).toEqual(['gost']);
  });

  it('suggests the tags of the group while typing and never forces one (R-006-10)', async () => {
    const { api, calls } = contactsApi(() => ana, [{ id: 't-1', name: 'udelezenec2025', contactCount: 3 }]);
    renderApp('/groups/g-1/contacts/c-1', { auth: signedIn(), api });
    await userEvent.type(await screen.findByLabelText('Tags'), 'ud');
    await waitFor(() =>
      expect(document.querySelector('datalist option[value="udelezenec2025"]')).not.toBeNull(),
    );
    expect(calls.some((c) => c.startsWith('GET /groups/g-1/tags?q=ud'))).toBe(true);
    await userEvent.type(screen.getByLabelText('Tags'), 'x{Enter}');
    expect(screen.getByRole('list', { name: 'Tags of this contact' })).toHaveTextContent('udx');
  });

  it('names the refused tag with the reason and keeps the typed tags (R-006-03, AC-006-03)', async () => {
    const { api } = contactsApi((method) =>
      method === 'PUT'
        ? refusal(400, {
            code: 'VALIDATION_FAILED',
            errors: [{ field: 'tags[0]', code: 'INVALID_CHARACTERS', message: 'x' }],
          })
        : ana,
    );
    renderApp('/groups/g-1/contacts/c-1', { auth: signedIn(), api });
    await userEvent.type(await screen.findByLabelText('Tags'), 'neka značka{Enter}');
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    expect(await screen.findByText('Tag names use letters, digits, - and _.')).toBeInTheDocument();
    expect(screen.getByText('neka značka', { selector: 'strong' })).toBeInTheDocument();
    expect(screen.getByRole('list', { name: 'Tags of this contact' })).toHaveTextContent('neka značka');
  });

  it('shows a Viewer the tags as text and no control to change them (R-006-09, AC-006-07)', async () => {
    const { api } = contactsApi(() => tagged);
    renderApp('/groups/g-3/contacts/c-1', { auth: signedIn(), api });
    expect(await screen.findByText('udelezenec2025')).toBeInTheDocument();
    expect(screen.getByText('neObvescaj')).toBeInTheDocument();
    expect(screen.queryByLabelText('Tags')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Remove tag/ })).not.toBeInTheDocument();
  });
});

describe('the attributes of a contact (R-008-01, R-008-02, R-008-05, R-008-07, R-008-08)', () => {
  const withAttributes: Contact = {
    ...ana,
    attributes: [
      { key: 'kompetenca', value: 'java' },
      { key: 'leto', value: '2025' },
    ],
  };

  function putBody(calls: string[]) {
    const put = calls.find((c) => c.startsWith('PUT'));
    return JSON.parse(put!.replace('PUT /groups/g-1/contacts/c-1 ', ''));
  }

  it('adds a row with Add attribute and sends key and value as text with the save (R-008-01, R-008-06)', async () => {
    const { api, calls } = contactsApi(() => ana);
    renderApp('/groups/g-1/contacts/c-1', { auth: signedIn(), api });
    await userEvent.click(await screen.findByRole('button', { name: 'Add attribute' }));
    await userEvent.type(screen.getByLabelText('Key'), 'leto');
    await userEvent.type(screen.getByLabelText('Value'), '2025');
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    await screen.findByRole('status');
    expect(putBody(calls).attributes).toEqual([{ key: 'leto', value: '2025' }]);
  });

  it('shows the attributes of the contact in rows and removes one (R-008-07)', async () => {
    const { api, calls } = contactsApi(() => withAttributes);
    renderApp('/groups/g-1/contacts/c-1', { auth: signedIn(), api });
    expect(await screen.findAllByLabelText('Key')).toHaveLength(2);
    await userEvent.click(screen.getByRole('button', { name: 'Remove attribute kompetenca' }));
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    await screen.findByRole('status');
    expect(putBody(calls).attributes).toEqual([{ key: 'leto', value: '2025' }]);
  });

  it('drops a row that was added and left empty', async () => {
    const { api, calls } = contactsApi(() => ana);
    renderApp('/groups/g-1/contacts/c-1', { auth: signedIn(), api });
    await userEvent.click(await screen.findByRole('button', { name: 'Add attribute' }));
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    await screen.findByRole('status');
    expect(putBody(calls).attributes).toEqual([]);
  });

  it('suggests the keys of the group while typing and never forces one (R-008-05)', async () => {
    const { api, calls } = contactsApi(() => ana, [], ['kompetenca']);
    renderApp('/groups/g-1/contacts/c-1', { auth: signedIn(), api });
    await userEvent.click(await screen.findByRole('button', { name: 'Add attribute' }));
    await userEvent.type(screen.getByLabelText('Key'), 'kom');
    await waitFor(() => expect(document.querySelector('datalist option[value="kompetenca"]')).not.toBeNull());
    expect(calls.some((c) => c.startsWith('GET /groups/g-1/attribute-keys?q=kom'))).toBe(true);
    await userEvent.type(screen.getByLabelText('Key'), 'p2');
    expect(screen.getByLabelText('Key')).toHaveValue('komp2');
  });

  it('names the refused attribute with the reason and keeps the typed rows (R-008-02, AC-008-02)', async () => {
    const { api } = contactsApi((method) =>
      method === 'PUT'
        ? refusal(400, {
            code: 'VALIDATION_FAILED',
            errors: [{ field: 'attributes[1].key', code: 'DUPLICATE_KEY', message: 'x' }],
          })
        : { ...ana, attributes: [{ key: 'kompetenca', value: 'java' }] },
    );
    renderApp('/groups/g-1/contacts/c-1', { auth: signedIn(), api });
    await userEvent.click(await screen.findByRole('button', { name: 'Add attribute' }));
    await userEvent.type(screen.getAllByLabelText('Key')[1]!, 'Kompetenca');
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    expect(await screen.findByText('This key is already used on this contact.')).toBeInTheDocument();
    expect(screen.getAllByLabelText('Key')[1]).toHaveValue('Kompetenca');
  });

  it('shows a Viewer the attributes as text and no control to change them (R-008-08, AC-008-07)', async () => {
    const { api } = contactsApi(() => withAttributes);
    renderApp('/groups/g-3/contacts/c-1', { auth: signedIn(), api });
    expect(await screen.findByText('kompetenca')).toBeInTheDocument();
    expect(screen.getByText('java')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Add attribute' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Remove attribute/ })).not.toBeInTheDocument();
  });
});

describe('a save refused as a conflict (R-005-08, AC-005-07)', () => {
  const changed: Contact = { ...ana, phone: '+386 41 555 123', revision: 2 };

  it('shows the message, the current values and the own entries, and overwrites nothing', async () => {
    const { api, calls } = contactsApi((method) =>
      method === 'PUT' ? refusal(409, { code: 'CONFLICT', current: changed }) : ana,
    );
    renderApp('/groups/g-1/contacts/c-1', { auth: signedIn(), api });
    const phone = await screen.findByLabelText('Phone');
    await userEvent.clear(phone);
    await userEvent.type(phone, '+386 42 000 000');
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    expect(await screen.findByText('This contact was changed by someone else.')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Current values' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Your entries' })).toBeInTheDocument();
    expect(screen.getByText('+386 41 555 123')).toBeInTheDocument();
    expect(screen.getByText('+386 42 000 000')).toBeInTheDocument();
    expect(calls.filter((c) => c.startsWith('PUT'))).toHaveLength(1);
  });

  it('continues from the current values after "Reload with the current values"', async () => {
    const { api } = contactsApi((method) =>
      method === 'PUT' ? refusal(409, { code: 'CONFLICT', current: changed }) : ana,
    );
    renderApp('/groups/g-1/contacts/c-1', { auth: signedIn(), api });
    await userEvent.type(await screen.findByLabelText('Company'), '2');
    await userEvent.click(screen.getByRole('button', { name: 'Save' }));
    await userEvent.click(await screen.findByRole('button', { name: 'Reload with the current values' }));
    expect(await screen.findByLabelText('Phone')).toHaveValue('+386 41 555 123');
    expect(screen.queryByText('This contact was changed by someone else.')).not.toBeInTheDocument();
  });
});
