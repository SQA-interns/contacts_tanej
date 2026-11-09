import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import en from '../locales/en.json';
import sl from '../locales/sl.json';

export const LANGUAGES = ['sl', 'en'] as const;
export type Language = (typeof LANGUAGES)[number];
export const LANGUAGE_STORAGE_KEY = 'language';

/** Slovenian by default, whatever the browser language is (R-018-01, EX-018-01); the choice is per browser (R-018-03). */
export function initialLanguage(stored: string | null): Language {
  return LANGUAGES.find((language) => language === stored) ?? 'sl';
}

export function setupI18n(stored: string | null = localStorage.getItem(LANGUAGE_STORAGE_KEY)) {
  void i18n.use(initReactI18next).init({
    resources: { sl: { translation: sl }, en: { translation: en } },
    lng: initialLanguage(stored),
    fallbackLng: false,
    interpolation: { escapeValue: false },
    returnNull: false,
  });
  // The page language follows the active language (accessibility, hyphenation, screen readers).
  i18n.on('languageChanged', (language) => {
    document.documentElement.lang = language;
  });
  return i18n;
}
