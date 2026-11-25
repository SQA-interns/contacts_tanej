import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ApiClient } from '@/lib/api/client';
import { groupsApi, konferenca, signedIn, skupno } from '@/test/groupsApi';
import { renderApp } from '@/test/renderApp';
import { sampleContacts } from './sampleContacts';

describe('the sample contacts (demo data)', () => {
  const contacts = sampleContacts();

  it('are fifty and the same on every call', () => {
    expect(contacts).toHaveLength(50);
    expect(sampleContacts()).toEqual(contacts);
  });

  it('keep the field rules', () => {
    for (const c of contacts) {
      expect(c.firstName || c.lastName || c.email || c.company).toBeTruthy(); // BR-05
      if (c.email) expect(c.email).toMatch(/^[a-z0-9.]+@example\.com$/);
      if (c.phone) expect(c.phone).toMatch(/^[0-9 +()\-./]{1,30}$/);
      if (c.country) expect(c.country).toMatch(/^[A-Z]{2}$/);
      for (const tag of c.tags ?? []) expect(tag).toMatch(/^[\p{L}\p{N}_-]{1,50}$/u);
      const keys = (c.attributes ?? []).map((a) => a.key.toLowerCase());
      expect(new Set(keys).size).toBe(keys.length);
    }
  });

  it('hold exactly the three deliberate duplicate pairs', () => {
    const emails = contacts.map((c) => c.email).filter(Boolean);
    expect(emails.length - new Set(emails).size).toBe(1);
    const names = contacts.map((c) => `${c.firstName} ${c.lastName}`);
    const repeated = names.filter((n, i) => names.indexOf(n) !== i);
    expect(repeated.sort()).toEqual(['Irena Lah', 'Simon Pirc']);
  });
});

function recording(base: ApiClient) {
  const created: unknown[] = [];
  const api: ApiClient = {
    ...base,
    request: (async (method: string, path: string, body?: unknown) => {
      if (method === 'POST' && path === '/groups/g-1/contacts') {
        created.push(body);
        return { id: `c-${created.length}` };
      }
      return base.request(method, path, body as never);
    }) as ApiClient['request'],
  };
  return { api, created };
}

describe('the "Fill with test data" button', () => {
  it('is not shown without the demo switch', async () => {
    const { api } = groupsApi([konferenca]);
    renderApp('/groups/g-1', { auth: signedIn(), api });
    expect(await screen.findByRole('button', { name: /add contact/i })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Fill with test data' })).not.toBeInTheDocument();
  });

  it('is not shown to a Viewer, even with the demo switch', async () => {
    const { api } = groupsApi([skupno]);
    renderApp('/groups/g-3', { auth: signedIn(), api, demoTools: true });
    expect(await screen.findByRole('searchbox')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Fill with test data' })).not.toBeInTheDocument();
  });

  it('adds the fifty contacts after the confirmation', async () => {
    const spy = recording(groupsApi([konferenca]).api);
    renderApp('/groups/g-1', { auth: signedIn(), api: spy.api, demoTools: true });
    await userEvent.click(await screen.findByRole('button', { name: 'Fill with test data' }));
    expect(spy.created).toHaveLength(0);
    await userEvent.click(await screen.findByRole('button', { name: 'Add test contacts' }));
    await waitFor(() => expect(spy.created).toHaveLength(50));
    expect(await screen.findByText('50 test contacts added.')).toBeInTheDocument();
  });
});
