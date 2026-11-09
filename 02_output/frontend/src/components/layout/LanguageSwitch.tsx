import { useTranslation } from 'react-i18next';
import { LANGUAGES, LANGUAGE_STORAGE_KEY, type Language } from '@/lib/i18n';
import { Button } from '@/components/ui/button';

/** The switch in the header (R-018-02): all texts change at once, the choice is kept per browser (R-018-03). */
export function LanguageSwitch() {
  const { t, i18n } = useTranslation();
  const choose = (language: Language) => {
    localStorage.setItem(LANGUAGE_STORAGE_KEY, language);
    void i18n.changeLanguage(language);
  };
  return (
    <div role="group" aria-label={t('header.language')} className="flex gap-1">
      {LANGUAGES.map((language) => (
        <Button
          key={language}
          variant={i18n.language === language ? 'secondary' : 'ghost'}
          aria-pressed={i18n.language === language}
          lang={language}
          onClick={() => choose(language)}
        >
          {language.toUpperCase()}
        </Button>
      ))}
    </div>
  );
}
