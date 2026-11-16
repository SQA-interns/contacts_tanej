import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { I18nextProvider } from 'react-i18next';
import { Services } from '@/app/services';
import { setupI18n } from '@/lib/i18n';
import { attributeProblems, type Attribute } from './AttributeRows';
import { ContactForm } from './ContactForm';
import { emptyValues, type Values } from './contactValues';
import { tagProblems } from './TagInput';

interface Options {
  initial?: Partial<Values>;
  tags?: string[];
  attributes?: Attribute[];
  problems?: Partial<Record<string, string>>;
  busy?: boolean;
  reply?: (path: string) => unknown;
}

function mount(o: Options = {}) {
  const request = vi.fn(async (_m: string, path: string) => (o.reply ? o.reply(path) : []));
  const onSubmit = vi.fn();
  const onCancel = vi.fn();
  const services = { api: { request, download: vi.fn() }, auth: {} } as never;
  render(
    <I18nextProvider i18n={setupI18n('en')}>
      <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
        <Services value={services}>
          <ContactForm
            groupId="g 1"
            initial={{ ...emptyValues, ...o.initial }}
            initialTags={o.tags ?? []}
            initialAttributes={o.attributes ?? []}
            problems={o.problems ?? {}}
            busy={o.busy ?? false}
            onSubmit={onSubmit}
            onCancel={onCancel}
          />
        </Services>
      </QueryClientProvider>
    </I18nextProvider>,
  );
  return { request, onSubmit, onCancel };
}

const form = () => screen.getByRole('form', { name: 'Contact' });
const save = () => userEvent.click(screen.getByRole('button', { name: 'Save' }));

