import { useQueryClient } from '@tanstack/react-query';
import { Trash2 } from 'lucide-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useServices } from '@/app/services';
import { Button } from '@/components/ui/button';
import { useToast } from '@/components/ui/toast';
import { ConfirmDialog } from '@/features/members/ConfirmDialog';
import { ApiError } from '@/lib/api/client';

interface TrashSelectedProps {
  groupId: string;
  /** The number of selected contacts, as the bar shows it. */
  count: number;
  /** The ids of the selection: the ticked ones, or every contact of the filter for "all results". */
  collectIds: () => Promise<string[]>;
  onDone: () => void;
}

const wait = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * Post-run (the developer's wish): "Move to trash" for the ticked contacts of the list, next to
 * Export. The backend trashes one contact per request (R-012-01), so the contacts go one after the
 * other, with the progress shown; a 429 waits and tries again, as the rate limit asks.
 */
export function TrashSelected({ groupId, count, collectIds, onDone }: TrashSelectedProps) {
  const { t } = useTranslation();
  const { api } = useServices();
  const queryClient = useQueryClient();
  const toast = useToast();
  const [asking, setAsking] = useState(false);
  const [progress, setProgress] = useState<{ done: number; total: number } | null>(null);
  const [failed, setFailed] = useState(0);

  async function trashOne(id: string): Promise<boolean> {
    const path = `/groups/${encodeURIComponent(groupId)}/contacts/${encodeURIComponent(id)}/trash`;
    for (let attempt = 0; ; attempt += 1) {
      try {
        await api.request<void>('POST', path);
        return true;
      } catch (error) {
        if (error instanceof ApiError && error.status === 429 && attempt < 5) {
          await wait((error.retryAfter ?? 10) * 1000);
          continue;
        }
        return false;
      }
    }
  }

  async function run() {
    setFailed(0);
    setProgress({ done: 0, total: count });
    let moved = 0;
    let notMoved = 0;
    try {
      const ids = await collectIds();
      setProgress({ done: 0, total: ids.length });
      for (const [index, id] of ids.entries()) {
        if (await trashOne(id)) moved += 1;
        else notMoved += 1;
        setProgress({ done: index + 1, total: ids.length });
      }
    } catch {
      notMoved = Math.max(notMoved, count - moved);
    } finally {
      setProgress(null);
      setFailed(notMoved);
      onDone();
      await queryClient.invalidateQueries({ queryKey: ['contacts', groupId] });
      await queryClient.invalidateQueries({ queryKey: ['trash', groupId] });
      if (moved > 0) toast(t('bulkTrash.done', { count: moved }));
    }
  }

  return (
    <>
      <Button
        variant="secondary"
        disabled={count === 0 || progress !== null}
        busy={progress !== null}
        onClick={() => setAsking(true)}
      >
        {progress === null && <Trash2 aria-hidden="true" className="size-4 shrink-0" />}
        {progress === null ? t('bulkTrash.button') : t('bulkTrash.busy', progress)}
      </Button>
      {failed > 0 && (
        <p role="alert" className="basis-full rounded-control border border-danger p-3 font-semibold">
          {t('bulkTrash.failed', { count: failed })}
        </p>
      )}
      {asking && (
        <ConfirmDialog
          title={t('bulkTrash.button')}
          text={t('bulkTrash.text', { count })}
          confirm={t('bulkTrash.button')}
          onConfirm={() => {
            void run();
            return Promise.resolve();
          }}
          onClose={() => setAsking(false)}
        />
      )}
    </>
  );
}
