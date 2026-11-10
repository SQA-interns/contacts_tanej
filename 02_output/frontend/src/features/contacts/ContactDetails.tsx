import { useTranslation } from 'react-i18next';
import { Card } from '@/components/ui/card';
import { countryName } from '@/lib/format';
import type { Language } from '@/lib/i18n';
import { FIELDS, type Values } from './contactValues';

interface ContactDetailsProps {
  title: string;
  values: Values;
}

/** The values of a contact as text: what a Viewer sees, and a contact in Trash (WF-10). */
export function ContactDetails({ title, values }: ContactDetailsProps) {
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
      </dl>
    </Card>
  );
}
