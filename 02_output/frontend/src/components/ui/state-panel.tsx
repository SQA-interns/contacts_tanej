import { CircleAlert, Inbox } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Button } from './button';
import { Skeleton } from './skeleton';

/** The loading, empty and error states of UI-14; every screen that loads data uses it. */
export type PanelState = 'loading' | 'empty' | 'error';

interface StatePanelProps {
  state: PanelState;
  /** Text of the empty state (short explanation); defaults to the generic one. */
  emptyText?: string;
  /** The next action of the empty state, for example "Add contact". */
  action?: React.ReactNode;
  onRetry?: () => void;
}

export function StatePanel({ state, emptyText, action, onRetry }: StatePanelProps) {
  const { t } = useTranslation();
  if (state === 'loading') {
    return (
      <div role="status" aria-live="polite" className="flex flex-col gap-3">
        <span className="sr-only">{t('state.loading')}</span>
        <Skeleton className="w-2/3" />
        <Skeleton className="w-full" />
        <Skeleton className="w-1/2" />
      </div>
    );
  }
  if (state === 'error') {
    return (
      <div
        role="alert"
        className="flex flex-col items-start gap-3 rounded-card border border-danger/40 bg-danger/10 p-4"
      >
        <p className="flex items-center gap-2">
          <CircleAlert aria-hidden="true" className="size-5 shrink-0 text-danger" />
          {t('state.error')}
        </p>
        {onRetry && (
          <Button variant="secondary" onClick={onRetry}>
            {t('state.retry')}
          </Button>
        )}
      </div>
    );
  }
  return (
    <div className="flex flex-col items-start gap-3 rounded-card border border-dashed border-border bg-surface p-6">
      <p className="flex items-center gap-2 text-text-muted">
        <Inbox aria-hidden="true" className="size-5 shrink-0" />
        {emptyText ?? t('state.empty')}
      </p>
      {action}
    </div>
  );
}
