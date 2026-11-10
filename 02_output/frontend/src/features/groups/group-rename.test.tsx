import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderApp } from '@/test/renderApp';
import { groupsApi, konferenca, poroka, signedIn, skupno } from '@/test/groupsApi';

describe('rename group dialog (R-002-08, AC-002-08, AC-002-09)', () => {
  async function openRename() {
    await userEvent.click(await screen.findByRole('button', { name: 'Rename' }));
    return screen.getByRole('dialog', { name: 'Rename group' });
  }

  it('offers Rename to an Owner only (R-002-08)', async () => {
    const { api } = groupsApi([konferenca, poroka, skupno]);
    renderApp('/', { auth: signedIn(), api });
    await screen.findByRole('link', { name: 'Konferenca 2026' });
    expect(screen.getAllByRole('button', { name: 'Rename' })).toHaveLength(1);
  });

  it('starts with the current name and shows the new name in the list', async () => {
    const { api, calls } = groupsApi([konferenca]);
    renderApp('/', { auth: signedIn(), api });
    const dialog = await openRename();
    const input = within(dialog).getByLabelText(/^Group name/);
    expect(input).toHaveValue('Konferenca 2026');
    await userEvent.clear(input);
    await userEvent.type(input, 'KONFERENCA 2026');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Rename group' }));
    expect(await screen.findByRole('link', { name: 'KONFERENCA 2026' })).toBeInTheDocument();
    expect(calls).toContain('PATCH /groups/g-1 {"name":"KONFERENCA 2026"}');
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('keeps the name and shows the reason when another own group has the name', async () => {
    const { api } = groupsApi([konferenca, { id: 'g-9', name: 'Poroka', role: 'OWNER' }]);
    renderApp('/', { auth: signedIn(), api });
    await userEvent.click((await screen.findAllByRole('button', { name: 'Rename' }))[1] as HTMLElement);
    const dialog = screen.getByRole('dialog', { name: 'Rename group' });
    const input = within(dialog).getByLabelText(/^Group name/);
    await userEvent.clear(input);
    await userEvent.type(input, 'konferenca 2026');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Rename group' }));
    expect(await within(dialog).findByText('You already have a group with this name.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Poroka', hidden: true })).toBeInTheDocument();
  });

  it('refuses an empty name before the server is asked', async () => {
    const { api, calls } = groupsApi([konferenca]);
    renderApp('/', { auth: signedIn(), api });
    const dialog = await openRename();
    await userEvent.clear(within(dialog).getByLabelText(/^Group name/));
    await userEvent.click(within(dialog).getByRole('button', { name: 'Rename group' }));
    expect(await within(dialog).findByRole('alert')).toHaveTextContent('Enter a group name.');
    expect(calls.some((c) => c.startsWith('PATCH'))).toBe(false);
  });
});
