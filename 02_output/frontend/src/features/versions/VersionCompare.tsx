import { useTranslation } from 'react-i18next';
import type { Attribute } from '@/features/contacts/AttributeRows';
import { FIELDS, type Values } from '@/features/contacts/contactValues';
import { countryName } from '@/lib/format';
import type { Language } from '@/lib/i18n';

/** What a version holds and the current contact holds: the 11 fields, tags and attributes. */
export interface Content {
  values: Values;
  tags: string[];
  attributes: Attribute[];
}

interface CompareProps {
  versionNumber: number;
  version: Content;
  current: Content;
}

interface Line {
  key: string;
  label: string;
  before: string;
  now: string;
}

const sorted = (tags: string[]) => [...tags].sort((a, b) => a.localeCompare(b)).join(', ');
const attributeText = (list: Attribute[]) =>
  [...list]
    .sort((a, b) => a.key.localeCompare(b.key))
    .map((a) => `${a.key}: ${a.value}`)
    .join('\n');

/**
 * WF-12, the state "Compare view": a version and the current contact side by side. A row that
 * differs is highlighted and carries the word "Changed", so colour is never the only signal (R-020-03).
 */
export function VersionCompare({ versionNumber, version, current }: CompareProps) {
  const { t, i18n } = useTranslation();
  const language = i18n.language as Language;
  const shown = (field: (typeof FIELDS)[number], values: Values) =>
    field === 'country' && values.country ? countryName(values.country, language) : values[field];
  const lines: Line[] = [
    ...FIELDS.map((field) => ({
      key: field,
      label: t(`contact.field.${field}`),
      before: shown(field, version.values),
      now: shown(field, current.values),
    })),
    {
      key: 'tags',
      label: t('contact.field.tags'),
      before: sorted(version.tags),
      now: sorted(current.tags),
    },
    {
      key: 'attributes',
      label: t('contact.field.attributes'),
      before: attributeText(version.attributes),
      now: attributeText(current.attributes),
    },
  ];
  return (
    <>
      <table className="w-full border-collapse text-left">
        <thead>
          <tr>
            <th scope="col" className="p-2">
              {t('versions.field')}
            </th>
            <th scope="col" className="p-2">
              {t('versions.version', { number: versionNumber })}
            </th>
            <th scope="col" className="p-2">
              {t('versions.currentContact')}
            </th>
          </tr>
        </thead>
        <tbody>
          {lines.map((line) => {
            const changed = line.before !== line.now;
            return (
              <tr key={line.key} className={changed ? 'bg-surface-raised font-medium' : undefined}>
                <th scope="row" className="p-2 align-top font-semibold">
                  {line.label}
                  {changed && (
                    <span className="ml-2 rounded-control border border-border px-1 text-sm">
                      {t('versions.changed')}
                    </span>
                  )}
                </th>
                <td className="p-2 align-top break-words whitespace-pre-wrap">
                  {line.before || <span aria-label={t('contact.empty')}>–</span>}
                </td>
                <td className="p-2 align-top break-words whitespace-pre-wrap">
                  {line.now || <span aria-label={t('contact.empty')}>–</span>}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
      <p className="mt-2 text-text-muted">{t('versions.highlightHint')}</p>
    </>
  );
}
