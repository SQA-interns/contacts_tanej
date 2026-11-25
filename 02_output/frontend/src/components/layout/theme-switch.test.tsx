import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { I18nextProvider } from 'react-i18next';
import { setupI18n } from '@/lib/i18n';
import { THEME_STORAGE_KEY } from '@/lib/theme';
import { ThemeSwitch } from './ThemeSwitch';

function show(language = 'en') {
  render(
    <I18nextProvider i18n={setupI18n(language)}>
      <ThemeSwitch />
    </I18nextProvider>,
  );
}

describe('the theme switch (post-run)', () => {
  beforeEach(() => {
    localStorage.clear();
    delete document.documentElement.dataset.mode;
  });

  it('follows the system until the user chooses', () => {
    show();
    expect(screen.getByRole('button', { name: 'Appearance: system' })).toBeInTheDocument();
    expect(document.documentElement.dataset.mode).toBeUndefined();
  });

  it('goes from system to light to dark and back, and remembers the choice', async () => {
    show();
    const button = screen.getByRole('button', { name: /^Appearance:/ });
    await userEvent.click(button);
    expect(button).toHaveAccessibleName('Appearance: light');
    expect(document.documentElement.dataset.mode).toBe('light');
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBe('light');
    await userEvent.click(button);
    expect(button).toHaveAccessibleName('Appearance: dark');
    expect(document.documentElement.dataset.mode).toBe('dark');
    await userEvent.click(button);
    expect(button).toHaveAccessibleName('Appearance: system');
    expect(document.documentElement.dataset.mode).toBeUndefined();
    expect(localStorage.getItem(THEME_STORAGE_KEY)).toBeNull();
  });

  it('starts from the stored choice', () => {
    localStorage.setItem(THEME_STORAGE_KEY, 'dark');
    show();
    expect(screen.getByRole('button', { name: 'Appearance: dark' })).toBeInTheDocument();
  });

  it('never names itself with "en", which a frozen test uses to find the English button', () => {
    for (const mode of ['system', 'light', 'dark']) {
      localStorage.setItem(THEME_STORAGE_KEY, mode);
      const { unmount } = render(
        <I18nextProvider i18n={setupI18n('sl')}>
          <ThemeSwitch />
        </I18nextProvider>,
      );
      expect(screen.getByRole('button', { name: /^Videz:/ }).getAttribute('aria-label')).not.toMatch(/en/i);
      unmount();
    }
  });
});
