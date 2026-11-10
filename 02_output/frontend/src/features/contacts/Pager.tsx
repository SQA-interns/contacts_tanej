import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/button';

/** The page numbers shown: the first, the last and two on each side of the current one. */
export function pageNumbers(page: number, last: number): number[] {
  const wanted = new Set([1, last, ...[-2, -1, 0, 1, 2].map((d) => page + d)]);
  return [...wanted].filter((n) => n >= 1 && n <= last).sort((a, b) => a - b);
}

interface PagerProps {
  page: number;
  last: number;
  onPage: (page: number) => void;
}

/** Paging of the list (R-009-01): numbered buttons with the current page marked. */
export function Pager({ page, last, onPage }: PagerProps) {
  const { t } = useTranslation();
  if (last <= 1) return null;
  return (
    <nav aria-label={t('list.pages')} className="mt-4 flex flex-wrap items-center gap-1">
      <Button variant="ghost" disabled={page <= 1} onClick={() => onPage(page - 1)}>
        {t('list.previous')}
      </Button>
      {pageNumbers(page, last).map((n) => (
        <Button
          key={n}
          variant={n === page ? 'primary' : 'secondary'}
          aria-current={n === page ? 'page' : undefined}
          onClick={() => onPage(n)}
        >
          {n}
        </Button>
      ))}
      <Button variant="ghost" disabled={page >= last} onClick={() => onPage(page + 1)}>
        {t('list.next')}
      </Button>
    </nav>
  );
}
