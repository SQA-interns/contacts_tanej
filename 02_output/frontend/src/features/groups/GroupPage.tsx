import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Link, NavLink, Outlet, useParams } from 'react-router';
import { useServices } from '@/app/services';
import { Card } from '@/components/ui/card';
import { StatePanel } from '@/components/ui/state-panel';
import { ApiError } from '@/lib/api/client';
import type { components } from '@/lib/api/schema';
import { cn } from '@/lib/utils';
import { RoleBadge } from './RoleBadge';

type GroupSummary = components['schemas']['GroupSummary'];

/** WF-06, state "Group not found": the same screen for a missing group and one of someone else (R-002-07). */
function GroupNotFound() {
  const { t } = useTranslation();
  return (
    <Card className="mx-auto flex max-w-xl flex-col items-start gap-3">
      <h1 className="text-2xl font-semibold">{t('groups.notFound.title')}</h1>
      <p className="text-text-muted">{t('groups.notFound.text')}</p>
      <Link to="/" className="underline">
        {t('notFound.back')}
      </Link>
    </Card>
  );
}

function GroupTab({ to, end, label }: { to: string; end?: boolean; label: string }) {
  return (
    <NavLink
      to={to}
      end={end}
      className={({ isActive }) =>
        cn(
          'flex min-h-11 items-center px-3',
          isActive ? 'border-b-2 border-primary font-semibold' : 'hover:bg-surface-raised',
        )
      }
    >
      {label}
    </NavLink>
  );
}

/**
 * The page of one group: its name, the person's role and the tabs of the group (R-002-06). The
 * tabs are the routes below it: Contacts and, from US-003, Members; the later stories add theirs.
 * The group id comes from the address only (AR-04).
 */
export function GroupPage() {
  const { t } = useTranslation();
  const { api } = useServices();
  const { groupId = '' } = useParams();
  const group = useQuery({
    queryKey: ['group', groupId],
    queryFn: () => api.request<GroupSummary>('GET', `/groups/${encodeURIComponent(groupId)}`),
    retry: false,
  });

  if (group.isPending) return <StatePanel state="loading" />;
  if (group.isError) {
    if (group.error instanceof ApiError && group.error.status === 404) return <GroupNotFound />;
    return <StatePanel state="error" onRetry={() => void group.refetch()} />;
  }
  return (
    <>
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <h1 className="text-2xl font-semibold break-words">{group.data.name}</h1>
        <RoleBadge role={group.data.role} />
      </div>
      <nav aria-label={t('groups.tabs.label')} className="mb-4 flex gap-1 border-b border-border">
        <GroupTab to={`/groups/${group.data.id}`} end label={t('groups.tabs.contacts')} />
        {group.data.role !== 'VIEWER' && (
          <GroupTab to={`/groups/${group.data.id}/tags`} label={t('groups.tabs.tags')} />
        )}
        <GroupTab to={`/groups/${group.data.id}/members`} label={t('groups.tabs.members')} />
      </nav>
      <Outlet context={group.data} />
    </>
  );
}
