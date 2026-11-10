import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { useOutletContext } from 'react-router';
import { useServices } from '@/app/services';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { StatePanel } from '@/components/ui/state-panel';
import { ConfirmDialog } from '@/features/members/ConfirmDialog';
import type { components } from '@/lib/api/schema';
import { RenameTagDialog } from './RenameTagDialog';

type GroupSummary = components['schemas']['GroupSummary'];
type Tag = components['schemas']['Tag'];

type Pending = { kind: 'rename'; tag: Tag } | { kind: 'delete'; tag: Tag } | null;

/**
 * WF-13, Manage tags (US-007): the tags of the group with their contact counts; Owner and Editor
 * rename or delete a tag for the whole group (R-007-01, R-007-05). A Viewer sees no control (R-007-07).
 */
export function ManageTagsPage() {
  const { t } = useTranslation();
  const { api } = useServices();
  const group = useOutletContext<GroupSummary>();
  const queryClient = useQueryClient();
  const [pending, setPending] = useState<Pending>(null);
  const canEdit = group.role !== 'VIEWER';
  const base = `/groups/${encodeURIComponent(group.id)}/tags`;
  const tags = useQuery({
    queryKey: ['tags', group.id, 'all'],
    queryFn: () => api.request<Tag[]>('GET', base),
  });

  async function refresh() {
    await queryClient.invalidateQueries({ queryKey: ['tags', group.id] });
    await queryClient.invalidateQueries({ queryKey: ['contact', group.id] });
  }

  async function rename(tag: Tag, name: string) {
    await api.request<Tag>('PATCH', `${base}/${encodeURIComponent(tag.id)}`, { name });
    setPending(null);
    await refresh();
  }

  async function remove(tag: Tag) {
    try {
      await api.request<void>('DELETE', `${base}/${encodeURIComponent(tag.id)}`);
    } finally {
      await refresh();
    }
  }

  if (tags.isPending) return <StatePanel state="loading" />;
  if (tags.isError) return <StatePanel state="error" onRetry={() => void tags.refetch()} />;
  if (tags.data.length === 0) return <StatePanel state="empty" emptyText={t('tags.empty')} />;
  return (
    <Card aria-labelledby="tags-heading" role="region" className="p-0">
      <h2 id="tags-heading" className="px-4 pt-4 text-xl font-semibold">
        {t('groups.tabs.tags')}
      </h2>
      <table className="w-full text-left">
        <thead>
          <tr className="border-b border-border">
            <th scope="col" className="px-4 py-3">
              {t('tags.column')}
            </th>
            <th scope="col" className="px-4 py-3">
              {t('tags.contacts')}
            </th>
            <th scope="col" className="px-4 py-3">
              <span className="sr-only">{t('groups.columns.actions')}</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {tags.data.map((tag) => (
            <tr key={tag.id} className="border-b border-border last:border-b-0">
              <td className="px-4 py-2 break-all">{tag.name}</td>
              <td className="px-4 py-2">{tag.contactCount}</td>
              <td className="px-4 py-2">
                {canEdit && (
                  <span className="flex flex-wrap justify-end gap-2">
                    <Button variant="secondary" onClick={() => setPending({ kind: 'rename', tag })}>
                      {t('tags.rename')}
                    </Button>
                    <Button variant="secondary" onClick={() => setPending({ kind: 'delete', tag })}>
                      {t('tags.delete')}
                    </Button>
                  </span>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {pending?.kind === 'rename' && (
        <RenameTagDialog
          name={pending.tag.name}
          onSubmit={(name) => rename(pending.tag, name)}
          onClose={() => setPending(null)}
        />
      )}
      {pending?.kind === 'delete' && (
        <ConfirmDialog
          title={t('tags.deleteTitle')}
          text={t('tags.deleteText')}
          confirm={t('tags.deleteConfirm')}
          onConfirm={() => remove(pending.tag)}
          onClose={() => setPending(null)}
        />
      )}
    </Card>
  );
}
