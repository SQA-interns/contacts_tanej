import { Check } from 'lucide-react';
import { useId, useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { COUNTRY_CODES } from '@/lib/countries';
import { countryName } from '@/lib/format';
import type { Language } from '@/lib/i18n';
import { cn } from '@/lib/utils';
import { FIELDS, maxLength, type Field, type Values } from './contactValues';
import { AttributeRows, attributeProblems, type Attribute } from './AttributeRows';
import { TagInput, tagProblems } from './TagInput';

const TEXT_FIELDS = FIELDS.filter((f) => f !== 'country' && f !== 'notes');
const HINTS: Partial<Record<Field, string>> = { email: 'emailHint', phone: 'phoneHint', notes: 'notesHint' };
const SELECT_CLASS =
  'min-h-11 w-full rounded-control border border-border bg-background px-3 text-base text-text ' +
  'aria-[invalid=true]:border-danger';

interface ContactFormProps {
  groupId: string;
  initial: Values;
  initialTags: string[];
  initialAttributes: Attribute[];
  /** The refused fields of the last save: field name to the code of the broken rule. */
  problems: Partial<Record<string, string>>;
  busy: boolean;
  onSubmit: (values: Values, tags: string[], attributes: Attribute[]) => void;
  onCancel: () => void;
}

/**
 * WF-10, the form of the 11 predefined fields (R-005-01, R-005-07). The server decides every rule;
 * the typed values stay when it refuses (UI-13) and each refused field shows its reason.
 */
export function ContactForm({
  groupId,
  initial,
  initialTags,
  initialAttributes,
  problems,
  busy,
  onSubmit,
  onCancel,
}: ContactFormProps) {
  const { t, i18n } = useTranslation();
  const id = useId();
  const [values, setValues] = useState<Values>(initial);
  const [tags, setTags] = useState<string[]>(initialTags);
  const [typed, setTyped] = useState('');
  const [rows, setRows] = useState<Attribute[]>(initialAttributes);
  const language = i18n.language as Language;
  const oneOfRefused = problems.firstName === 'REQUIRED_ONE_OF';

  function submit(event: FormEvent) {
    event.preventDefault();
    const typedTag = typed.trim();
    const all = typedTag === '' ? tags : [...tags, typedTag];
    setTags(all);
    setTyped('');
    const filled = rows.filter((row) => row.key.trim() !== '' || row.value !== '');
    setRows(filled);
    onSubmit(values, all, filled);
  }

  function reason(field: Field): string | undefined {
    const code = problems[field];
    if (!code) return undefined;
    return t(`contact.error.${code}`, { max: maxLength(field), defaultValue: t('error.VALIDATION_FAILED') });
  }

  function field(name: Field, control: (props: ControlProps) => React.ReactNode, wide = false) {
    const message = reason(name);
    const hint = HINTS[name];
    const props: ControlProps = {
      id: `${id}-${name}`,
      value: values[name],
      invalid: message !== undefined,
      describedBy: [hint ? `${id}-${name}-hint` : '', message ? `${id}-${name}-error` : '']
        .filter(Boolean)
        .join(' '),
      onChange: (value) => setValues((previous) => ({ ...previous, [name]: value })),
    };
    return (
      <div key={name} className={cn('flex flex-col gap-1', wide && 'sm:col-span-2')}>
        <Label htmlFor={props.id}>{t(`contact.field.${name}`)}</Label>
        {control(props)}
        {hint && (
          <p id={`${id}-${name}-hint`} className="text-sm text-text-muted">
            {t(`contact.${hint}`)}
          </p>
        )}
        {message && (
          <p id={`${id}-${name}-error`} role="alert" className="text-sm text-danger">
            {message}
          </p>
        )}
      </div>
    );
  }

  return (
    <form aria-label={t('contact.form')} noValidate onSubmit={submit}>
      <Card className="mb-4">
        <h2 className="mb-3 text-xl font-semibold">{t('contact.details')}</h2>
        {oneOfRefused && (
          <div role="alert" className="mb-4 rounded-card border border-danger bg-surface-raised p-3">
            {t('contact.error.REQUIRED_ONE_OF')}
          </div>
        )}
        <div className="grid gap-4 sm:grid-cols-2">
          {TEXT_FIELDS.map((name) =>
            field(name, (p) => (
              <Input
                id={p.id}
                type={name === 'email' ? 'email' : 'text'}
                value={p.value}
                autoComplete="off"
                aria-invalid={p.invalid || undefined}
                aria-describedby={p.describedBy || undefined}
                onChange={(e) => p.onChange(e.target.value)}
              />
            )),
          )}
          {field('country', (p) => (
            <select
              id={p.id}
              value={p.value}
              className={SELECT_CLASS}
              aria-invalid={p.invalid || undefined}
              aria-describedby={p.describedBy || undefined}
              onChange={(e) => p.onChange(e.target.value)}
            >
              <option value="">{t('contact.countryChoose')}</option>
              {COUNTRY_CODES.map((code) => (
                <option key={code} value={code}>
                  {countryName(code, language)}
                </option>
              ))}
            </select>
          ))}
          {field(
            'notes',
            (p) => (
              <textarea
                id={p.id}
                value={p.value}
                rows={5}
                className={cn(SELECT_CLASS, 'py-2')}
                aria-invalid={p.invalid || undefined}
                aria-describedby={p.describedBy || undefined}
                onChange={(e) => p.onChange(e.target.value)}
              />
            ),
            true,
          )}
          <TagInput
            groupId={groupId}
            tags={tags}
            typed={typed}
            problems={tagProblems(problems)}
            onTags={setTags}
            onTyped={setTyped}
          />
          <AttributeRows
            groupId={groupId}
            rows={rows}
            problems={attributeProblems(problems)}
            onRows={setRows}
          />
        </div>
      </Card>
      <div className="sticky bottom-0 flex gap-3 border-t border-border bg-background py-3">
        <Button type="submit" busy={busy}>
          {!busy && <Check aria-hidden="true" className="size-4 shrink-0" />}
          {busy ? t('contact.saving') : t('contact.save')}
        </Button>
        <Button type="button" variant="secondary" disabled={busy} onClick={onCancel}>
          {t('form.cancel')}
        </Button>
      </div>
    </form>
  );
}

interface ControlProps {
  id: string;
  value: string;
  invalid: boolean;
  describedBy: string;
  onChange: (value: string) => void;
}
