import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate, useOutletContext, useSearchParams } from 'react-router';
import { useServices } from '@/app/services';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { StatePanel } from '@/components/ui/state-panel';
import type { Attribute } from '@/features/contacts/AttributeRows';
import { FIELDS, valuesOf, type Contact, type Field, type Values } from '@/features/contacts/contactValues';
import { ApiError } from '@/lib/api/client';
import type { components } from '@/lib/api/schema';
import { countryName } from '@/lib/format';
import { errorText, type Language } from '@/lib/i18n';

type GroupSummary = components['schemas']['GroupSummary'];

/** The picked contacts, the oldest first; the same creation time is decided by the lower id (EX-022-03). */
export const byAge = (contacts: Contact[]): Contact[] =>
  [...contacts].sort((a, b) => a.createdAt.localeCompare(b.createdAt) || a.id.localeCompare(b.id));

/** For each field the first non-empty value, the oldest contact first (R-022-02). */
export function preselect(contacts: Contact[]): Record<Field, number> {
  return Object.fromEntries(
    FIELDS.map((field) => {
      const found = contacts.findIndex((c) => valuesOf(c)[field] !== '');
      return [field, Math.max(found, 0)];
    }),
  ) as Record<Field, number>;
}

/** The keys that have different values (ignoring the case of the key) and need a choice (R-022-04). */
export function attributeConflicts(contacts: Contact[]): { key: string; values: string[] }[] {
  const seen = new Map<string, { key: string; values: string[] }>();
  for (const attribute of contacts.flatMap((c) => c.attributes ?? [])) {
    const entry = seen.get(attribute.key.toLowerCase()) ?? { key: attribute.key, values: [] };
    if (!entry.values.includes(attribute.value)) entry.values.push(attribute.value);
    seen.set(attribute.key.toLowerCase(), entry);
  }
  return [...seen.values()].filter((e) => e.values.length > 1);
}

/** Every tag once, ignoring the case, in the spelling met first (R-022-03). */
export function combinedTags(contacts: Contact[]): string[] {
  const seen = new Map<string, string>();
  for (const tag of contacts.flatMap((c) => c.tags ?? [])) {
    if (!seen.has(tag.toLowerCase())) seen.set(tag.toLowerCase(), tag);
  }
  return [...seen.values()];
}

/**
 * WF-17, the merge screen (US-022). The contacts are read when the screen opens, so a change by
 * someone else meanwhile makes the merge a conflict (R-022-12). Nothing is sent before "Confirm
 * merge" (R-022-11, R-022-14); a Viewer has no screen (R-022-13).
 */
export function MergePage() {
  const { api } = useServices();
  const group = useOutletContext<GroupSummary>();
  const [params] = useSearchParams();
  const ids = (params.get('ids') ?? '').split(',').filter(Boolean);
  const contacts = useQuery({
    queryKey: ['merge-contacts', group.id, ids.join()],
    queryFn: () =>
      Promise.all(
        ids.map((id) =>
          api.request<Contact>(
            'GET',
            `/groups/${encodeURIComponent(group.id)}/contacts/${encodeURIComponent(id)}`,
          ),
        ),
      ),
    enabled: group.role !== 'VIEWER' && ids.length >= 2,
    staleTime: Infinity,
    gcTime: 0,
    refetchOnWindowFocus: false,
  });
  const { t } = useTranslation();
  if (group.role === 'VIEWER') return null;
  return (
    <section className="flex flex-col gap-4">
      <p>
        <Link to={`/groups/${group.id}/duplicates`} className="underline">
          {t('duplicates.tab')}
        </Link>{' '}
        / {t('merge.crumb')}
      </p>
      <h2 className="text-xl font-semibold">{t('merge.title')}</h2>
      {contacts.isPending && ids.length >= 2 ? (
        <Skeleton />
      ) : contacts.isError || !contacts.data ? (
        <StatePanel state="error" onRetry={() => void contacts.refetch()} />
      ) : (
        <MergeForm group={group} opened={byAge(contacts.data)} />
      )}
    </section>
  );
}