describe('ContactForm: fields', () => {
  it('shows the nine text boxes, the country list and the notes in their order, with the typed values', () => {
    mount({ initial: { firstName: 'Ana', email: 'a@b.si', country: 'SI', notes: 'n' } });
    const labels = Array.from(form().querySelectorAll('label')).map((l) => l.textContent);
    expect(labels.slice(0, 11)).toEqual([
      'First name',
      'Last name',
      'E-mail',
      'Company',
      'Job title',
      'Phone',
      'Street',
      'Postal code',
      'City',
      'Country',
      'Notes',
    ]);
    expect(screen.getByLabelText('First name')).toHaveValue('Ana');
    expect(screen.getByLabelText('E-mail')).toHaveAttribute('type', 'email');
    expect(screen.getByLabelText('First name')).toHaveAttribute('type', 'text');
    expect(screen.getByLabelText('Phone')).toHaveAttribute('type', 'text');
    expect(screen.getByLabelText('Country')).toHaveValue('SI');
    expect(screen.getByLabelText('Country').tagName).toBe('SELECT');
    expect(screen.getByLabelText('Notes').tagName).toBe('TEXTAREA');
    expect(screen.getByLabelText('Notes')).toHaveAttribute('rows', '5');
    expect(within(screen.getByLabelText('Country')).getByRole('option', { name: 'Choose …' })).toHaveValue(
      '',
    );
    expect(screen.getByRole('heading', { name: 'Details' })).toBeInTheDocument();
  });

  it('shows the hints of e-mail, phone and notes only, each linked to its field', () => {
    mount();
    for (const [label, text] of [
      ['E-mail', 'For example name@example.com'],
      ['Phone', 'Digits, spaces and + ( ) - . /'],
      ['Notes', 'Up to 5,000 characters.'],
    ] as const) {
      const field = screen.getByLabelText(label);
      const hint = screen.getByText(text);
      expect(field.getAttribute('aria-describedby')).toBe(hint.id);
    }
    expect(screen.getByLabelText('First name')).not.toHaveAttribute('aria-describedby');
    expect(screen.getByLabelText('First name')).not.toHaveAttribute('aria-invalid');
  });

  it('shows a refused field with its reason, links it and flags it, for each control kind', () => {
    mount({
      problems: { email: 'INVALID_FORMAT', notes: 'TOO_LONG', country: 'UNKNOWN_COUNTRY', city: 'TOO_LONG' },
    });
    const email = screen.getByLabelText('E-mail');
    expect(email).toHaveAttribute('aria-invalid', 'true');
    const ids = email.getAttribute('aria-describedby')!.split(' ');
    expect(ids).toHaveLength(2);
    expect(document.getElementById(ids[0]!)).toHaveTextContent('For example name@example.com');
    expect(document.getElementById(ids[1]!)).toHaveTextContent('Check the e-mail address.');
    const notes = screen.getByLabelText('Notes');
    expect(notes).toHaveAttribute('aria-invalid', 'true');
    expect(document.getElementById(notes.getAttribute('aria-describedby')!.split(' ')[1]!)).toHaveTextContent(
      'Use at most 5000 characters.',
    );
    const country = screen.getByLabelText('Country');
    expect(country).toHaveAttribute('aria-invalid', 'true');
    expect(country.getAttribute('aria-describedby')).toMatch(/-country-error$/);
    const city = screen.getByLabelText('City');
    expect(city.getAttribute('aria-describedby')).toMatch(/-city-error$/);
    expect(
      screen.getByText('Use at most 255 characters.', { selector: `p[id$="-city-error"]` }),
    ).toBeInTheDocument();
    expect(screen.getByLabelText('Company')).not.toHaveAttribute('aria-invalid');
  });

  it('uses the limit of a phone number and the general text for an unknown code', () => {
    mount({ problems: { phone: 'TOO_LONG', street: 'SOMETHING_NEW' } });
    expect(screen.getByText('Use at most 30 characters.')).toBeInTheDocument();
    expect(screen.getByText('The data is not valid.')).toBeInTheDocument();
  });

  it('explains "at least one of" as a banner and at the field, as alerts, and only when the first name carries that code', () => {
    mount({ problems: { firstName: 'REQUIRED_ONE_OF' } });
    expect(
      screen
        .getAllByRole('alert')
        .filter((a) => a.textContent === 'Enter at least one of first name, last name, e-mail or company.'),
    ).toHaveLength(2);
    expect(form().querySelector('div[role="alert"]')).toHaveTextContent('Enter at least one of');
  });

  it('shows no such alert for another refusal of the first name', () => {
    mount({ problems: { firstName: 'TOO_LONG' } });
    expect(screen.queryByText(/Enter at least one of first name/)).toBeNull();
  });

  it('keeps typing in every field and sends the values', async () => {
    const { onSubmit } = mount();
    await userEvent.type(screen.getByLabelText('First name'), 'Ana');
    await userEvent.type(screen.getByLabelText('Phone'), '123');
    await userEvent.selectOptions(screen.getByLabelText('Country'), 'Slovenia');
    await userEvent.type(screen.getByLabelText('Notes'), 'hi');
    await save();
    const [values, tags, attributes] = onSubmit.mock.calls[0]!;
    expect(values).toEqual({ ...emptyValues, firstName: 'Ana', phone: '123', country: 'SI', notes: 'hi' });
    expect(tags).toEqual([]);
    expect(attributes).toEqual([]);
  });
});

