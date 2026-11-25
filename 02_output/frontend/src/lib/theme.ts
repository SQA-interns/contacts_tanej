/**
 * Light or dark appearance (post-run): "system" follows the operating system (the default and the
 * behaviour before the switch existed), "light" and "dark" are the user's choice, kept per browser
 * like the language. The choice is the data-mode attribute on <html>; tokens.final.css reads it.
 */
export const THEME_MODES = ['system', 'light', 'dark'] as const;
export type ThemeMode = (typeof THEME_MODES)[number];
export const THEME_STORAGE_KEY = 'theme';

export function readThemeMode(): ThemeMode {
  try {
    const stored = localStorage.getItem(THEME_STORAGE_KEY);
    return THEME_MODES.includes(stored as ThemeMode) ? (stored as ThemeMode) : 'system';
  } catch {
    return 'system'; // storage blocked: follow the system
  }
}

export function applyThemeMode(mode: ThemeMode): void {
  if (mode === 'system') delete document.documentElement.dataset.mode;
  else document.documentElement.dataset.mode = mode;
}

export function saveThemeMode(mode: ThemeMode): void {
  try {
    if (mode === 'system') localStorage.removeItem(THEME_STORAGE_KEY);
    else localStorage.setItem(THEME_STORAGE_KEY, mode);
  } catch {
    // storage blocked: the choice lasts until the page is reloaded
  }
  applyThemeMode(mode);
}

/** System, then light, then dark, then system again. */
export function nextThemeMode(mode: ThemeMode): ThemeMode {
  return THEME_MODES[(THEME_MODES.indexOf(mode) + 1) % THEME_MODES.length] ?? 'system';
}
