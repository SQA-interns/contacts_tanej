import { useQuery } from '@tanstack/react-query';
import { useId, useState, type FormEvent, type KeyboardEvent, type ReactNode } from 'react';
import { Dialog } from 'radix-ui';
import { useTranslation } from 'react-i18next';
import { useServices } from '@/app/services';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import type { components } from '@/lib/api/schema';
import { COUNTRY_CODES } from '@/lib/countries';
import { countryName } from '@/lib/format';
import type { Language } from '@/lib/i18n';
import { CONDITION_TYPES, type Condition, type ConditionType } from './conditions';

type Tag = components['schemas']['Tag'];

const SELECT_CLASS =
  'min-h-11 w-full rounded-control border border-border bg-background px-3 text-base text-text ' +
  'aria-[invalid=true]:border-danger';

interface Draft {
  type: ConditionType | '';
  tags: string[];
  typed: string;
  tag: string;
  key: string;
  value: string;
  country: string;
  company: string;
}

const EMPTY: Draft = { type: '', tags: [], typed: '', tag: '', key: '', value: '', country: '', company: '' };

/** The condition the draft describes, or the field that is still empty (R-010-01 to R-010-07). */
function build(draft: Draft): { condition: Condition } | { missing: string } {
  const key = draft.key.trim();
  const tags = [...draft.tags, ...(draft.typed.trim() ? [draft.typed.trim()] : [])];
  switch (draft.type) {
    case 'TAGS_ALL':
    case 'TAGS_ANY':
      return tags.length > 0 ? { condition: { type: draft.type, tags } } : { missing: 'tags' };
    case 'TAG_NOT':
      return draft.tag.trim()
        ? { condition: { type: 'TAG_NOT', tag: draft.tag.trim() } }
        : { missing: 'tag' };
    case 'ATTRIBUTE_EQUALS':
      return key
        ? { condition: { type: 'ATTRIBUTE_EQUALS', key, value: draft.value.trim() } }
        : { missing: 'key' };
    case 'ATTRIBUTE_EXISTS':
      return key ? { condition: { type: 'ATTRIBUTE_EXISTS', key } } : { missing: 'key' };
    case 'COUNTRY':
      return draft.country
        ? { condition: { type: 'COUNTRY', country: draft.country } }
        : { missing: 'country' };
    case 'COMPANY':
      return draft.company.trim()
        ? { condition: { type: 'COMPANY', company: draft.company.trim() } }
        : { missing: 'company' };
    default:
      return { missing: 'type' };
  }
}

function useSuggestions(
  groupId: string,
  kind: 'tags' | 'attribute-keys' | 'companies',
  part: string,
  on: boolean,
) {
  const { api } = useServices();
  const query = useQuery({
    queryKey: [kind, groupId, part],
    queryFn: async () => {
      const found = await api.request<Tag[] | string[]>(
        'GET',
        `/groups/${encodeURIComponent(groupId)}/${kind}?q=${encodeURIComponent(part)}`,
      );
      return found.map((item) => (typeof item === 'string' ? item : item.name));
    },
    enabled: on && part !== '',
    retry: false,
  });
  return query.data ?? [];
}

interface FilterDialogProps {
  groupId: string;
  onAdd: (condition: Condition) => void;
  onClose: () => void;
}

/**
 * WF-09, "Filter contacts": a choice of exactly the seven conditions and only the fields of the
 * chosen one (R-010-09). There is no way to save a filter. Suggestions come from the group.
 */
