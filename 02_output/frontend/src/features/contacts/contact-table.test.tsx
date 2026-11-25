import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { I18nextProvider } from 'react-i18next';
import { MemoryRouter } from 'react-router';
import { setupI18n } from '@/lib/i18n';
import { ContactTable, type Sort } from './ContactTable';

type Row = Parameters<typeof ContactTable>[0]['rows'][number];

const full: Row = {
  id: 'c-1',
  lastName: 'Novak',
  firstName: 'Ana',
  email: 'ana@x.si',
  phone: '+386 1',
  company: 'Acme',
  city: 'Ljubljana',
  country: 'SI',
  tags: ['vip', 'b'],
};

function mount(
  rows: Row[],
  sort?: Sort,
  ticks?: Parameters<typeof ContactTable>[0]['ticks'],
  language = 'en',
) {
  const onSort = vi.fn();
  render(
    <I18nextProvider i18n={setupI18n(language)}>
      <MemoryRouter>
        <ContactTable rows={rows} groupId="g-1" sort={sort} onSort={onSort} ticks={ticks} />
      </MemoryRouter>
    </I18nextProvider>,
  );
  return { onSort };
}

describe('ContactTable headers', () => {
  it('has eight columns in order, a sort button for all but tags, and no tick column without ticks', () => {
    mount([full]);
    const heads = screen.getAllByRole('columnheader');
    expect(heads.map((h) => h.textContent?.replace(/[↕↑↓]/g, ''))).toEqual([
      'Last name',
      'First name',
      'E-mail',
      'Phone',
      'Company',
      'City',
      'Country',
      'Tags',
    ]);
    expect(screen.getAllByRole('button')).toHaveLength(7);
    expect(within(heads[7]!).queryByRole('button')).toBeNull();
    expect(screen.queryByRole('checkbox')).toBeNull();
  });

  it('shows no sort state when unsorted, and ↕ on each sortable column; tags has no aria-sort', () => {
    mount([full]);
    const heads = screen.getAllByRole('columnheader');
    expect(heads.slice(0, 7).map((h) => h.getAttribute('aria-sort'))).toEqual(Array(7).fill('none'));
    expect(heads[7]).not.toHaveAttribute('aria-sort');
    expect(heads.slice(0, 7).map((h) => h.textContent?.slice(-1))).toEqual(Array(7).fill('↕'));
  });

  it('marks the ascending column with ↑ and the descending one with ↓, and no other', () => {
    mount([full], { field: 'email', direction: 'ASC' });
    const heads = screen.getAllByRole('columnheader');
    expect(heads[2]).toHaveAttribute('aria-sort', 'ascending');
    expect(heads[2]!.textContent).toBe('E-mail↑');
    expect(heads[0]).toHaveAttribute('aria-sort', 'none');
    expect(heads[0]!.textContent).toBe('Last name↕');
  });

  it('marks a descending column', () => {
    mount([full], { field: 'company', direction: 'DESC' });
    const head = screen.getAllByRole('columnheader')[4]!;
    expect(head).toHaveAttribute('aria-sort', 'descending');
    expect(head.textContent).toBe('Company↓');
  });

  it('reports the field of the pressed header', async () => {
    const { onSort } = mount([full]);
    await userEvent.click(screen.getByRole('button', { name: /^City/ }));
    expect(onSort).toHaveBeenCalledWith('city');
    await userEvent.click(screen.getByRole('button', { name: /^Last name/ }));
    expect(onSort).toHaveBeenLastCalledWith('lastName');
    expect(onSort).toHaveBeenCalledTimes(2);
  });

  it('hides phone, city and country on tablets, and only those, in header and body', () => {
    mount([full]);
    const heads = screen.getAllByRole('columnheader');
    expect(heads.map((h) => h.className.includes('max-lg:hidden'))).toEqual([
      false,
      false,
      false,
      true,
      false,
      true,
      true,
      false,
    ]);
    const cells = screen.getAllByRole('cell');
    expect(cells.map((c) => c.className.includes('max-lg:hidden'))).toEqual([
      false,
      false,
      false,
      true,
      false,
      true,
      true,
      false,
    ]);
    // Every cell is a flex line on phones, except the first name, which D-80 hides there (own test below).
    expect(cells.map((c) => c.className.includes('max-md:flex'))).toEqual([
      true,
      false,
      true,
      true,
      true,
      true,
      true,
      true,
    ]);
    expect(cells.every((c) => c.className.includes('px-3'))).toBe(true);
    expect(heads.every((h) => h.className.includes('px-3 py-2'))).toBe(true);
  });
});