describe('ContactForm: buttons', () => {
  it('shows "Saving …" and disables Cancel while busy', () => {
    mount({ busy: true });
    expect(screen.getByRole('button', { name: /Saving/ })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
    expect(screen.queryByRole('button', { name: 'Save' })).toBeNull();
  });

  it('shows "Save" and a usable Cancel when not busy', async () => {
    const { onCancel, onSubmit } = mount();
    expect(screen.getByRole('button', { name: 'Save' })).toHaveAttribute('type', 'submit');
    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(onCancel).toHaveBeenCalledTimes(1);
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('does not reload the page on submit (default prevented)', () => {
    mount();
    const event = new Event('submit', { bubbles: true, cancelable: true });
    form().dispatchEvent(event);
    expect(event.defaultPrevented).toBe(true);
  });
});

describe('ContactForm: tags and attributes on save', () => {
  it('adds the tag still typed (trimmed) to the confirmed ones and empties the box', async () => {
    const { onSubmit } = mount({ tags: ['a'] });
    const box = screen.getByLabelText('Tags');
    await userEvent.type(box, '  b  ');
    await save();
    expect(onSubmit.mock.calls[0]![1]).toEqual(['a', 'b']);
    expect(box).toHaveValue('');
    expect(screen.getByText('b')).toBeInTheDocument();
  });

  it('adds nothing for blank typing', async () => {
    const { onSubmit } = mount({ tags: ['a'] });
    await userEvent.type(screen.getByLabelText('Tags'), '   ');
    await save();
    expect(onSubmit.mock.calls[0]![1]).toEqual(['a']);
    expect(screen.getByLabelText('Tags')).toHaveValue('');
  });

  it('drops attribute rows that are empty and keeps those with a key or only a value', async () => {
    const { onSubmit } = mount({
      attributes: [
        { key: '', value: '' },
        { key: '  ', value: '' },
        { key: 'k', value: '' },
        { key: '', value: 'v' },
        { key: ' ', value: ' ' },
      ],
    });
    await save();
    expect(onSubmit.mock.calls[0]![2]).toEqual([
      { key: 'k', value: '' },
      { key: '', value: 'v' },
      { key: ' ', value: ' ' },
    ]);
    expect(screen.getAllByLabelText('Key')).toHaveLength(3);
  });

  it('shows the refused tags and attributes of the last save', () => {
    mount({
      tags: ['ok', 'bad tag'],
      problems: {
        'tags[1]': 'INVALID_CHARACTERS',
        tags: 'TOO_MANY',
        'attributes[0].key': 'REQUIRED',
        attributes: 'TOO_MANY',
      },
      attributes: [{ key: '', value: 'x' }],
    });
    expect(screen.getByText('Tag names use letters, digits, - and _.')).toBeInTheDocument();
    expect(screen.getByText('bad tag', { selector: 'strong' })).toBeInTheDocument();
    expect(screen.getByText('A contact has at most 50 tags.')).toBeInTheDocument();
    expect(screen.getByText('An attribute needs a key.')).toBeInTheDocument();
    expect(screen.getByText('A contact has at most 50 attributes.')).toBeInTheDocument();
  });
});

describe('tagProblems and attributeProblems', () => {
  it('read the place of a refusal from the field name', () => {
    expect(
      tagProblems({
        tags: 'TOO_MANY',
        'tags[2]': 'TOO_LONG',
        'tags[x]': 'A',
        tagsX: 'B',
        'xtags[1]': 'C',
        city: 'D',
      }),
    ).toEqual([
      { index: null, code: 'TOO_MANY' },
      { index: 2, code: 'TOO_LONG' },
    ]);
    expect(tagProblems({ tags: undefined })).toEqual([]);
    expect(tagProblems({ tags: '' })).toEqual([]);
    expect(
      attributeProblems({
        attributes: 'TOO_MANY',
        'attributes[0]': 'A',
        'attributes[1].key': 'REQUIRED',
        'attributes[2].value': 'TOO_LONG',
        'attributes[3].other': 'X',
        'attributes[a]': 'X',
        xattributes: 'X',
        'attributes[1].keyx': 'X',
        name: 'X',
      }),
    ).toEqual([
      { index: null, code: 'TOO_MANY' },
      { index: 0, code: 'A' },
      { index: 1, code: 'REQUIRED' },
      { index: 2, code: 'TOO_LONG' },
    ]);
    expect(attributeProblems({ attributes: '' })).toEqual([]);
  });
});

describe('TagInput', () => {
  it('adds a tag with Enter or a comma, not with other keys, and removes exactly the chosen one', async () => {
    const { onSubmit } = mount();
    const box = screen.getByLabelText('Tags');
    expect(screen.queryByRole('list', { name: 'Tags of this contact' })).toBeNull();
    await userEvent.type(box, 'one{Enter}two,three{Enter}');
    expect(box).toHaveValue('');
    await userEvent.type(box, 'x');
    expect(box).toHaveValue('x');
    await userEvent.clear(box);
    await userEvent.type(box, '{Enter}');
    const list = screen.getByRole('list', { name: 'Tags of this contact' });
    expect(
      within(list)
        .getAllByRole('listitem')
        .map((li) => li.textContent),
    ).toEqual(['one×', 'two×', 'three×']);
    await userEvent.click(screen.getByRole('button', { name: 'Remove tag two' }));
    expect(
      within(list)
        .getAllByRole('listitem')
        .map((li) => li.textContent),
    ).toEqual(['one×', 'three×']);
    await save();
    expect(onSubmit.mock.calls[0]![1]).toEqual(['one', 'three']);
  });

  it('does not add a tag on Enter when the field is blank and does not submit the form', async () => {
    const { onSubmit } = mount();
    await userEvent.type(screen.getByLabelText('Tags'), '   {Enter}');
    expect(screen.queryByRole('list', { name: 'Tags of this contact' })).toBeNull();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('flags the box, links its hint and offers the tags of the group while typing', async () => {
    const { request } = mount({
      reply: () => [
        { id: 't1', name: 'alpha' },
        { id: 't2', name: 'alps' },
      ],
    });
    const box = screen.getByLabelText('Tags');
    expect(box).not.toHaveAttribute('aria-invalid');
    expect(request).not.toHaveBeenCalled();
    const hint = screen.getByText(/Press Enter to add a tag/);
    expect(box.getAttribute('aria-describedby')).toBe(hint.id);
    await userEvent.type(box, ' al ');
    await waitFor(() => expect(request).toHaveBeenCalledWith('GET', '/groups/g%201/tags?q=al'));
    await waitFor(() =>
      expect(
        Array.from(document.querySelectorAll(`datalist[id="${box.getAttribute('list')}"] option`)).map(
          (o) => (o as HTMLOptionElement).value,
        ),
      ).toEqual(['alpha', 'alps']),
    );
  });

  it('flags the box when a tag was refused', () => {
    mount({ tags: ['a'], problems: { 'tags[0]': 'TOO_SHORT' } });
    expect(screen.getByLabelText('Tags')).toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByText('A tag needs at least 1 character.')).toBeInTheDocument();
    expect(screen.getByText('a', { selector: 'strong' })).toBeInTheDocument();
  });

  it('names an unknown code generally and shows no tag name for a whole-list problem or a missing index', () => {
    mount({ tags: ['a'], problems: { tags: 'ODD', 'tags[7]': 'TOO_LONG' } });
    expect(screen.getByText('This tag is not valid.')).toBeInTheDocument();
    expect(screen.getByText('A tag has at most 50 characters.')).toBeInTheDocument();
    expect(document.querySelectorAll('p[role="alert"] strong')).toHaveLength(0);
  });
});

describe('AttributeRows', () => {
  it('adds an empty row, edits key and value and removes the right row', async () => {
    const { onSubmit } = mount({
      attributes: [
        { key: 'A', value: '1' },
        { key: 'B', value: '2' },
      ],
    });
    await userEvent.click(screen.getByRole('button', { name: 'Add attribute' }));
    expect(screen.getAllByLabelText('Key')).toHaveLength(3);
    const keys = screen.getAllByLabelText('Key') as HTMLInputElement[];
    const values = screen.getAllByLabelText('Value') as HTMLInputElement[];
    expect(keys.map((k) => k.value)).toEqual(['A', 'B', '']);
    await userEvent.type(keys[2]!, 'C');
    await userEvent.type(values[2]!, '3');
    await userEvent.type(values[0]!, 'x');
    await userEvent.click(screen.getByRole('button', { name: 'Remove attribute B' }));
    await save();
    expect(onSubmit.mock.calls[0]![2]).toEqual([
      { key: 'A', value: '1x' },
      { key: 'C', value: '3' },
    ]);
  });

  it('shows the refused rows with their reasons next to the row, and whole-list problems below', () => {
    mount({
      attributes: [
        { key: 'A', value: '1' },
        { key: 'A', value: '2' },
      ],
      problems: {
        'attributes[1].key': 'DUPLICATE_KEY',
        'attributes[0].value': 'TOO_LONG',
        attributes: 'ODD',
      },
    });
    const rows = screen.getAllByLabelText('Key') as HTMLInputElement[];
    expect(rows[0]).toHaveAttribute('aria-invalid', 'true');
    expect(rows[1]).toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByText('This key is already used on this contact.')).toBeInTheDocument();
    expect(screen.getByText('A key has at most 50 and a value at most 255 characters.')).toBeInTheDocument();
    expect(screen.getByText('This attribute is not valid.')).toBeInTheDocument();
  });

  it('does not flag a row that was not refused', () => {
    mount({
      attributes: [
        { key: 'A', value: '1' },
        { key: 'B', value: '2' },
      ],
      problems: { 'attributes[1]': 'REQUIRED' },
    });
    const keys = screen.getAllByLabelText('Key');
    expect(keys[0]).not.toHaveAttribute('aria-invalid');
    expect(keys[1]).toHaveAttribute('aria-invalid', 'true');
  });

  it('offers the keys of the group for the typed key, as a closed-by-default list that picks a key', async () => {
    const { request } = mount({ attributes: [{ key: '', value: '' }], reply: () => ['Level', 'Lang'] });
    const key = screen.getByLabelText('Key');
    expect(key).toHaveAttribute('role', 'combobox');
    expect(key).toHaveAttribute('aria-expanded', 'false');
    expect(key).not.toHaveAttribute('aria-controls');
    expect(request).not.toHaveBeenCalled();
    await userEvent.type(key, ' L ');
    await waitFor(() => expect(request).toHaveBeenCalledWith('GET', '/groups/g%201/attribute-keys?q=L'));
    const listbox = await screen.findByRole('listbox');
    expect(key).toHaveAttribute('aria-expanded', 'true');
    expect(key.getAttribute('aria-controls')).toBe(listbox.id);
    expect(
      within(listbox)
        .getAllByRole('option')
        .map((o) => o.textContent),
    ).toEqual(['Level', 'Lang']);
    expect(
      within(listbox)
        .getAllByRole('option')
        .map((o) => o.getAttribute('aria-selected')),
    ).toEqual(['false', 'false']);
    await userEvent.keyboard('{Escape}');
    expect(screen.queryByRole('listbox')).toBeNull();
    await userEvent.type(key, 'x');
    expect(await screen.findByRole('listbox')).toBeInTheDocument();
    await userEvent.tab();
    expect(screen.queryByRole('listbox')).toBeNull();
    await userEvent.click(key);
    const again = await screen.findByRole('listbox');
    await userEvent.pointer([
      { keys: '[MouseLeft>]', target: within(again).getByRole('option', { name: 'Lang' }) },
    ]);
    expect(screen.getByLabelText('Key')).toHaveValue('Lang');
    expect(screen.queryByRole('listbox')).toBeNull();
  });

  it('marks the option equal to the key as selected', async () => {
    mount({ attributes: [{ key: 'Level', value: '' }], reply: () => ['Level', 'Lang'] });
    await userEvent.click(screen.getByLabelText('Key'));
    const options = within(await screen.findByRole('listbox')).getAllByRole('option');
    expect(options.map((o) => o.getAttribute('aria-selected'))).toEqual(['true', 'false']);
  });

  it('keeps a key typed with only blanks away from the suggestions', async () => {
    const { request } = mount({ attributes: [{ key: '   ', value: '' }] });
    await userEvent.click(screen.getByLabelText('Key'));
    expect(request).not.toHaveBeenCalled();
    expect(screen.queryByRole('listbox')).toBeNull();
  });
});
