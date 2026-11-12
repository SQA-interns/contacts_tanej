import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ApiError, type ApiClient } from '@/lib/api/client';
import { groupsApi, konferenca, skupno, signedIn } from '@/test/groupsApi';
import { renderApp } from '@/test/renderApp';
import type { Contact } from '@/features/contacts/contactValues';
import { attributeConflicts, byAge, combinedTags, preselect } from './MergePage';

const contact = (id: string, createdAt: string, extra: Partial<Contact> = {}): Contact =>
  ({
    id,
    state: 'ACTIVE',
    createdAt,
    revision: 3,
    versionNumber: 1,
    lastName: 'Novak',
    tags: [],
    attributes: [],
    ...extra,
  }) as Contact;

const a = contact('a', '2026-06-01T10:00:00Z', {
  company: 'Acme',
  tags: ['a', 'Shared'],
  attributes: [{ key: 'kompetenca', value: 'java' }],
});
const b = contact('b', '2026-06-02T10:00:00Z', {
  phone: '+386 41 555 123',
  tags: ['shared', 'b'],
  attributes: [{ key: 'Kompetenca', value: 'python' }],
});

describe('merge rules of the screen (R-022-02, R-022-03, R-022-04, EX-022-03)', () => {
  it('puts the oldest contact first and the lower id first on the same time', () => {
    expect(byAge([b, a]).map((c) => c.id)).toEqual(['a', 'b']);
    const same = contact('c', a.createdAt);
    expect(byAge([same, a]).map((c) => c.id)).toEqual(['a', 'c']);
  });

  it('preselects the first non-empty value, the oldest contact first', () => {
    const picked = preselect([a, b]);
    expect(picked.company).toBe(0);
    expect(picked.phone).toBe(1);
    expect(picked.notes).toBe(0);
  });

  it('combines the tags once ignoring the case', () => {
    expect(combinedTags([a, b])).toEqual(['a', 'Shared', 'b']);
  });

  it('finds the keys with different values only', () => {
    expect(attributeConflicts([a, b])).toEqual([{ key: 'kompetenca', values: ['java', 'python'] }]);
    const same = contact('c', a.createdAt, { attributes: [{ key: 'KOMPETENCA', value: 'java' }] });
    expect(attributeConflicts([a, same])).toEqual([]);
  });
});

function mergeApi(merge?: () => Promise<unknown>) {
  const { api: groups } = groupsApi([konferenca, skupno]);
  const calls: { method: string; path: string; body?: unknown }[] = [];
  const api: ApiClient = {
    ...groups,
    request: (async (method: string, path: string, body?: unknown) => {
      if (path.endsWith('/contacts/a')) return a;
      if (path.endsWith('/contacts/b')) return b;
      if (path.endsWith('/merges')) {
        calls.push({ method, path, body });
        return merge ? merge() : { ...a, phone: '+386 41 555 123' };
      }
      return groups.request(method, path, body);
    }) as ApiClient['request'],
  };
  return { api, calls };
}

const open = (merge?: () => Promise<unknown>, group = 'g-1') => {
  const fake = mergeApi(merge);
  renderApp(`/groups/${group}/duplicates/merge?ids=b,a`, { auth: signedIn(), api: fake.api });
  return fake;
};

describe('Merge screen (WF-17)', () => {
  it('shows the contacts, the revert hint and blocks Confirm until a conflict is chosen', async () => {
    open();
    expect(await screen.findByRole('heading', { name: 'Merge contacts' })).toBeInTheDocument();
    expect(await screen.findByText('Survivor (the oldest, stays)')).toBeInTheDocument();
    expect(screen.getByText(/You can revert a merge later/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Confirm merge' })).toBeDisabled();
    await userEvent.click(screen.getByRole('radio', { name: 'python' }));
    expect(screen.getByRole('button', { name: 'Confirm merge' })).toBeEnabled();
  });

  it('sends the revisions, the chosen values and the attribute choice on Confirm', async () => {
    const { calls } = open();
    await userEvent.click(await screen.findByRole('radio', { name: 'python' }));
    await userEvent.click(screen.getByRole('button', { name: 'Confirm merge' }));
    await screen.findByRole('heading', { level: 1, name: 'Konferenca 2026' });
    const body = calls[0]?.body as {
      contacts: { id: string; revision: number }[];
      fields: Record<string, string>;
      attributeChoices: { key: string; value: string }[];
    };
    expect(body.contacts).toEqual([
      { id: 'a', revision: 3 },
      { id: 'b', revision: 3 },
    ]);
    expect(body.fields.phone).toBe('+386 41 555 123');
    expect(body.fields.company).toBe('Acme');
    expect(body.attributeChoices).toEqual([{ key: 'kompetenca', value: 'python' }]);
  });

  it('sends nothing on Cancel (R-022-14)', async () => {
    const { calls } = open();
    await userEvent.click(await screen.findByRole('button', { name: 'Cancel' }));
    expect(calls).toHaveLength(0);
  });

  it('names the tag limit when the server refuses (R-022-09)', async () => {
    open(async () => {
      throw new ApiError(400, {
        status: 400,
        code: 'VALIDATION_FAILED',
        message: 'x',
        errors: [{ field: 'tags', code: 'TOO_MANY', message: 'x' }],
      });
    });
    await userEvent.click(await screen.findByRole('radio', { name: 'python' }));
    await userEvent.click(screen.getByRole('button', { name: 'Confirm merge' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('A contact has at most 50 tags.');
  });

  it('shows a Viewer no merge screen (R-022-13)', async () => {
    open(undefined, 'g-3');
    expect(await screen.findByRole('heading', { level: 1, name: 'Skupno' })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Merge contacts' })).not.toBeInTheDocument();
  });
});