function MergeForm({ group, opened }: { group: GroupSummary; opened: Contact[] }) {
  const { t, i18n } = useTranslation();
  const { api } = useServices();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [picked, setPicked] = useState(() => preselect(opened));
  const conflicts = attributeConflicts(opened);
  const [chosen, setChosen] = useState<Record<string, string>>({});
  const [message, setMessage] = useState<string | null>(null);
  const language = i18n.language as Language;
  const back = `/groups/${group.id}/duplicates`;
  const allChosen = conflicts.every((c) => chosen[c.key.toLowerCase()] !== undefined);

  const shown = (field: Field, values: Values) =>
    field === 'country' && values.country ? countryName(values.country, language) : values[field];

  const merge = useMutation({
    mutationFn: () => {
      const fields = Object.fromEntries(FIELDS.map((f) => [f, valuesOf(opened[picked[f]])[f]]));
      const attributeChoices: Attribute[] = conflicts.map((c) => ({
        key: c.key,
        value: chosen[c.key.toLowerCase()] ?? '',
      }));
      return api.request<Contact>('POST', `/groups/${encodeURIComponent(group.id)}/merges`, {
        contacts: opened.map((c) => ({ id: c.id, revision: c.revision })),
        fields,
        attributeChoices,
      });
    },
    onSuccess: async (survivor) => {
      await queryClient.invalidateQueries();
      void navigate(`/groups/${group.id}/contacts/${survivor.id}`);
    },
    onError: (failure) => setMessage(failureText(failure)),
  });

  function failureText(failure: unknown): string {
    if (!(failure instanceof ApiError)) return errorText(t, 'INTERNAL_ERROR');
    const problem = failure.body.errors?.find((e) => e.code === 'TOO_MANY');
    if (problem?.field.startsWith('tags')) return t('contact.tagError.TOO_MANY');
    if (problem?.field.startsWith('attributes')) return t('contact.attributeError.TOO_MANY');
    return errorText(t, failure.code);
  }

  return (
    <>
      {message && (
        <div role="alert" className="rounded-card border border-border bg-surface-raised p-3">
          {message}
        </div>
      )}
      <div className="overflow-x-auto">
        <div role="group" aria-label={t('merge.fields')} className="flex min-w-[40rem] flex-col">
          <Row label="">
            {opened.map((c, i) => (
              <strong key={c.id}>{i === 0 ? t('merge.survivor') : t('merge.joining')}</strong>
            ))}
          </Row>
          {FIELDS.map((field) => (
            <Row key={field} label={t(`contact.field.${field}`)}>
              {opened.map((c, i) => (
                <Choice
                  key={c.id}
                  name={field}
                  checked={picked[field] === i}
                  onChange={() => setPicked({ ...picked, [field]: i })}
                  text={shown(field, valuesOf(c))}
                  empty={t('contact.empty')}
                />
              ))}
            </Row>
          ))}
        </div>
      </div>
      <div className="grid gap-4 md:grid-cols-2">
        <section className="rounded-card border border-border p-3">
          <h3 className="mb-2 font-semibold">{t('merge.tags')}</h3>
          <ul className="flex flex-wrap gap-2">
            {combinedTags(opened).map((tag) => (
              <li key={tag} className="rounded-control border border-border px-2 py-0.5">
                {tag}
              </li>
            ))}
          </ul>
        </section>
        {conflicts.length > 0 && (
          <section className="rounded-card border border-border p-3">
            <h3 className="mb-1 font-semibold">{t('merge.conflicts')}</h3>
            <p className="mb-2 text-text-muted">{t('merge.conflictsHint')}</p>
            {conflicts.map((c) => (
              <fieldset key={c.key.toLowerCase()} className="mb-2 flex flex-col gap-1">
                <legend className="font-semibold">{c.key}</legend>
                {c.values.map((value) => (
                  <Choice
                    key={value}
                    name={`attribute-${c.key.toLowerCase()}`}
                    checked={chosen[c.key.toLowerCase()] === value}
                    onChange={() => setChosen({ ...chosen, [c.key.toLowerCase()]: value })}
                    text={value}
                    empty={t('contact.empty')}
                  />
                ))}
              </fieldset>
            ))}
          </section>
        )}
      </div>
      <div role="status" className="rounded-card border border-border bg-surface-raised p-3">
        {t('merge.revertHint')}
      </div>
      <div className="sticky bottom-0 flex flex-wrap gap-3 bg-background py-3">
        <Button busy={merge.isPending} disabled={!allChosen} onClick={() => merge.mutate()}>
          {merge.isPending ? t('merge.merging') : t('merge.confirm')}
        </Button>
        <Button variant="secondary" onClick={() => void navigate(back)}>
          {t('form.cancel')}
        </Button>
      </div>
    </>
  );
}

function Row({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div
      className="grid items-start gap-3 border-b border-border py-2"
      style={{
        gridTemplateColumns: `8rem repeat(${Array.isArray(children) ? children.length : 1}, minmax(0, 1fr))`,
      }}
    >
      <span className="font-semibold">{label}</span>
      {children}
    </div>
  );
}

function Choice(props: {
  name: string;
  checked: boolean;
  onChange: () => void;
  text: string;
  empty: string;
}) {
  return (
    <label className="flex min-h-11 items-start gap-2 break-words">
      <input
        type="radio"
        name={props.name}
        checked={props.checked}
        onChange={props.onChange}
        className="mt-1"
      />
      <span>{props.text || <span aria-label={props.empty}>–</span>}</span>
    </label>
  );
}
