import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Link, useOutletContext, useParams } from 'react-router';
import { useServices } from '@/app/services';
import { StatePanel } from '@/components/ui/state-panel';
import { Pager } from '@/features/contacts/Pager';
import type { components } from '@/lib/api/schema';
import { ChangeSetView, changeSets } from './ChangeSetView';

type GroupSummary = components['schemas']['GroupSummary'];
type HistoryPageData = components['schemas']['HistoryPage'];

/**
 * WF-11, the History tab of a contact (US-013): newest first, one block per save. A Viewer has no
 * History tab; the API refuses the request anyway (R-013-13).
 */
export function ContactHistoryPage() {
  const { t } = useTranslation();
  const { api } = useServices();
  const group = useOutletContext<GroupSummary>();
  const { contactId = '' } = useParams();
  const [page, setPage] = useState(1);
  const base = `/groups/${encodeURIComponent(group.id)}/contacts/${encodeURIComponent(contactId)}`;
  const history = useQuery({
    queryKey: ['history', group.id, contactId, page],
    queryFn: () => api.request<HistoryPageData>('GET', `${base}/history?page=${page}`),
    enabled: group.role !== 'VIEWER',
  });

  if (group.role === 'VIEWER') return null;
  if (history.isPending) return <StatePanel state="loading" />;
  if (history.isError) return <StatePanel state="error" onRetry={() => void history.refetch()} />;
  const data = history.data;
  return (
    <>
      <p className="mb-2 text-sm">
        <Link to={`/groups/${group.id}/contacts/${contactId}`} className="underline">
          {t('history.back')}
        </Link>
      </p>
      <h1 className="mb-4 text-2xl font-semibold">{t('history.title')}</h1>
      {data.items.length === 0 ? (
        <section className="flex flex-col gap-1">
          <h2 className="text-xl font-semibold">{t('history.emptyTitle')}</h2>
          <p className="text-text-muted">{t('history.emptyText')}</p>
        </section>
      ) : (
        <ol className="flex flex-col gap-3">
          {changeSets(data.items).map((set) => (
            <ChangeSetView key={set[0]!.changeSetId} set={set} />
          ))}
        </ol>
      )}
      <Pager page={data.page} last={Math.max(1, Math.ceil(data.total / data.pageSize))} onPage={setPage} />
    </>
  );
}
