import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { I18nextProvider } from 'react-i18next';
import { Services } from '@/app/services';
import { setupI18n } from '@/lib/i18n';
import { FilterDialog } from './FilterDialog';

function mount(reply: (path: string) => unknown = () => []) {
  const request = vi.fn(async (_method: string, path: string) => reply(path));
  const onAdd = vi.fn();
  const onClose = vi.fn();
  const services = { api: { request, download: vi.fn() }, auth: {} } as never;
  render(
    <I18nextProvider i18n={setupI18n('en')}>
      <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
        <Services value={services}>
          <FilterDialog groupId="g 1" onAdd={onAdd} onClose={onClose} />
        </Services>
      </QueryClientProvider>
    </I18nextProvider>,
  );
  return { request, onAdd, onClose };
}

const dialog = () => screen.getByRole('dialog');
const pick = (type: string) => userEvent.selectOptions(within(dialog()).getByLabelText('Condition'), type);
const add = () => userEvent.click(within(dialog()).getByRole('button', { name: 'Add condition' }));

describe('FilterDialog: the condition it builds', () => {
  it('builds TAGS_ALL and TAGS_ANY from the confirmed tags plus the one still typed (trimmed)', async () => {
    const { onAdd } = mount();
    await pick('Has all of these tags');
    await userEvent.type(within(dialog()).getByLabelText('Tags'), 'a{Enter}  b  ');
    await add();
    expect(onAdd).toHaveBeenCalledWith({ type: 'TAGS_ALL', tags: ['a', 'b'] });
  });

  it('builds TAGS_ANY from one typed tag only', async () => {
    const { onAdd } = mount();
    await pick('Has any of these tags');
    await userEvent.type(within(dialog()).getByLabelText('Tags'), 'x');
    await add();
    expect(onAdd).toHaveBeenCalledWith({ type: 'TAGS_ANY', tags: ['x'] });
  });

  it('refuses tags when there are none, and blank typing does not count as a tag', async () => {
    const { onAdd } = mount();
    await pick('Has any of these tags');
    await userEvent.type(within(dialog()).getByLabelText('Tags'), '   ');
    await add();
    expect(onAdd).not.toHaveBeenCalled();
    expect(within(dialog()).getByRole('alert')).toHaveTextContent('Enter at least one tag.');
  });

  it('builds TAG_NOT with a trimmed tag and refuses a blank one', async () => {
    const { onAdd } = mount();
    await pick('Does not have tag');
    await userEvent.type(within(dialog()).getByLabelText('Tag'), '   ');
    await add();
    expect(onAdd).not.toHaveBeenCalled();
    expect(within(dialog()).getByRole('alert')).toHaveTextContent('Enter a tag.');
    await userEvent.type(within(dialog()).getByLabelText('Tag'), ' vip ');
    await add();
    expect(onAdd).toHaveBeenCalledWith({ type: 'TAG_NOT', tag: 'vip' });
  });

  it('builds ATTRIBUTE_EQUALS with trimmed key and value, and ATTRIBUTE_EXISTS with the key only', async () => {
    const { onAdd } = mount();
    await pick('Attribute key equals value');
    await userEvent.type(within(dialog()).getByLabelText('Attribute key'), ' Level ');
    await userEvent.type(within(dialog()).getByLabelText('Attribute value'), ' gold ');
    await add();
    expect(onAdd).toHaveBeenLastCalledWith({ type: 'ATTRIBUTE_EQUALS', key: 'Level', value: 'gold' });
    await pick('Attribute key exists');
    await userEvent.type(within(dialog()).getByLabelText('Attribute key'), 'Level');
    await add();
    expect(onAdd).toHaveBeenLastCalledWith({ type: 'ATTRIBUTE_EXISTS', key: 'Level' });
  });

  it('refuses a blank key for both attribute conditions', async () => {
    const { onAdd } = mount();
    await pick('Attribute key equals value');
    await userEvent.type(within(dialog()).getByLabelText('Attribute key'), '  ');
    await add();
    expect(within(dialog()).getByRole('alert')).toHaveTextContent('Enter a key.');
    await pick('Attribute key exists');
    await userEvent.type(within(dialog()).getByLabelText('Attribute key'), '  ');
    await add();
    expect(within(dialog()).getByRole('alert')).toHaveTextContent('Enter a key.');
    expect(onAdd).not.toHaveBeenCalled();
  });

  it('builds COUNTRY from the chosen code and refuses none', async () => {
    const { onAdd } = mount();
    await pick('Country');
    await add();
    expect(within(dialog()).getByRole('alert')).toHaveTextContent('Choose a country.');
    await userEvent.selectOptions(within(dialog()).getByLabelText('Country'), 'Slovenia');
    expect(within(dialog()).queryByRole('alert')).toBeNull();
    await add();
    expect(onAdd).toHaveBeenCalledWith({ type: 'COUNTRY', country: 'SI' });
  });

  it('builds COMPANY with a trimmed name and refuses a blank one', async () => {
    const { onAdd } = mount();
    await pick('Company');
    await userEvent.type(within(dialog()).getByLabelText('Company'), '  ');
    await add();
    expect(within(dialog()).getByRole('alert')).toHaveTextContent('Enter a company.');
    await userEvent.type(within(dialog()).getByLabelText('Company'), ' Acme ');
    await add();
    expect(onAdd).toHaveBeenCalledWith({ type: 'COMPANY', company: 'Acme' });
  });

  it('asks for the type first, flags the select and clears the flag when it is chosen', async () => {
    const { onAdd } = mount();
    await add();
    const select = within(dialog()).getByLabelText('Condition');
    expect(select).toHaveAttribute('aria-invalid', 'true');
    const alert = within(dialog()).getByRole('alert');
    expect(alert).toHaveTextContent('Choose a condition.');
    expect(select.getAttribute('aria-describedby')).toBe(alert.id);
    expect(alert.id).toMatch(/-type-error$/);
    expect(onAdd).not.toHaveBeenCalled();
  });

  it('flags only the missing field and clears the message on the next change', async () => {
    mount();
    await pick('Company');
    await add();
    const input = within(dialog()).getByLabelText('Company');
    expect(input).toHaveAttribute('aria-invalid', 'true');
    expect(input.getAttribute('aria-describedby')).toBe(within(dialog()).getByRole('alert').id);
    expect(within(dialog()).getByLabelText('Condition')).not.toHaveAttribute('aria-invalid');
    await userEvent.type(input, 'A');
    expect(within(dialog()).queryByRole('alert')).toBeNull();
    expect(input).not.toHaveAttribute('aria-invalid');
    expect(input).not.toHaveAttribute('aria-describedby');
  });

  it('forgets everything typed when the condition changes', async () => {
    mount();
    await pick('Company');
    await userEvent.type(within(dialog()).getByLabelText('Company'), 'Acme');
    await pick('Does not have tag');
    await pick('Company');
    expect(within(dialog()).getByLabelText('Company')).toHaveValue('');
  });
});

