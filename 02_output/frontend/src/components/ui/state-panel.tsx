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
      <div role="alert" className="flex flex-col items-start gap-3">
        <p>{t('state.error')}</p>
        {onRetry && (
          <Button variant="secondary" onClick={onRetry}>
            {t('state.retry')}
          </Button>
        )}
      </div>
    );
  }
  return (
    <div className="flex flex-col items-start gap-3">
      <p className="text-text-muted">{emptyText ?? t('state.empty')}</p>
      {action}
    </div>
  );
}