describe('ContactTable rows', () => {
  it('shows every value with its label for the card layout, the name as a link, the country by name', () => {
    mount([full]);
    const cells = screen.getAllByRole('cell');
    expect(cells.map((c) => c.getAttribute('data-label'))).toEqual([
      'Last name',
      'First name',
      'E-mail',
      'Phone',
      'Company',
      'City',
      'Country',
      'Tags',
    ]);
    expect(within(cells[0]!).getByRole('link', { name: 'Novak' })).toHaveAttribute(
      'href',
      '/groups/g-1/contacts/c-1',
    );
    expect(cells.slice(1, 7).map((c) => c.textContent)).toEqual([
      'Ana',
      'ana@x.si',
      '+386 1',
      'Acme',
      'Ljubljana',
      'Slovenia',
    ]);
    expect(
      within(cells[7]!)
        .getAllByRole('listitem')
        .map((li) => li.textContent),
    ).toEqual(['vip', 'b']);
    expect(screen.getAllByRole('link')).toHaveLength(1);
  });

  it('names the country in the language of the screen', () => {
    mount([full], undefined, undefined, 'sl');
    expect(screen.getAllByRole('cell')[6]).toHaveTextContent('Slovenija');
  });

  it('labels the link with the first name, else the e-mail, else the company when there is no last name', () => {
    mount([
      { id: 'a', firstName: 'Ana', email: 'e@x.si', company: 'C', tags: [] },
      { id: 'b', email: 'e@x.si', company: 'C', tags: [] },
      { id: 'c', company: 'C', tags: [] },
      { id: 'd', lastName: '', tags: [] },
    ]);
    const links = screen.getAllByRole('link');
    expect(links.map((l) => l.textContent)).toEqual(['Ana', 'e@x.si', 'C', '']);
    expect(links.map((l) => l.getAttribute('href'))).toEqual([
      '/groups/g-1/contacts/a',
      '/groups/g-1/contacts/b',
      '/groups/g-1/contacts/c',
      '/groups/g-1/contacts/d',
    ]);
  });

  it('shows empty cells for missing values and no country text for none, and no stray text', () => {
    mount([{ id: 'x', tags: [] }]);
    const cells = screen.getAllByRole('cell');
    expect(cells.map((c) => c.textContent)).toEqual(['', '', '', '', '', '', '', '']);
    expect(within(cells[7]!).queryAllByRole('listitem')).toHaveLength(0);
  });

  it('shows one row per contact in the given order', () => {
    mount([
      { ...full, id: 'a', lastName: 'A' },
      { ...full, id: 'b', lastName: 'B' },
    ]);
    expect(screen.getAllByRole('row')).toHaveLength(3);
    expect(screen.getAllByRole('link').map((l) => l.textContent)).toEqual(['A', 'B']);
  });

  it('D-80: on phones the link is the card title with the full name, the first-name row is hidden', () => {
    mount([full, { id: 'b', firstName: 'Bor', tags: [] }]);
    const [both, onlyFirst] = screen.getAllByRole('link');
    expect(both).toHaveTextContent('Novak');
    expect(both).toHaveAttribute('data-first', 'Ana ');
    expect(both).toHaveClass('max-md:before:content-[attr(data-first)]');
    // Without a last name the link already shows the first name: no second copy before it.
    expect(onlyFirst).toHaveTextContent('Bor');
    expect(onlyFirst).not.toHaveAttribute('data-first');
    const [lastCell, firstCell] = screen.getAllByRole('cell');
    expect(lastCell).toHaveClass('max-md:before:content-none');
    expect(lastCell).not.toHaveClass('max-md:before:content-[attr(data-label)]');
    expect(firstCell).toHaveClass('max-md:hidden');
  });
});

describe('ContactTable tick boxes', () => {
  it('adds a column and a box per row that reflects and reports the ticking', async () => {
    const onTick = vi.fn();
    mount(
      [
        { ...full, id: 'a' },
        { ...full, id: 'b' },
      ],
      undefined,
      { isTicked: (id) => id === 'b', onTick },
    );
    const heads = screen.getAllByRole('columnheader');
    expect(heads).toHaveLength(9);
    expect(heads[0]).toHaveTextContent('Select');
    expect(heads[0]).toHaveAttribute('scope', 'col');
    const boxes = screen.getAllByRole('checkbox', { name: 'Select contact' }) as HTMLInputElement[];
    expect(boxes.map((b) => b.checked)).toEqual([false, true]);
    await userEvent.click(boxes[0]!);
    expect(onTick).toHaveBeenCalledWith('a', true);
    await userEvent.click(boxes[1]!);
    expect(onTick).toHaveBeenLastCalledWith('b', false);
  });
});