describe('FilterDialog: the tag list', () => {
  it('adds a tag with Enter or a comma, trims it, empties the field and ignores other keys and blanks', async () => {
    mount();
    await pick('Has all of these tags');
    const typed = within(dialog()).getByLabelText('Tags');
    await userEvent.type(typed, ' one {Enter}');
    expect(typed).toHaveValue('');
    await userEvent.type(typed, 'two,');
    expect(typed).toHaveValue('');
    await userEvent.type(typed, 'x');
    expect(typed).toHaveValue('x');
    await userEvent.clear(typed);
    await userEvent.type(typed, '{Enter}');
    expect(within(dialog()).getAllByRole('listitem')).toHaveLength(2);
    expect(within(dialog()).getByText('one')).toBeInTheDocument();
    expect(within(dialog()).getByText('two')).toBeInTheDocument();
  });

  it('shows no list while there are no tags, and removes exactly the tag whose button is pressed', async () => {
    const { onAdd } = mount();
    await pick('Has all of these tags');
    expect(within(dialog()).queryByRole('list')).toBeNull();
    const typed = within(dialog()).getByLabelText('Tags');
    await userEvent.type(typed, 'a{Enter}b{Enter}c{Enter}');
    await userEvent.click(within(dialog()).getByRole('button', { name: 'Remove tag b' }));
    expect(within(dialog()).getAllByRole('listitem').map((li) => li.textContent)).toEqual(['a×', 'c×']);
    await userEvent.click(within(dialog()).getByRole('button', { name: 'Remove tag a' }));
    await userEvent.click(within(dialog()).getByRole('button', { name: 'Remove tag c' }));
    expect(within(dialog()).queryByRole('list')).toBeNull();
    await userEvent.type(typed, 'z');
    await add();
    expect(onAdd).toHaveBeenCalledWith({ type: 'TAGS_ALL', tags: ['z'] });
  });

  it('allows the same tag twice in the list (keys stay unique)', async () => {
    mount();
    await pick('Has all of these tags');
    await userEvent.type(within(dialog()).getByLabelText('Tags'), 'a{Enter}a{Enter}');
    expect(within(dialog()).getAllByRole('listitem')).toHaveLength(2);
  });
});

