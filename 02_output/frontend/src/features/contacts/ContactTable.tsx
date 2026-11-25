import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import type { components } from '@/lib/api/schema';
import { countryName } from '@/lib/format';
import type { Language } from '@/lib/i18n';
import { cn } from '@/lib/utils';

type ContactRow = components['schemas']['ContactRow'];
export type Sort = components['schemas']['Sort'];
type SortField = components['schemas']['SortField'];

/** The columns of R-009-02; the tags column cannot be sorted. `hideTablet` columns drop out on tablets (WF-08). */
const COLUMNS: { field: SortField | 'tags'; hideTablet?: boolean }[] = [
  { field: 'lastName' },
  { field: 'firstName' },
  { field: 'email' },
  { field: 'phone', hideTablet: true },
  { field: 'company' },
  { field: 'city', hideTablet: true },
  { field: 'country', hideTablet: true },
  { field: 'tags' },
];

interface ContactTableProps {
  rows: ContactRow[];
  groupId: string;
  sort: Sort | undefined;
  onSort: (field: SortField) => void;
  /** Tick boxes of the export selection; absent for a Viewer (R-011-10). */
  ticks?: { isTicked: (id: string) => boolean; onTick: (id: string, checked: boolean) => void };
}

function ariaSort(sort: Sort | undefined, field: string) {
  if (sort?.field !== field) return 'none';
  return sort.direction === 'ASC' ? 'ascending' : 'descending';
}

function arrow(sort: Sort | undefined, field: string) {
  if (sort?.field !== field) return '↕';
  return sort.direction === 'ASC' ? '↑' : '↓';
}

/**
 * WF-08: the list as a table, stacked cards below 768 px (R-009-02). A header is a button that
 * sorts by its column; a click on the sorted column turns the direction. A row opens the contact
 * (R-009-09).
 */
export function ContactTable({ rows, groupId, sort, onSort, ticks }: ContactTableProps) {
  const { t, i18n } = useTranslation();
  const language = i18n.language as Language;

  function value(row: ContactRow, field: SortField | 'tags') {
    if (field === 'tags') {
      return (
        <ul className="flex flex-wrap gap-1">
          {row.tags.map((tag) => (
            <li
              key={tag}
              className="rounded-control border border-border bg-surface px-2 py-0.5 text-sm break-all"
            >
              {tag}
            </li>
          ))}
        </ul>
      );
    }
    const text = row[field] ?? '';
    if (field === 'country') return text && countryName(text, language);
    if (field !== 'lastName') return text;
    const label = text || row.firstName || row.email || row.company || '';
    return (
      <Link to={`/groups/${groupId}/contacts/${row.id}`} className="underline">
        {label}
      </Link>
    );
  }

  return (
    <table className="w-full text-left max-md:block">
      <thead className="max-md:sr-only">
        <tr className="border-b border-border">
          {ticks && (
            <th scope="col" className="w-12 px-3 py-2">
              <span className="sr-only">{t('export.selectColumn')}</span>
            </th>
          )}
          {COLUMNS.map(({ field, hideTablet }) => (
            <th
              key={field}
              scope="col"
              aria-sort={field === 'tags' ? undefined : ariaSort(sort, field)}
              className={cn('px-3 py-2 md:sticky md:top-0 md:bg-background', hideTablet && 'max-lg:hidden')}
            >
              {field === 'tags' ? (
                t('list.column.tags')
              ) : (
                <button
                  type="button"
                  onClick={() => onSort(field)}
                  className="flex min-h-11 w-full items-center gap-1 text-left font-semibold whitespace-nowrap"
                >
                  {t(`list.column.${field}`)}
                  <span aria-hidden="true" className="text-text [font-variant-emoji:text]">
                    {arrow(sort, field)}
                  </span>
                </button>
              )}
            </th>
          ))}
        </tr>
      </thead>
      <tbody className="max-md:block">
        {rows.map((row) => (
          <tr
            key={row.id}
            className="border-b border-border transition-colors md:hover:bg-surface max-md:mb-3 max-md:block max-md:rounded-card max-md:border max-md:px-3 max-md:py-2"
          >
            {ticks && (
              <td className="px-3 py-2">
                <input
                  type="checkbox"
                  className="size-5"
                  aria-label={t('export.selectContact')}
                  checked={ticks.isTicked(row.id)}
                  onChange={(event) => ticks.onTick(row.id, event.target.checked)}
                />
              </td>
            )}
            {COLUMNS.map(({ field, hideTablet }) => (
              <td
                key={field}
                data-label={t(`list.column.${field}`)}
                className={cn(
                  'px-3 py-2 break-words',
                  field === 'phone' && 'whitespace-nowrap',
                  hideTablet && 'max-lg:hidden',
                  'max-md:flex max-md:gap-2 max-md:px-0 max-md:py-0.5 max-md:text-sm max-md:empty:hidden max-md:has-[ul:empty]:hidden max-md:before:min-w-20 max-md:before:font-semibold max-md:before:content-[attr(data-label)]',
                )}
              >
                {value(row, field)}
              </td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  );
}
