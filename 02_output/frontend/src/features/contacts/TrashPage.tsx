import { RotateCcw, TriangleAlert } from 'lucide-react';
import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { useOutletContext } from 'react-router';
import { useServices } from '@/app/services';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { StatePanel } from '@/components/ui/state-panel';
import { stacked } from '@/components/ui/stacked-table';
import { ConfirmDialog } from '@/features/members/ConfirmDialog';
import { ApiError } from '@/lib/api/client';
import type { components } from '@/lib/api/schema';
import { formatDateTime } from '@/lib/format';
import { errorText, type Language } from '@/lib/i18n';
import { Pager } from './Pager';
import { contactTitle, emptyValues } from './contactValues';

type GroupSummary = components['schemas']['GroupSummary'];
type TrashPageData = components['schemas']['TrashedContactPage'];
type Item = TrashPageData['items'][number];

/**
 * WF-14, the Trash view (US-012): the group's deleted contacts with the days left, "Restore" for
 * Owner and Editor and "Delete forever" for the Owner only (R-012-02, R-012-03, R-012-04). A Viewer
 * has no Trash tab; the API refuses the request anyway.
 */
export function TrashPage() {
  const { t, i18n } = useTranslation();
  const { api } = useServices();
  const group = useOutletContext<GroupSummary>();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(1);
  const [erasing, setErasing] = useState<Item | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [failure, setFailure] = useState<string | null>(null);
  const base = `/groups/${encodeURIComponent(group.id)}`;
  const trash = useQuery({
    queryKey: ['trash', group.id, page],
    queryFn: () => api.request<TrashPageData>('GET', `${base}/trash-contacts?page=${page}`),
  });

  async function run(action: () => Promise<void>, done: string | null) {
    setNotice(null);
    setFailure(null);
    try {
      await action();
      setNotice(done);
    } catch (error) {
      setFailure(errorText(t, error instanceof ApiError ? error.code : 'INTERNAL_ERROR'));
    } finally {
      await queryClient.invalidateQueries({ queryKey: ['trash', group.id] });
      await queryClient.invalidateQueries({ queryKey: ['contacts', group.id] });
      await queryClient.invalidateQueries({ queryKey: ['contact', group.id] });
    }
  }

  if (group.role === 'VIEWER') return null;
  if (trash.isPending) return <StatePanel state="loading" />;
  if (trash.isError) return <StatePanel state="error" onRetry={() => void trash.refetch()} />;
  const data = trash.data;
  return (
    <>
      <p className="mb-3 text-text-muted">{t('trash.intro')}</p>
      {notice && (
        <p role="status" className="mb-3 rounded-card border border-border bg-surface-raised p-3">
          {notice}
        </p>
      )}
      {failure && (
        <div role="alert" className="mb-3 rounded-card border border-danger bg-surface-raised p-3">
          {failure}
        </div>
      )}
      {data.items.length === 0 ? (
        <StatePanel state="empty" emptyText={t('trash.empty')} />
      ) : (
        <Card className="p-0">
          <table className={stacked.table}>
            <thead className={stacked.head}>
              <tr className="border-b border-border">
                <th scope="col" className="px-4 py-3">
                  {t('trash.contact')}
                </th>
                <th scope="col" className="px-4 py-3">
                  {t('trash.deleted')}
                </th>
                <th scope="col" className="px-4 py-3">
                  {t('trash.daysLeft')}
                </th>
                <th scope="col" className="px-4 py-3">
                  <span className="sr-only">{t('groups.columns.actions')}</span>
                </th>
              </tr>
            </thead>
            <tbody className={stacked.body}>
              {data.items.map((item) => (
                <tr key={item.id} className={stacked.row}>
                  <td data-label={t('trash.contact')} className={stacked.cell}>
                    {contactTitle({ ...emptyValues, ...item })}
                  </td>
                  <td data-label={t('trash.deleted')} className={stacked.cell}>
                    {formatDateTime(item.trashedAt, i18n.language as Language)}
                  </td>
                  <td data-label={t('trash.daysLeft')} className={stacked.cell}>
                    {item.daysLeft}
                  </td>
                  <td className={stacked.actions}>
                    <span className="flex flex-wrap justify-end gap-2">
                      <Button
                        variant="secondary"
                        onClick={() =>
                          void run(
                            () => api.request<void>('POST', `${base}/contacts/${item.id}/restore`),
                            t('trash.restored'),
                          )
                        }
                      >
                        <RotateCcw aria-hidden="true" className="size-4 shrink-0" />
                        {t('trash.restore')}
                      </Button>
                      {group.role === 'OWNER' && (
                        <Button variant="secondary" onClick={() => setErasing(item)}>
                          <TriangleAlert aria-hidden="true" className="size-4 shrink-0" />
                          {t('trash.erase')}
                        </Button>
                      )}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}
      <Pager page={data.page} last={Math.max(1, Math.ceil(data.total / data.pageSize))} onPage={setPage} />
      {erasing && (
        <ConfirmDialog
          title={t('trash.erase')}
          text={t('trash.eraseText')}
          confirm={t('trash.erase')}
          onConfirm={() => run(() => api.request<void>('DELETE', `${base}/contacts/${erasing.id}`), null)}
          onClose={() => setErasing(null)}
        />
      )}
    </>
  );
}