describe('FilterDialog: suggestions', () => {
  it('asks the group for tag names while tags are typed, and offers them in the list of the field', async () => {
    const { request } = mount((path) => (path.includes('/tags?') ? [{ name: 'alpha' }, { name: 'alps' }] : []));
    expect(request).not.toHaveBeenCalled();
    await pick('Has any of these tags');
    expect(request).not.toHaveBeenCalled();
    const typed = within(dialog()).getByLabelText('Tags');
    await userEvent.type(typed, 'al');
    await waitFor(() => expect(request).toHaveBeenCalledWith('GET', '/groups/g%201/tags?q=al'));
    const list = document.getElementById(typed.getAttribute('list') as string) as HTMLElement;
    await waitFor(() => expect(Array.from(list.querySelectorAll('option')).map((o) => o.value)).toEqual(['alpha', 'alps']));
    expect(list.tagName).toBe('DATALIST');
  });

  it('asks for tag names for TAG_NOT with the trimmed text, and not before something is typed', async () => {
    const { request } = mount(() => [{ name: 'vip' }]);
    await pick('Does not have tag');
    expect(request).not.toHaveBeenCalled();
    const tag = within(dialog()).getByLabelText('Tag');
    await userEvent.type(tag, ' v ');
    await waitFor(() => expect(request).toHaveBeenCalledWith('GET', '/groups/g%201/tags?q=v'));
    expect(tag.getAttribute('list')).toMatch(/-tag-list$/);
    await waitFor(() => expect(document.querySelector(`datalist[id="${tag.getAttribute('list')}"] option`)).not.toBeNull());
  });

  it('asks for attribute keys for both attribute conditions, as plain strings', async () => {
    const { request } = mount(() => ['Level', 'Lang']);
    await pick('Attribute key exists');
    const key = within(dialog()).getByLabelText('Attribute key');
    await userEvent.type(key, 'L');
    await waitFor(() => expect(request).toHaveBeenCalledWith('GET', '/groups/g%201/attribute-keys?q=L'));
    await waitFor(() =>
      expect(Array.from(document.querySelectorAll(`datalist[id="${key.getAttribute('list')}"] option`)).map((o) => (o as HTMLOptionElement).value)).toEqual(['Level', 'Lang']),
    );
    request.mockClear();
    await pick('Attribute key equals value');
    await userEvent.type(within(dialog()).getByLabelText('Attribute key'), 'L');
    await waitFor(() => expect(request).toHaveBeenCalledWith('GET', '/groups/g%201/attribute-keys?q=L'));
    expect(within(dialog()).getByLabelText('Attribute value')).not.toHaveAttribute('list');
  });

  it('asks for companies for COMPANY, and for nothing else', async () => {
    const { request } = mount(() => ['Acme']);
    await pick('Country');
    await pick('Company');
    expect(request).not.toHaveBeenCalled();
    await userEvent.type(within(dialog()).getByLabelText('Company'), 'Ac');
    await waitFor(() => expect(request).toHaveBeenCalledWith('GET', '/groups/g%201/companies?q=Ac'));
    expect(request.mock.calls.every((c) => String(c[1]).includes('/companies?'))).toBe(true);
  });

  it('does not ask while the text is blank', async () => {
    const { request } = mount();
    await pick('Company');
    await userEvent.type(within(dialog()).getByLabelText('Company'), '  ');
    expect(request).not.toHaveBeenCalled();
  });
});

describe('FilterDialog: opening and closing', () => {
  it('is open at once with its title, intro and the choice of condition, and has not closed', () => {
    const { onClose } = mount();
    expect(screen.getByRole('heading', { name: 'Filter contacts' })).toBeInTheDocument();
    const intro = screen.getByText(/All conditions and the search text must match/);
    expect(dialog().getAttribute('aria-describedby')).toBe(intro.id);
    expect(intro.id).toMatch(/-intro$/);
    expect(within(dialog()).getByLabelText('Condition')).toHaveValue('');
    expect(within(dialog()).getByRole('option', { name: 'Choose …' })).toBeInTheDocument();
    expect(onClose).not.toHaveBeenCalled();
  });

  it('closes on Escape and on Cancel, once each', async () => {
    const { onClose } = mount();
    await userEvent.keyboard('{Escape}');
    expect(onClose).toHaveBeenCalledTimes(1);
    await userEvent.click(within(dialog()).getByRole('button', { name: 'Cancel' }));
    expect(onClose).toHaveBeenCalledTimes(2);
  });

  it('shows the countries by name in the language and lists no other fields for COUNTRY', async () => {
    mount();
    await pick('Country');
    const select = within(dialog()).getByLabelText('Country');
    expect(within(select).getByRole('option', { name: 'Choose …' })).toHaveValue('');
    expect(within(select).getByRole('option', { name: 'Slovenia' })).toHaveValue('SI');
    expect(within(dialog()).queryByLabelText('Company')).toBeNull();
    expect(within(dialog()).queryByLabelText('Tags')).toBeNull();
    expect(within(dialog()).queryByLabelText('Tag')).toBeNull();
    expect(within(dialog()).queryByLabelText('Attribute key')).toBeNull();
  });
});
