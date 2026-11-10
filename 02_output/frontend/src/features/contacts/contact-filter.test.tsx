import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { groupsApi, konferenca, signedIn, type ContactRow } from '@/test/groupsApi';
import { renderApp } from '@/test/renderApp';

const ana: ContactRow = {
  id: 'c-1',
  lastName: 'Novak',
  firstName: 'Ana',
  email: '',
  phone: '',
  company: '',
  city: '',
  country: '',
  tags: [],
};

async function openDialog() {
  await userEvent.click(await screen.findByRole('button', { name: 'Filter' }));
  return screen.findByRole('dialog');
}

async function choose(dialog: HTMLElement, condition: string) {
  await userEvent.selectOptions(within(dialog).getByLabelText('Condition'), condition);
}

describe('the filter dialog (R-010-01 to R-010-07, R-010-09)', () => {
  it('offers exactly the seven conditions and no way to save a filter', async () => {
    const { api } = groupsApi([konferenca], { contacts: [ana] });
    renderApp('/groups/g-1', { auth: signedIn(), api });
    const dialog = await openDialog();
    const options = within(within(dialog).getByLabelText('Condition')).getAllByRole('option');
    expect(options.map((o) => o.textContent)).toEqual([
      'Choose …',
      'Has all of these tags',
      'Has any of these tags',
      'Does not have tag',
      'Attribute key equals value',
      'Attribute key exists',
      'Country',
      'Company',
    ]);
    expect(within(dialog).queryByRole('button', { name: /save/i })).toBeNull();
  });

  it('shows only the fields of the chosen condition', async () => {
    const { api } = groupsApi([konferenca], { contacts: [ana] });
    renderApp('/groups/g-1', { auth: signedIn(), api });
    const dialog = await openDialog();
    await choose(dialog, 'Attribute key equals value');
    expect(within(dialog).getByLabelText('Attribute key')).toBeInTheDocument();
    expect(within(dialog).getByLabelText('Attribute value')).toBeInTheDocument();
    expect(within(dialog).queryByLabelText('Tags')).toBeNull();
    await choose(dialog, 'Attribute key exists');
    expect(within(dialog).queryByLabelText('Attribute value')).toBeNull();
  });

  it('refuses an empty condition and keeps the dialog open', async () => {
    const { api, calls } = groupsApi([konferenca], { contacts: [ana] });
    renderApp('/groups/g-1', { auth: signedIn(), api });
    const dialog = await openDialog();
    await userEvent.click(within(dialog).getByRole('button', { name: 'Add condition' }));
    expect(await within(dialog).findByRole('alert')).toHaveTextContent('Choose a condition.');
    await choose(dialog, 'Attribute key exists');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Add condition' }));
    expect(await within(dialog).findByRole('alert')).toHaveTextContent('Enter a key.');
    expect(calls.filter((c) => c.includes('"conditions":[{'))).toHaveLength(0);
  });

  it('sends the tags typed, including the one not yet confirmed with Enter', async () => {
    const { api, calls } = groupsApi([konferenca], { contacts: [ana] });
    renderApp('/groups/g-1', { auth: signedIn(), api });
    const dialog = await openDialog();
    await choose(dialog, 'Has any of these tags');
    await userEvent.type(within(dialog).getByLabelText('Tags'), 'a{Enter}b');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Add condition' }));
    await waitFor(() =>
      expect(calls.at(-1)).toContain('"conditions":[{"type":"TAGS_ANY","tags":["a","b"]}]'),
    );
    expect(await screen.findByText('Has any of: a, b')).toBeInTheDocument();
  });
});

describe('active conditions (R-010-08, R-010-10, R-010-11)', () => {
  it('shows each condition as a chip that can be removed, and keeps the search text', async () => {
    const { api, calls } = groupsApi([konferenca], { contacts: [ana] });
    renderApp('/groups/g-1', { auth: signedIn(), api });
    await userEvent.type(await screen.findByLabelText('Search'), 'novak');
    let dialog = await openDialog();
    await choose(dialog, 'Does not have tag');
    await userEvent.type(within(dialog).getByLabelText('Tag'), 'neObvescaj');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Add condition' }));
    dialog = await openDialog();
    await choose(dialog, 'Country');
    await userEvent.selectOptions(within(dialog).getByLabelText('Country'), 'Slovenia');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Add condition' }));
    expect(await screen.findByText('Filters:')).toBeInTheDocument();
    expect(screen.getByText('Does not have tag: neObvescaj')).toBeInTheDocument();
    expect(screen.getByText('Country: Slovenia')).toBeInTheDocument();
    await waitFor(() => {
      expect(calls.at(-1)).toContain('"q":"novak"');
      expect(calls.at(-1)).toContain('"conditions":[{"type":"TAG_NOT","tag":"neObvescaj"}');
    });
    await userEvent.click(screen.getAllByRole('button', { name: 'Remove condition' }).at(0) as HTMLElement);
    expect(screen.queryByText('Does not have tag: neObvescaj')).toBeNull();
    await waitFor(() => expect(calls.at(-1)).toContain('"conditions":[{"type":"COUNTRY","country":"SI"}]'));
    expect(screen.getByLabelText('Search')).toHaveValue('novak');
  });

  it('goes back to page 1 when a condition is added', async () => {
    const many = Array.from({ length: 60 }, (_, i) => ({ ...ana, id: `c-${i}`, lastName: `P${i}` }));
    const { api, calls } = groupsApi([konferenca], { contacts: many });
    renderApp('/groups/g-1', { auth: signedIn(), api });
    await userEvent.click(await screen.findByRole('button', { name: '2' }));
    await waitFor(() => expect(calls.at(-1)).toContain('"page":2'));
    const dialog = await openDialog();
    await choose(dialog, 'Company');
    await userEvent.type(within(dialog).getByLabelText('Company'), 'Acme');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Add condition' }));
    await waitFor(() => expect(calls.at(-1)).toContain('"page":1'));
  });

  it('shows "No contacts match" for a filter without result and clears search and conditions', async () => {
    const { api, calls } = groupsApi([konferenca], { contacts: [] });
    renderApp('/groups/g-1', { auth: signedIn(), api });
    const dialog = await openDialog();
    await choose(dialog, 'Has all of these tags');
    await userEvent.type(within(dialog).getByLabelText('Tags'), 'nobody');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Add condition' }));
    expect(await screen.findByRole('heading', { name: 'No contacts match' })).toBeInTheDocument();
    await userEvent.click(screen.getAllByRole('button', { name: 'Clear filters' }).at(0) as HTMLElement);
    await waitFor(() => expect(calls.at(-1)).toContain('"conditions":[]'));
    expect(screen.queryByText('Filters:')).toBeNull();
    expect(await screen.findByRole('heading', { name: 'No contacts yet' })).toBeInTheDocument();
  });

  it('"Clear all" removes every chip at once and leaves "Filter" a single button', async () => {
    const { api, calls } = groupsApi([konferenca], { contacts: [ana] });
    renderApp('/groups/g-1', { auth: signedIn(), api });
    const dialog = await openDialog();
    await choose(dialog, 'Company');
    await userEvent.type(within(dialog).getByLabelText('Company'), 'Acme');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Add condition' }));
    await screen.findByText('Company: Acme');
    expect(screen.getAllByRole('button', { name: /filter/i })).toHaveLength(1);
    await userEvent.click(screen.getByRole('button', { name: 'Clear all' }));
    await waitFor(() => expect(calls.at(-1)).toContain('"conditions":[]'));
    expect(screen.queryByText('Company: Acme')).toBeNull();
  });
});
