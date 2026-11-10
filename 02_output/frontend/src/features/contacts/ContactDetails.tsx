import { useTranslation } from 'react-i18next';
import { Card } from '@/components/ui/card';
import { countryName } from '@/lib/format';
import type { Language } from '@/lib/i18n';
import type { Attribute } from './AttributeRows';
import { FIELDS, type Values } from './contactValues';

interface ContactDetailsProps {
  title: string;
  values: Values;
  tags: string[];
  attributes: Attribute[];
}

/** The values of a contact as text: what a Viewer sees, and a contact in Trash (WF-10). */
export function ContactDetails({ title, values, tags, attributes }: ContactDetailsProps) {
  const { t, i18n } = useTranslation();
  const language = i18n.language as Language;
  return (
    <Card>
      <h2 className="mb-3 text-xl font-semibold">{title}</h2>
      <dl className="grid gap-4 sm:grid-cols-2">
        {FIELDS.map((name) => (
          <div key={name} className={name === 'notes' ? 'sm:col-span-2' : undefined}>
            <dt className="text-sm text-text-muted">{t(`contact.field.${name}`)}</dt>
            <dd className="break-words whitespace-pre-wrap">
              {name === 'country' && values.country
                ? countryName(values.country, language)
                : values[name] || <span aria-label={t('contact.empty')}>–</span>}
            </dd>
          </div>
        ))}
        <div className="sm:col-span-2">
          <dt className="text-sm text-text-muted">{t('contact.field.tags')}</dt>
          <dd>
            {tags.length === 0 ? (
              <span aria-label={t('contact.empty')}>–</span>
            ) : (
              <ul aria-label={t('contact.tagsOfContact')} className="flex flex-wrap gap-2">
                {tags.map((tag) => (
                  <li
                    key={tag}
                    className="rounded-control border border-border bg-surface-raised px-3 py-1 break-all"
                  >
                    {tag}
                  </li>
                ))}
              </ul>
            )}
          </dd>
        </div>
        <div className="sm:col-span-2">
          <dt className="text-sm text-text-muted">{t('contact.field.attributes')}</dt>
          <dd>
            {attributes.length === 0 ? (
              <span aria-label={t('contact.empty')}>–</span>
            ) : (
              <dl
                aria-label={t('contact.attributesOfContact')}
                className="grid gap-1 sm:grid-cols-[auto_1fr]"
              >
                {attributes.map((attribute) => (
                  <div key={attribute.key} className="contents">
                    <dt className="font-medium break-all">{attribute.key}</dt>
                    <dd className="break-words whitespace-pre-wrap">{attribute.value}</dd>
                  </div>
                ))}
              </dl>
            )}
          </dd>
        </div>
      </dl>
    </Card>
  );
}
