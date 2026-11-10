import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { useOutletContext } from 'react-router';
import { useServices } from '@/app/services';
import { StatePanel } from '@/components/ui/state-panel';
import { Pager } from '@/features/contacts/Pager';
import type { components } from '@/lib/api/schema';
import { ChangeSetView, changeSets } from './ChangeSetView';
import { HistoryFilters, NO_FILTERS, type Filters } from './HistoryFilters';

type GroupSummary = components['schemas']['GroupSummary'];
type HistoryPageData = components['schemas']['HistoryPage'];

function query(filters: Filters, page: number): string {
  const params = new URLSearchParams({ page: String(page) });
  if (filters.actorId) params.set('actorId', filters.actorId);
  if (filters.type) params.set('types', filters.type);
  if (filters.from) params.set('from', filters.from);
  if (filters.to) params.set('to', filters.to);
  return params.toString();
}

/**
 * WF-15, the group-wide History (US-014): newest first, 25 per page, filtered by person, event
 * type and days. A Viewer has no such tab; the API refuses the request anyway (R-014-07).
 */
export function GroupHistoryPage() {
  const { t } = useTranslation();
  const { api } = useServices();
  const group = useOutletContext<GroupSummary>();
  const [filters, setFilters] = useState<Filters>(NO_FILTERS);
  const [page, setPage] = useState(1);
  const base = `/groups/${encodeURIComponent(group.id)}/history`;
  const history = useQuery({
    queryKey: ['group-history', group.id, filters, page],
    queryFn: () => api.request<HistoryPageData>('GET', `${base}?${query(filters, page)}`),
    enabled: group.role !== 'VIEWER',
    placeholderData: (previous) => previous,
  });

  if (group.role === 'VIEWER') return null;
  const apply = (next: Filters) => {
    setFilters(next);
    setPage(1);
  };
  const filtered = JSON.stringify(filters) !== JSON.stringify(NO_FILTERS);

  let body;
  if (history.isPending) body = <StatePanel state="loading" />;
  else if (history.isError) body = <StatePanel state="error" onRetry={() => void history.refetch()} />;
  else if (history.data.items.length === 0 && filtered) {
    body = (
      <section className="flex flex-col items-start gap-2">
        <h2 className="text-xl font-semibold">{t('history.noMatchTitle')}</h2>
        <p className="text-text-muted">{t('history.noMatchText')}</p>
        <button type="button" className="underline" onClick={() => apply(NO_FILTERS)}>
          {t('history.clear')}
        </button>
      </section>
    );
  } else if (history.data.items.length === 0) {
    body = (
      <section className="flex flex-col gap-1">
        <h2 className="text-xl font-semibold">{t('history.emptyTitle')}</h2>
        <p className="text-text-muted">{t('history.groupEmptyText')}</p>
      </section>
    );
  } else {
    const data = history.data;
    body = (
      <>
        <ol aria-label={t('history.listLabel')} className="flex flex-col gap-3">
          {changeSets(data.items).map((set) => (
            <ChangeSetView key={set[0]!.changeSetId} set={set} showContact />
          ))}
        </ol>
        <Pager page={data.page} last={Math.max(1, Math.ceil(data.total / data.pageSize))} onPage={setPage} />
      </>
    );
  }
  return (
    <div className="grid gap-4 lg:grid-cols-[1fr_20rem]">
      <h2 className="sr-only lg:col-span-2">{t('history.title')}</h2>
      <div className="order-2 lg:order-1">{body}</div>
      <div className="order-1 lg:order-2">
        <HistoryFilters groupId={group.id} applied={filters} onApply={apply} />
      </div>
    </div>
  );
}
