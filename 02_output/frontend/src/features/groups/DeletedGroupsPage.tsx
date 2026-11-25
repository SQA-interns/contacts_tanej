import { RotateCcw } from 'lucide-react';
import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { useServices } from '@/app/services';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { StatePanel } from '@/components/ui/state-panel';
import { ApiError } from '@/lib/api/client';
import type { components } from '@/lib/api/schema';
import { formatDateTime } from '@/lib/format';
import { type Language } from '@/lib/i18n';

type DeletedGroup = components['schemas']['DeletedGroup'];

/**
 * WF-18, "Deleted groups" (R-015-04, R-015-05): the groups in Trash of which the person is an
 * Owner, with the days left and "Restore". A name clash (R-015-10) is explained next to the list.
 */
export function DeletedGroupsPage() {
  const { t, i18n } = useTranslation();
  const { api } = useServices();
  const queryClient = useQueryClient();
  const [notice, setNotice] = useState<string | null>(null);
  const [failure, setFailure] = useState<string | null>(null);
  const deleted = useQuery({
    queryKey: ['deleted-groups'],
    queryFn: () => api.request<DeletedGroup[]>('GET', '/deleted-groups'),
  });

  async function restore(group: DeletedGroup) {
    setNotice(null);
    setFailure(null);
    try {
      await api.request<void>('POST', `/groups/${encodeURIComponent(group.id)}/restore`);
      setNotice(t('deletedGroups.restored'));
    } catch (error) {
      const clash = error instanceof ApiError && error.code === 'GROUP_NAME_TAKEN';
      setFailure(t(clash ? 'deletedGroups.nameClash' : 'error.INTERNAL_ERROR'));
    } finally {
      await queryClient.invalidateQueries({ queryKey: ['deleted-groups'] });
      await queryClient.invalidateQueries({ queryKey: ['groups'] });
    }
  }

  if (deleted.isPending) return <StatePanel state="loading" />;
  if (deleted.isError) return <StatePanel state="error" onRetry={() => void deleted.refetch()} />;
  return (
    <>
      <h1 className="mb-2 text-2xl font-semibold">{t('nav.deletedGroups')}</h1>
      <p className="mb-3 text-text-muted">{t('deletedGroups.intro')}</p>
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
      {deleted.data.length === 0 ? (
        <StatePanel state="empty" emptyText={t('deletedGroups.empty')} />
      ) : (
        <Card className="p-0">
          <table className="w-full text-left">
            <thead>
              <tr className="border-b border-border">
                <th scope="col" className="px-4 py-3">
                  {t('groups.columns.name')}
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
            <tbody>
              {deleted.data.map((group) => (
                <tr key={group.id} className="border-b border-border last:border-b-0">
                  <td className="px-4 py-2 break-words">{group.name}</td>
                  <td className="px-4 py-2">{formatDateTime(group.trashedAt, i18n.language as Language)}</td>
                  <td className="px-4 py-2">{group.daysLeft}</td>
                  <td className="px-4 py-2 text-right">
                    <Button variant="secondary" onClick={() => void restore(group)}>
                      <RotateCcw aria-hidden="true" className="size-4 shrink-0" />
                      {t('trash.restore')}
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}
    </>
  );
}