export function FilterDialog({ groupId, onAdd, onClose }: FilterDialogProps) {
  const { t, i18n } = useTranslation();
  const id = useId();
  const [draft, setDraft] = useState<Draft>(EMPTY);
  const [missing, setMissing] = useState<string | null>(null);
  const set = (change: Partial<Draft>) => {
    setDraft((d) => ({ ...d, ...change }));
    setMissing(null);
  };
  const tagHints = useSuggestions(
    groupId,
    'tags',
    draft.typed.trim(),
    draft.type === 'TAGS_ALL' || draft.type === 'TAGS_ANY',
  );
  const notHints = useSuggestions(groupId, 'tags', draft.tag.trim(), draft.type === 'TAG_NOT');
  const keyHints = useSuggestions(
    groupId,
    'attribute-keys',
    draft.key.trim(),
    draft.type === 'ATTRIBUTE_EQUALS' || draft.type === 'ATTRIBUTE_EXISTS',
  );
  const companyHints = useSuggestions(groupId, 'companies', draft.company.trim(), draft.type === 'COMPANY');

  function submit(event: FormEvent) {
    event.preventDefault();
    const built = build(draft);
    if ('condition' in built) onAdd(built.condition);
    else setMissing(built.missing);
  }

  function addTag(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key !== 'Enter' && event.key !== ',') return;
    event.preventDefault();
    const name = draft.typed.trim();
    if (name) set({ tags: [...draft.tags, name], typed: '' });
  }

  const error = (field: string) =>
    missing === field ? (
      <p id={`${id}-${field}-error`} role="alert" className="text-sm text-danger">
        {t(`list.filter.error.${field}`)}
      </p>
    ) : null;
  const invalid = (field: string) =>
    missing === field ? { 'aria-invalid': true, 'aria-describedby': `${id}-${field}-error` } : {};
  const field = (name: string, label: string, control: ReactNode) => (
    <div className="flex flex-col gap-1">
      <Label htmlFor={`${id}-${name}`}>{label}</Label>
      {control}
      {error(name)}
    </div>
  );
  const hints = (name: string, values: string[]) => (
    <datalist id={`${id}-${name}-list`}>
      {values.map((v) => (
        <option key={v} value={v} />
      ))}
    </datalist>
  );
  const textInput = (name: keyof Draft, hintList?: string[]) => (
    <>
      <Input
        id={`${id}-${name}`}
        value={draft[name] as string}
        autoComplete="off"
        list={hintList ? `${id}-${name}-list` : undefined}
        onChange={(e) => set({ [name]: e.target.value })}
        {...invalid(name)}
      />
      {hintList && hints(name, hintList)}
    </>
  );

  return (
    <Dialog.Root open onOpenChange={(open) => !open && onClose()}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 bg-text/50" />
        <Dialog.Content
          aria-describedby={`${id}-intro`}
          className="fixed top-1/2 left-1/2 max-h-[90vh] w-[calc(100%-2rem)] max-w-xl -translate-x-1/2 -translate-y-1/2 overflow-y-auto rounded-card border border-border bg-surface-raised p-6 shadow-card"
        >
          <Dialog.Title className="mb-2 text-xl font-semibold">{t('list.filter.title')}</Dialog.Title>
          <p id={`${id}-intro`} className="mb-4 text-text-muted">
            {t('list.filter.intro')}
          </p>
          <form onSubmit={submit} noValidate className="flex flex-col gap-4">
            {field(
              'type',
              t('list.filter.condition'),
              <select
                id={`${id}-type`}
                className={SELECT_CLASS}
                value={draft.type}
                onChange={(e) => setDraft({ ...EMPTY, type: e.target.value as ConditionType | '' })}
                {...invalid('type')}
              >
                <option value="">{t('list.filter.choose')}</option>
                {CONDITION_TYPES.map((type) => (
                  <option key={type} value={type}>
                    {t(`list.filter.types.${type}`)}
                  </option>
                ))}
              </select>,
            )}
            {(draft.type === 'TAGS_ALL' || draft.type === 'TAGS_ANY') && (
              <div className="flex flex-col gap-1">
                <Label htmlFor={`${id}-typed`}>{t('list.filter.tags')}</Label>
                {draft.tags.length > 0 && (
                  <ul className="flex flex-wrap gap-2">
                    {draft.tags.map((tag, index) => (
                      <li
                        key={`${tag}-${index}`}
                        className="flex items-center gap-1 rounded-control border border-border bg-surface pl-3"
                      >
                        <span className="break-all">{tag}</span>
                        <button
                          type="button"
                          className="min-h-11 min-w-11 rounded-control"
                          aria-label={t('contact.removeTag', { tag })}
                          onClick={() => set({ tags: draft.tags.filter((_, i) => i !== index) })}
                        >
                          <span aria-hidden="true">×</span>
                        </button>
                      </li>
                    ))}
                  </ul>
                )}
                <Input
                  id={`${id}-typed`}
                  value={draft.typed}
                  autoComplete="off"
                  list={`${id}-typed-list`}
                  onChange={(e) => set({ typed: e.target.value })}
                  onKeyDown={addTag}
                  {...invalid('tags')}
                />
                {hints('typed', tagHints)}
                <p className="text-sm text-text-muted">{t('list.filter.tagsHint')}</p>
                {error('tags')}
              </div>
            )}
            {draft.type === 'TAG_NOT' && field('tag', t('list.filter.tag'), textInput('tag', notHints))}
            {(draft.type === 'ATTRIBUTE_EQUALS' || draft.type === 'ATTRIBUTE_EXISTS') &&
              field('key', t('list.filter.key'), textInput('key', keyHints))}
            {draft.type === 'ATTRIBUTE_EQUALS' && field('value', t('list.filter.value'), textInput('value'))}
            {draft.type === 'COUNTRY' &&
              field(
                'country',
                t('list.filter.country'),
                <select
                  id={`${id}-country`}
                  className={SELECT_CLASS}
                  value={draft.country}
                  onChange={(e) => set({ country: e.target.value })}
                  {...invalid('country')}
                >
                  <option value="">{t('list.filter.choose')}</option>
                  {COUNTRY_CODES.map((code) => (
                    <option key={code} value={code}>
                      {countryName(code, i18n.language as Language)}
                    </option>
                  ))}
                </select>,
              )}
            {draft.type === 'COMPANY' &&
              field('company', t('list.filter.company'), textInput('company', companyHints))}
            <div className="flex justify-end gap-2">
              <Button type="button" variant="secondary" onClick={onClose}>
                {t('form.cancel')}
              </Button>
              <Button type="submit">{t('list.filter.add')}</Button>
            </div>
          </form>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
