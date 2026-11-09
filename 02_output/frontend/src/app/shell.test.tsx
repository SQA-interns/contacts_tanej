import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { I18nextProvider } from 'react-i18next';
import { createMemoryRouter, RouterProvider } from 'react-router';
import { initialLanguage, setupI18n } from '@/lib/i18n';
import { loadConfig } from '@/lib/config';
import { routes } from './routes';

function renderAt(path: string, stored: string | null = null) {
  const i18n = setupI18n(stored);
  render(
    <I18nextProvider i18n={i18n}>
      <RouterProvider router={createMemoryRouter(routes, { initialEntries: [path] })} />
    </I18nextProvider>,
  );
}

describe('web app shell', () => {
  it('shows the Slovenian UI by default (R-018-01)', () => {
    renderAt('/');
    expect(screen.getByRole('heading', { level: 1, name: 'Dobrodošli' })).toBeInTheDocument();
    expect(screen.getByRole('navigation')).toBeInTheDocument();
  });

  it('switches to English at once and remembers the choice (R-018-02, R-018-03)', async () => {
    renderAt('/');
    await userEvent.click(screen.getByRole('button', { name: 'EN' }));
    expect(screen.getByRole('heading', { level: 1, name: 'Welcome' })).toBeInTheDocument();
    expect(localStorage.getItem('language')).toBe('en');
  });

  it('shows "not found" for an unknown address', () => {
    renderAt('/nope');
    expect(screen.getByRole('heading', { level: 1, name: 'Ni najdeno' })).toBeInTheDocument();
  });

  it('falls back to Slovenian for an unknown stored language (EX-018-01)', () => {
    expect(initialLanguage('fr')).toBe('sl');
    expect(initialLanguage(null)).toBe('sl');
    expect(initialLanguage('en')).toBe('en');
  });

  it('refuses a configuration with a missing value (NFR-12)', () => {
    expect(() => loadConfig({ firebaseProjectId: 'p' })).toThrow(/firebaseApiKey/);
    const config = loadConfig({
      firebaseProjectId: 'p',
      firebaseApiKey: 'k',
      firebaseAuthDomain: 'd',
      apiBaseUrl: '/api',
    });
    expect(config.firebaseAuthEmulatorUrl).toBe('');
  });
});
