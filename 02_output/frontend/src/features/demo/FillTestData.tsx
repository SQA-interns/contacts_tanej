import { useQueryClient } from '@tanstack/react-query';
import { DatabaseZap } from 'lucide-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useServices } from '@/app/services';
import { Button } from '@/components/ui/button';
import { ConfirmDialog } from '@/features/members/ConfirmDialog';
import { ApiError } from '@/lib/api/client';
import { sampleContacts } from './sampleContacts';

const wait = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * "Fill with test data" (post-run, demo deployments only): adds the fifty sample contacts to the
 * group through the normal API, one by one, so history, versions and every rule apply as for a person
 * typing them. Shown only when the runtime configuration sets demoTools (never in a customer
 * installation). Waits and retries when the rate limit answers 429.
 */
export function FillTestData({ groupId }: { groupId: string }) {
  const { t } = useTranslation();
  const { api } = useServices();
  const queryClient = useQueryClient();
  const [asking, setAsking] = useState(false);
  const [state, setState] = useState<{ done: number; total: number; failed: number } | null>(null);

  async function fill() {
    const contacts = sampleContacts();
    let failed = 0;
    for (const [index, contact] of contacts.entries()) {
      setState({ done: index, total: contacts.length, failed });
      for (let attempt = 0; ; attempt++) {
        try {
          await api.request('POST', `/groups/${encodeURIComponent(groupId)}/contacts`, contact);
          break;
        } catch (error) {
          if (error instanceof ApiError && error.status === 429 && attempt < 5) {
            await wait((error.retryAfter ?? 10) * 1000);
            continue;
          }
          failed++;
          break;
        }
      }
    }
    setState({ done: contacts.length, total: contacts.length, failed });
    await queryClient.invalidateQueries({ queryKey: ['contacts', groupId] });
  }

  const running = state !== null && state.done < state.total;
  return (
    <>
      <Button variant="secondary" disabled={running} onClick={() => setAsking(true)}>
        <DatabaseZap aria-hidden="true" className="size-4 shrink-0" />
        {running ? t('demo.progress', { done: state.done, total: state.total }) : t('demo.fill')}
      </Button>
      {state && !running && (
        <p role="status" className="basis-full text-sm text-text-muted">
          {state.failed === 0
            ? t('demo.done', { count: state.total })
            : t('demo.partly', { count: state.total - state.failed, failed: state.failed })}
        </p>
      )}
      {asking && (
        <ConfirmDialog
          title={t('demo.confirmTitle')}
          text={t('demo.confirmText')}
          confirm={t('demo.confirm')}
          onConfirm={async () => {
            setAsking(false);
            void fill();
          }}
          onClose={() => setAsking(false)}
        />
      )}
    </>
  );
}
