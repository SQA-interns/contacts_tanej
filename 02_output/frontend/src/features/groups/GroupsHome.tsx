import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { useServices } from '@/app/services';
import { StatePanel } from '@/components/ui/state-panel';
import type { components } from '@/lib/api/schema';
import { EmptyState } from './EmptyState';

type GroupSummary = components['schemas']['GroupSummary'];

/**
 * The address "/" of a verified person: loading, error, the empty state of US-001, or the groups.
 * The list itself is a plain list until US-002 builds the screen.
 */
export function GroupsHome() {
  const { t } = useTranslation();
  const { api } = useServices();
  const groups = useQuery({
    queryKey: ['groups'],
    queryFn: () => api.request<GroupSummary[]>('GET', '/groups'),
  });

  if (groups.isPending) return <StatePanel state="loading" />;
  if (groups.isError) return <StatePanel state="error" onRetry={() => void groups.refetch()} />;
  if (groups.data.length === 0) return <EmptyState />;
  return (
    <>
      <h1 className="mb-4 text-2xl font-semibold">{t('nav.groups')}</h1>
      <ul className="flex flex-col gap-2">
        {groups.data.map((group) => (
          <li key={group.id}>{group.name}</li>
        ))}
      </ul>
    </>
  );
}
