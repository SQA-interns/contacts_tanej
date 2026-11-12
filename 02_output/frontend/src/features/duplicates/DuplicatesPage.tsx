import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { useOutletContext } from 'react-router';
import { useServices } from '@/app/services';
import { Button } from '@/components/ui/button';
import { StatePanel } from '@/components/ui/state-panel';
import type { components } from '@/lib/api/schema';
import { errorText } from '@/lib/i18n';
import { ApiError } from '@/lib/api/client';

type GroupSummary = components['schemas']['GroupSummary'];
type DuplicateSet = components['schemas']['DuplicateSet'];
type Result = components['schemas']['DuplicateSearchResult'];
type Member = DuplicateSet['contacts'][number];

const REASON_KEY = {
  SAME_EMAIL: 'duplicates.reason.email',
  SAME_NAME_AND_COMPANY: 'duplicates.reason.company',
  SAME_NAME_AND_PHONE: 'duplicates.reason.phone',
} as const;

/** The name shown for a contact: first and last name, else e-mail, else company. */
const labelOf = (m: Member): string =>
  [m.firstName, m.lastName].filter(Boolean).join(' ') || m.email || m.company || m.phone || '';

const lowerFirst = (text: string) => text.charAt(0).toLowerCase() + text.slice(1);

/**
 * WF-16, the Duplicates tab (US-021). The search runs only when the user presses "Find
 * duplicates" (R-021-08) and covers the whole group whatever the list filter (R-021-01). A set
 * marked "not a duplicate" leaves the list at once; the server remembers it (R-021-09). A Viewer
 * has no tab and no button (R-021-11).
 */
export function DuplicatesPage() {
  const { t } = useTranslation();
  const { api } = useServices();
  const group = useOutletContext<GroupSummary>();
  const [sets, setSets] = useState<DuplicateSet[] | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const base = `/groups/${encodeURIComponent(group.id)}/duplicates`;
  const search = useMutation({
    mutationFn: () => api.request<Result>('POST', `${base}/search`),
    onSuccess: (result) => {
      setMessage(null);
      setSets(result.sets);
    },
  });

  if (group.role === 'VIEWER') return null;

  async function dismiss(set: DuplicateSet) {
    setMessage(null);
    try {
      await api.request('POST', `${base}/dismissals`, { contactIds: set.contacts.map((c) => c.id) });
      setSets((current) => current?.filter((s) => s !== set) ?? null);
    } catch (failure) {
      setMessage(errorText(t, failure instanceof ApiError ? failure.code : 'INTERNAL_ERROR'));
    }
  }

  const find = (
    <Button busy={search.isPending} onClick={() => search.mutate()}>
      {search.isPending ? t('duplicates.searching') : t('duplicates.find')}
    </Button>
  );

  return (
    <section className="flex flex-col gap-4">
      {sets !== null && <div className="flex justify-end">{find}</div>}
      {message && (
        <div role="alert" className="rounded-card border border-border bg-surface-raised p-3">
          {message}
        </div>
      )}
      {search.isError ? (
        <StatePanel state="error" onRetry={() => search.mutate()} />
      ) : sets === null ? (
        <div className="flex flex-col items-start gap-3">
          <h2 className="text-xl font-semibold">{t('duplicates.find')}</h2>
          <p className="text-text-muted">{t('duplicates.intro')}</p>
          {find}
        </div>
      ) : sets.length === 0 ? (
        <div className="flex flex-col gap-1">
          <h2 className="text-xl font-semibold">{t('duplicates.noneTitle')}</h2>
          <p className="text-text-muted">{t('duplicates.noneText')}</p>
        </div>
      ) : (
        <ul aria-label={t('duplicates.listLabel')} className="flex flex-col gap-3">
          {sets.map((set) => (
            <SetItem key={set.contacts.map((c) => c.id).join()} set={set} onDismiss={() => dismiss(set)} />
          ))}
        </ul>
      )}
    </section>
  );
}

function SetItem({ set, onDismiss }: { set: DuplicateSet; onDismiss: () => void }) {
  const { t } = useTranslation();
  const reasons = set.reasons
    .map((r, i) => (i === 0 ? t(REASON_KEY[r]) : lowerFirst(t(REASON_KEY[r]))))
    .join(', ');
  return (
    <li className="flex flex-wrap items-start justify-between gap-3 rounded-card border border-border bg-surface p-3">
      <div className="flex min-w-0 flex-col gap-2">
        <div className="flex flex-wrap items-center gap-2">
          <span className="rounded-control border border-border bg-surface-raised px-2 py-0.5 text-sm font-semibold">
            {t(set.kind === 'CERTAIN' ? 'duplicates.certain' : 'duplicates.probable')}
          </span>
          <span className="text-sm text-text-muted">{reasons}</span>
        </div>
        <div className="flex flex-wrap gap-2">
          {set.contacts.map((c) => (
            <span key={c.id} className="rounded-control border border-border px-2 py-1 break-words">
              {labelOf(c)}
            </span>
          ))}
        </div>
      </div>
      <Button variant="ghost" onClick={onDismiss}>
        {t('duplicates.notDuplicate')}
      </Button>
    </li>
  );
}
