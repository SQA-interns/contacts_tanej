import type { Language } from './i18n';

/** The project time zone (business-rules.md, R-018-05): the same in both languages, summer time included. */
export const PROJECT_TIME_ZONE = 'Europe/Ljubljana';

const dateTimeParts = new Intl.DateTimeFormat('en-GB', {
  timeZone: PROJECT_TIME_ZONE,
  year: 'numeric',
  month: 'numeric',
  day: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
});

const locales: Record<Language, string> = { sl: 'sl-SI', en: 'en-GB' };

/** `d. M. yyyy HH:mm` in Slovenian, `yyyy-MM-dd HH:mm` in English (R-018-04). */
export function formatDateTime(iso: string, language: Language): string {
  const part = Object.fromEntries(
    dateTimeParts.formatToParts(new Date(iso)).map(({ type, value }) => [type, value]),
  );
  const time = `${part.hour}:${part.minute}`;
  if (language === 'sl') return `${Number(part.day)}. ${Number(part.month)}. ${part.year} ${time}`;
  return `${part.year}-${part.month!.padStart(2, '0')}-${part.day!.padStart(2, '0')} ${time}`;
}

/** A decimal with a comma in Slovenian and a point in English (R-018-04). */
export function formatDecimal(value: number, language: Language): string {
  return new Intl.NumberFormat(locales[language]).format(value);
}

/** The country name in the active language (R-018-06); an unknown code is shown as it is. */
export function countryName(code: string, language: Language): string {
  try {
    return new Intl.DisplayNames([locales[language]], { type: 'region' }).of(code) ?? code;
  } catch {
    return code;
  }
}
