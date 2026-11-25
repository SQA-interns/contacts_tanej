import { Download } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/button';
import type { ExportState } from './useExport';

interface ExportBarProps {
  /** The number of contacts of the current filter. */
  total: number;
  /** The number of ticked contacts. */
  count: number;
  pageAllTicked: boolean;
  allResults: boolean;
  state: ExportState;
  onTogglePage: (checked: boolean) => void;
  onSelectAll: () => void;
  onExport: () => void;
}

/** WF-08: select all on this page, select all results, the count and the Export button (R-011-01, R-011-03). */
export function ExportBar(props: ExportBarProps) {
  const { total, count, pageAllTicked, allResults, state, onTogglePage, onSelectAll, onExport } = props;
  const { t } = useTranslation();
  return (
    <div className="flex flex-col gap-2">
      <div className="flex flex-wrap items-center gap-3">
        <label className="flex min-h-11 items-center gap-2">
          <input
            type="checkbox"
            className="size-5"
            aria-label={t('export.selectPage')}
            checked={pageAllTicked || allResults}
            onChange={(event) => onTogglePage(event.target.checked)}
          />
          <span className="text-sm">{t('export.selectPage')}</span>
        </label>
        {!allResults && (
          <Button variant="ghost" onClick={onSelectAll}>
            {t('export.selectAll', { total })}
          </Button>
        )}
        <span role="status" className="text-text-muted">
          {count === 0 ? t('export.none') : t('export.selected', { count })}
        </span>
        <span className="grow" />
        <Button variant="secondary" disabled={count === 0} busy={state === 'busy'} onClick={onExport}>
          {state !== 'busy' && <Download aria-hidden="true" className="size-4 shrink-0" />}
          {state === 'busy' ? t('export.busy') : t('export.button')}
        </Button>
      </div>
      {state === 'tooMany' && (
        <p role="alert" className="rounded-control border border-danger p-3 font-semibold">
          {t('export.tooMany')}
        </p>
      )}
      {state === 'error' && (
        <p role="alert" className="rounded-control border border-danger p-3 font-semibold">
          {t('export.error')}
        </p>
      )}
      {state === 'done' && <p role="status">{t('export.done')}</p>}
    </div>
  );
}
