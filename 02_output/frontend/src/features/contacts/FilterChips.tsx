import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/button';
import type { Language } from '@/lib/i18n';
import { conditionText, type Condition } from './conditions';

interface FilterChipsProps {
  conditions: Condition[];
  onRemove: (index: number) => void;
  onClear: () => void;
}

/**
 * WF-09, the active conditions as chips (R-010-11): each can be removed, and one button removes
 * them all (R-010-10). Its name is "Clear all" so that "Filter" names one button only.
 */
export function FilterChips({ conditions, onRemove, onClear }: FilterChipsProps) {
  const { t, i18n } = useTranslation();
  if (conditions.length === 0) return null;
  return (
    <div className="flex flex-wrap items-center gap-2">
      <span className="text-sm text-text-muted">{t('list.filter.active')}</span>
      <ul className="flex flex-wrap gap-2">
        {conditions.map((condition, index) => (
          <li
            key={`${condition.type}-${index}`}
            className="flex items-center gap-1 rounded-control border border-border bg-surface-raised pl-3"
          >
            <span className="break-all">{conditionText(condition, t, i18n.language as Language)}</span>
            <button
              type="button"
              className="min-h-11 min-w-11 rounded-control"
              aria-label={t('list.filter.remove')}
              onClick={() => onRemove(index)}
            >
              <span aria-hidden="true">×</span>
            </button>
          </li>
        ))}
      </ul>
      <Button variant="ghost" onClick={onClear}>
        {t('list.filter.clearAll')}
      </Button>
    </div>
  );
}
