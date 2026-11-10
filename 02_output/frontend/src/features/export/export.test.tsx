import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ApiError } from '@/lib/api/client';
import { groupsApi, konferenca, skupno, signedIn, type ContactRow } from '@/test/groupsApi';
import { renderApp } from '@/test/renderApp';

function contact(i: number): ContactRow {
  return {
    id: `c-${i}`,
    lastName: `P${String(i).padStart(2, '0')}`,
    firstName: 'Ime',
    email: '',
    phone: '',
    company: '',
    city: '',
    country: '',
    tags: [],
  };
}

const many = (n: number) => Array.from({ length: n }, (_, i) => contact(i));

/** The groups stand-in whose export answers are recorded (R-011-02, R-011-03, R-011-09, R-011-10). */
function exportApi(contacts: ContactRow[], fail?: ApiError) {
  const { api, calls } = groupsApi([konferenca, skupno], { contacts });
  const downloads: { path: string; body: unknown }[] = [];
  api.download = async (path, body) => {
    downloads.push({ path, body });
    if (fail) throw fail;
    return { blob: new Blob(['x']), fileName: 'Konferenca 2026_2026-11-09.xlsx' };
  };
  return { api, calls, downloads };
}

/** The i-th tick box of the page; the list has at least that many. */
function tick(boxes: HTMLElement[], i: number): HTMLElement {
  const box = boxes[i];
  if (!box) throw new Error(`no tick box ${i}`);
  return box;
}

describe('select and export (US-011)', () => {
  beforeEach(() => {
    URL.createObjectURL = vi.fn(() => 'blob:x');
    URL.revokeObjectURL = vi.fn();
  });

  it('keeps Export disabled while nothing is ticked and counts the ticks over pages', async () => {
    const { api } = exportApi(many(60));
    renderApp('/groups/g-1', { auth: signedIn(), api });
    await screen.findByText('60 contacts');
    expect(screen.getByRole('button', { name: 'Export' })).toBeDisabled();
    expect(screen.getByText('Nothing selected')).toBeInTheDocument();
    const boxes = screen.getAllByRole('checkbox', { name: 'Select contact' });
    await userEvent.click(tick(boxes, 0));
    await userEvent.click(tick(boxes, 1));
    expect(screen.getByText('2 selected')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Export' })).toBeEnabled();
    await userEvent.click(screen.getByRole('button', { name: '2' }));
    await userEvent.click(tick(await screen.findAllByRole('checkbox', { name: 'Select contact' }), 0));
    expect(screen.getByText('3 selected')).toBeInTheDocument();
  });

  it('clears the selection when the search text changes', async () => {
    const { api } = exportApi(many(5));
    renderApp('/groups/g-1', { auth: signedIn(), api });
    await screen.findByText('5 contacts');
    await userEvent.click(screen.getByLabelText('Select all on this page'));
    expect(screen.getByText('5 selected')).toBeInTheDocument();
    await userEvent.type(screen.getByLabelText('Search'), 'p');
    expect(screen.getByText('Nothing selected')).toBeInTheDocument();
  });

  it('exports the ticked ids in the language of the screen', async () => {
    const { api, downloads } = exportApi(many(3));
    renderApp('/groups/g-1', { auth: signedIn(), api });
    await screen.findByText('3 contacts');
    await userEvent.click(tick(screen.getAllByRole('checkbox', { name: 'Select contact' }), 1));
    await userEvent.click(screen.getByRole('button', { name: 'Export' }));
    expect(await screen.findByText('The Excel file was downloaded.')).toBeInTheDocument();
    expect(downloads).toEqual([
      {
        path: '/groups/g-1/exports',
        body: { language: 'en', sort: undefined, contactIds: ['c-1'] },
      },
    ]);
  });

  it('exports the current filter for "select all N results"', async () => {
    const { api, downloads } = exportApi(many(60));
    renderApp('/groups/g-1', { auth: signedIn(), api });
    await screen.findByText('60 contacts');
    await userEvent.click(screen.getByRole('button', { name: 'Select all 60 results' }));
    expect(screen.getByText('60 selected')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Export' }));
    await waitFor(() => expect(downloads).toHaveLength(1));
    expect(downloads[0]?.body).toMatchObject({ filter: { q: '', conditions: [] } });
    expect(downloads[0]?.body).not.toHaveProperty('contactIds');
  });

  it('shows the refusal for more than 10,000 contacts and no success text', async () => {
    const refused = new ApiError(400, {
      status: 400,
      code: 'VALIDATION_FAILED',
      message: 'Too many.',
      errors: [{ field: 'contacts', code: 'TOO_MANY_CONTACTS', message: 'Too many' }],
    });
    const { api } = exportApi(many(2), refused);
    renderApp('/groups/g-1', { auth: signedIn(), api });
    await screen.findByText('2 contacts');
    await userEvent.click(screen.getByLabelText('Select all on this page'));
    await userEvent.click(screen.getByRole('button', { name: 'Export' }));
    expect(
      await screen.findByText('You selected more than 10,000 contacts. Narrow the selection to export.'),
    ).toBeInTheDocument();
    expect(screen.queryByText('The Excel file was downloaded.')).toBeNull();
  });

  it('shows another failure as a general error', async () => {
    const failed = new ApiError(500, { status: 500, code: 'INTERNAL_ERROR', message: 'Oops' });
    const { api } = exportApi(many(1), failed);
    renderApp('/groups/g-1', { auth: signedIn(), api });
    await screen.findByText('1 contact');
    await userEvent.click(screen.getByLabelText('Select all on this page'));
    await userEvent.click(screen.getByRole('button', { name: 'Export' }));
    expect(await screen.findByText('The export did not work. Please try again.')).toBeInTheDocument();
  });

  it('shows a Viewer no tick boxes and no Export button', async () => {
    const { api } = exportApi(many(2));
    renderApp('/groups/g-3', { auth: signedIn(), api });
    await screen.findByText('2 contacts');
    expect(screen.queryAllByRole('checkbox')).toHaveLength(0);
    expect(screen.queryByRole('button', { name: 'Export' })).toBeNull();
  });
});
