import { screen } from '@testing-library/react';
import type { ApiClient } from '@/lib/api/client';
import { renderApp } from '@/test/renderApp';
import { failure, groupsApi, konferenca, poroka, signedIn } from '@/test/groupsApi';

describe('open a group (R-002-06, R-002-07, AC-002-03)', () => {
  it('shows the name, the role and the empty group of a member', async () => {
    const { api } = groupsApi([poroka]);
    renderApp('/groups/g-2', { auth: signedIn(), api });
    expect(await screen.findByRole('heading', { level: 1, name: 'Poroka' })).toBeInTheDocument();
    expect(screen.getByText('Editor')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'No contacts yet' })).toBeInTheDocument();
    expect(screen.getByRole('navigation', { name: 'Group' })).toBeInTheDocument();
  });

  it('shows "Group not found" and nothing else for a group that is not available', async () => {
    const { api } = groupsApi([konferenca]);
    renderApp('/groups/g-77', { auth: signedIn(), api });
    expect(await screen.findByRole('heading', { level: 1, name: 'Group not found' })).toBeInTheDocument();
    expect(screen.getByText('This group does not exist or you are not a member of it.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Back to groups' })).toHaveAttribute('href', '/');
    expect(screen.queryByText('Konferenca 2026')).toBeNull();
  });

  it('shows the error state with "Try again" when the group cannot be loaded', async () => {
    const { api } = groupsApi();
    const down: ApiClient = {
      ...api,
      request: (async () => {
        throw failure(503, 'UNAVAILABLE');
      }) as ApiClient['request'],
    };
    renderApp('/groups/g-2', { auth: signedIn(), api: down });
    expect(await screen.findByRole('alert')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument();
  });
});
