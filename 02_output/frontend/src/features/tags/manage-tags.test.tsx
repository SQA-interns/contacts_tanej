import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ApiError, type ApiClient } from '@/lib/api/client';
import type { components } from '@/lib/api/schema';
import { failure, groupsApi, konferenca, poroka, skupno, signedIn } from '@/test/groupsApi';
import { renderApp } from '@/test/renderApp';

type Tag = components['schemas']['Tag'];

/** The groups stand-in plus the tags of group g-1 and the way a rename or delete is answered. */
function tagsApi(tags: Tag[], refuse?: ApiError) {
  const { api: groups, calls } = groupsApi([konferenca, poroka, skupno]);
  const list = tags.map((t) => ({ ...t }));
  const api: ApiClient = {
    ...groups,
    request: (async (method: string, path: string, body?: { name: string }) => {
      const one = /^\/groups\/g-[13]\/tags\/(.+)$/.exec(path);
      if (path.startsWith('/groups/g-1/tags') || path.startsWith('/groups/g-3/tags')) {
        calls.push(`${method} ${path} ${body ? JSON.stringify(body) : ''}`.trim());
      }
      if (path.endsWith('/tags')) return list.map((t) => ({ ...t }));
      if (one && refuse) throw refuse;
      if (one && method === 'PATCH') {
        const tag = list.find((t) => t.id === one[1]);
        if (tag && body) tag.name = body.name;
        return tag;
      }
      if (one && method === 'DELETE') {
        list.splice(
          list.findIndex((t) => t.id === one[1]),
          1,
        );
        return undefined;
      }
      return groups.request(method, path, body);
    }) as ApiClient['request'],
  };
  return { api, calls };
}

const old: Tag = { id: 't-1', name: 'udelezenec2205', contactCount: 3 };

describe('Manage tags (R-007-01, R-007-05, R-007-07)', () => {
  it('lists the tags with their contact counts and gives an Owner rename and delete', async () => {
    const { api } = tagsApi([old, { id: 't-2', name: 'gost', contactCount: 1 }]);
    renderApp('/groups/g-1/tags', { auth: signedIn(), api });
    const row = await screen.findByRole('row', { name: /udelezenec2205/ });
    expect(within(row).getByText('3')).toBeInTheDocument();
    expect(within(row).getByRole('button', { name: 'Rename' })).toBeInTheDocument();
    expect(within(row).getByRole('button', { name: 'Delete' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Tags' })).toBeInTheDocument();
  });

  it('shows a Viewer neither the Tags tab nor a control', async () => {
    const { api } = tagsApi([old]);
    renderApp('/groups/g-3/tags', { auth: signedIn(), api });
    await screen.findByRole('row', { name: /udelezenec2205/ });
    expect(screen.queryByRole('link', { name: 'Tags' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Rename' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Delete' })).not.toBeInTheDocument();
  });

  it('renames a tag and shows the new name', async () => {
    const { api, calls } = tagsApi([old]);
    renderApp('/groups/g-1/tags', { auth: signedIn(), api });
    await userEvent.click(await screen.findByRole('button', { name: 'Rename' }));
    const dialog = screen.getByRole('dialog');
    const field = within(dialog).getByLabelText(/Tag name/);
    await userEvent.clear(field);
    await userEvent.type(field, 'udelezenec2025');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Rename' }));
    expect(await screen.findByRole('row', { name: /udelezenec2025/ })).toBeInTheDocument();
    expect(calls).toContain('PATCH /groups/g-1/tags/t-1 {"name":"udelezenec2025"}');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('keeps the dialog and the typed name when the name is taken (UI-13)', async () => {
    const { api } = tagsApi([old], failure(409, 'TAG_NAME_TAKEN'));
    renderApp('/groups/g-1/tags', { auth: signedIn(), api });
    await userEvent.click(await screen.findByRole('button', { name: 'Rename' }));
    const dialog = screen.getByRole('dialog');
    const field = within(dialog).getByLabelText(/Tag name/);
    await userEvent.clear(field);
    await userEvent.type(field, 'GOST');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Rename' }));
    expect(await within(dialog).findByText('A tag with this name already exists.')).toBeInTheDocument();
    expect(field).toHaveValue('GOST');
  });

  it('shows the reason for a name with a space', async () => {
    const problem = new ApiError(400, {
      status: 400,
      code: 'VALIDATION_FAILED',
      message: 'x',
      errors: [{ field: 'name', code: 'INVALID_CHARACTERS', message: 'x' }],
    });
    const { api } = tagsApi([old], problem);
    renderApp('/groups/g-1/tags', { auth: signedIn(), api });
    await userEvent.click(await screen.findByRole('button', { name: 'Rename' }));
    const dialog = screen.getByRole('dialog');
    await userEvent.type(within(dialog).getByLabelText(/Tag name/), ' x');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Rename' }));
    expect(await within(dialog).findByText('Tag names use letters, digits, - and _.')).toBeInTheDocument();
  });

  it('deletes only after the second click and lists no tag afterwards', async () => {
    const { api, calls } = tagsApi([old]);
    renderApp('/groups/g-1/tags', { auth: signedIn(), api });
    await userEvent.click(await screen.findByRole('button', { name: 'Delete' }));
    expect(calls.some((c) => c.startsWith('DELETE'))).toBe(false);
    const dialog = screen.getByRole('dialog');
    expect(
      within(dialog).getByText('The tag is removed from its contacts. The contacts stay.'),
    ).toBeInTheDocument();
    await userEvent.click(within(dialog).getByRole('button', { name: 'Delete tag' }));
    expect(await screen.findByText(/No tags yet/)).toBeInTheDocument();
    expect(calls).toContain('DELETE /groups/g-1/tags/t-1');
  });
});
