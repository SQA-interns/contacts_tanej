import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate } from 'react-router';
import { useServices } from '@/app/services';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { StatePanel } from '@/components/ui/state-panel';
import type { components } from '@/lib/api/schema';
import { EmptyState } from './EmptyState';
import { GroupNameDialog } from './GroupNameDialog';
import { RoleBadge } from './RoleBadge';

type GroupSummary = components['schemas']['GroupSummary'];

type Dialog = { mode: 'create' } | null;

/**
 * WF-05, the address "/" of a verified person: loading, error, the empty state of US-001, or the
 * list of groups with the person's role, "Create group" and, for an Owner, "Rename" (US-002).
 */
export function GroupsHome() {
  const { t } = useTranslation();
  const { api } = useServices();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [dialog, setDialog] = useState<Dialog>(null);
  const groups = useQuery({
    queryKey: ['groups'],
    queryFn: () => api.request<GroupSummary[]>('GET', '/groups'),
  });

  async function create(name: string) {
    const created = await api.request<GroupSummary>('POST', '/groups', { name });
    await queryClient.invalidateQueries({ queryKey: ['groups'] });
    setDialog(null);
    void navigate(`/groups/${created.id}`);
  }

  if (groups.isPending) return <StatePanel state="loading" />;
  if (groups.isError) return <StatePanel state="error" onRetry={() => void groups.refetch()} />;

  const dialogElement =
    dialog?.mode === 'create' ? (
      <GroupNameDialog mode="create" onSubmit={create} onClose={() => setDialog(null)} />
    ) : null;

  if (groups.data.length === 0) {
    return (
      <>
        <EmptyState onCreate={() => setDialog({ mode: 'create' })} />
        {dialogElement}
      </>
    );
  }
  return (
    <>
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold">{t('nav.groups')}</h1>
        <Button onClick={() => setDialog({ mode: 'create' })}>{t('groups.create')}</Button>
      </div>
      <Card className="p-0">
        <table className="w-full text-left">
          <thead>
            <tr className="border-b border-border">
              <th scope="col" className="px-4 py-3">
                {t('groups.columns.name')}
              </th>
              <th scope="col" className="px-4 py-3">
                {t('groups.columns.role')}
              </th>
            </tr>
          </thead>
          <tbody>
            {groups.data.map((group) => (
              <tr key={group.id} className="border-b border-border last:border-b-0">
                <td className="px-4 py-2 break-words">
                  <Link to={`/groups/${group.id}`} className="underline">
                    {group.name}
                  </Link>
                </td>
                <td className="px-4 py-2">
                  <RoleBadge role={group.role} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
      {dialogElement}
    </>
  );
}
