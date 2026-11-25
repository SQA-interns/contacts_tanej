import { Monitor, Moon, Sun } from 'lucide-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/button';
import { nextThemeMode, readThemeMode, saveThemeMode, type ThemeMode } from '@/lib/theme';

const ICONS = { system: Monitor, light: Sun, dark: Moon } satisfies Record<ThemeMode, unknown>;

/**
 * The theme switch in the header (post-run, the developer's wish): one button that goes from the
 * system setting to light, to dark and back. Its name says the current appearance and that pressing
 * changes it; the icon shows the same.
 */
export function ThemeSwitch() {
  const { t } = useTranslation();
  const [mode, setMode] = useState<ThemeMode>(readThemeMode);
  const Icon = ICONS[mode];
  const label = t('header.theme', { mode: t(`theme.${mode}`) });
  return (
    <Button
      variant="ghost"
      aria-label={label}
      title={label}
      onClick={() => {
        const next = nextThemeMode(mode);
        saveThemeMode(next);
        setMode(next);
      }}
    >
      <Icon aria-hidden="true" className="size-4 shrink-0" />
    </Button>
  );
}
