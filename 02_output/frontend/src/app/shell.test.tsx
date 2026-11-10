import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { initialLanguage } from '@/lib/i18n';
import { loadConfig } from '@/lib/config';
import { ANA, createFakeAuth } from '@/test/fakeAuth';
import { renderApp } from '@/test/renderApp';

describe('web app shell', () => {
  it('shows the Slovenian UI by default (R-018-01)', async () => {
    renderApp('/', { language: '' });
    expect(await screen.findByRole('heading', { level: 1, name: 'Prijava' })).toBeInTheDocument();
  });

  it('switches to English at once and remembers the choice (R-018-02, R-018-03)', async () => {
    renderApp('/', { language: '' });
    await userEvent.click(await screen.findByRole('button', { name: 'EN' }));
    expect(screen.getByRole('heading', { level: 1, name: 'Sign in' })).toBeInTheDocument();
    expect(localStorage.getItem('language')).toBe('en');
  });

  it('shows the navigation to a verified person', async () => {
    renderApp('/', { auth: createFakeAuth(ANA) });
    expect(await screen.findByRole('navigation')).toBeInTheDocument();
  });

  it('shows "not found" for an unknown address', async () => {
    renderApp('/nope', { auth: createFakeAuth(ANA) });
    expect(await screen.findByRole('heading', { level: 1, name: 'Not found' })).toBeInTheDocument();
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
