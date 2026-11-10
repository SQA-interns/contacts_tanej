import { screen, within } from '@testing-library/react';
import { renderApp } from '@/test/renderApp';
import { groupsApi, konferenca, poroka, signedIn } from '@/test/groupsApi';

/**
 * F-02, NFR-08 (UI-22, WF-05 mobile): below 768 px a table of rows becomes one card per row with
 * a label before each value, and the tab row of a group wraps instead of scrolling the page.
 * jsdom has no layout, so the test checks the classes and labels that produce it; the width itself
 * is checked by `e2e/responsive.spec.ts`.
 */
describe('mobile layout of lists (F-02)', () => {
  it('turns the groups table into cards with a label before the role', async () => {
    const { api } = groupsApi([konferenca, poroka]);
    renderApp('/', { auth: signedIn(), api });
    const row = (await screen.findByRole('link', { name: 'Konferenca 2026' })).closest('tr') as HTMLElement;
    expect(row.className).toContain('max-md:block');
    expect(row.closest('table')?.className).toContain('max-md:block');
    expect(within(row).getByText('Owner').closest('td')).toHaveAttribute('data-label', 'Role');
  });

  it('lets the tab row of a group wrap', async () => {
    const { api } = groupsApi([konferenca]);
    renderApp(`/groups/${konferenca.id}`, { auth: signedIn(), api });
    const nav = await screen.findByRole('navigation', { name: 'Group' });
    expect(nav.className).toContain('flex-wrap');
  });
});
