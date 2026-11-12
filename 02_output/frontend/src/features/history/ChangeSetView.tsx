import { useTranslation } from 'react-i18next';
import { Card } from '@/components/ui/card';
import type { components } from '@/lib/api/schema';
import { formatDateTime } from '@/lib/format';
import type { Language } from '@/lib/i18n';

export type HistoryEntry = components['schemas']['HistoryEntry'];

/** The entries of one save, in the order they were written, are shown together (R-013-07). */
export function changeSets(entries: HistoryEntry[]): HistoryEntry[][] {
  const sets = new Map<string, HistoryEntry[]>();
  for (const entry of entries) {
    sets.set(entry.changeSetId, [...(sets.get(entry.changeSetId) ?? []), entry]);
  }
  return [...sets.values()];
}

/** The number of contacts of an `EXPORTED` entry (R-013-04); undefined when the entry holds none. */
function exportedCount(entry: HistoryEntry): number | undefined {
  const count = Number(entry.changes.find((change) => change.field === 'contacts')?.newValue);
  return Number.isInteger(count) ? count : undefined;
}

/** One entry of one field change as the reader names it: a known field, "tag", an attribute key or the raw name. */
function useFieldLabel() {
  const { t, i18n } = useTranslation();
  return (field: string) => {
    if (field.startsWith('attribute:')) return field.slice('attribute:'.length);
    const key = ['tag', 'name', 'contacts', 'version'].includes(field)
      ? `history.field.${field}`
      : `contact.field.${field}`;
    return i18n.exists(key) ? t(key) : field;
  };
}

function Value({ erased, value }: { erased: boolean; value?: string }) {
  const { t } = useTranslation();
  if (erased) return <span className="font-medium">{t('history.erased')}</span>;
  if (!value) return <span className="text-text-muted">{t('contact.empty')}</span>;
  return <span className="break-words whitespace-pre-wrap">{value}</span>;
}

/** WF-11: the actor, the time, what happened and, per changed field, the old and the new value. */
export function ChangeSetView({ set, showContact }: { set: HistoryEntry[]; showContact?: boolean }) {
  const { t, i18n } = useTranslation();
  const label = useFieldLabel();
  const first = set[0]!;
  return (
    <li>
      <Card className="flex flex-col gap-2">
        <p className="flex flex-wrap items-baseline gap-x-3">
          <span className="font-medium">{first.actor.name}</span>
          {first.actor.email && <span className="text-text-muted">{first.actor.email}</span>}
          <time dateTime={first.createdAt} className="text-text-muted">
            {formatDateTime(first.createdAt, i18n.language as Language)}
          </time>
        </p>
        {set.map((entry) => {
          const exported = entry.eventType === 'EXPORTED' ? exportedCount(entry) : undefined;
          return (
            <div key={entry.id} className="flex flex-col gap-1">
              <p>
                {exported === undefined
                  ? t(`history.event.${entry.eventType}`)
                  : t('history.event.EXPORTED_count', { count: exported })}
                {showContact && (entry.erased || entry.contactLabel) && (
                  <span className="text-text-muted">
                    {' '}
                    · {entry.erased ? t('history.erased') : entry.contactLabel}
                  </span>
                )}
              </p>
              <ul className="flex flex-col gap-1">
                {(entry.eventType === 'CONTACT_CREATED' || exported !== undefined ? [] : entry.changes).map(
                  (change) => (
                    <li key={change.field} className="flex flex-wrap items-baseline gap-x-2">
                      <span className="font-medium">{label(change.field)}</span>
                      <Value erased={entry.erased} value={change.oldValue} />
                      <span aria-hidden="true">→</span>
                      <Value erased={entry.erased} value={change.newValue} />
                    </li>
                  ),
                )}
              </ul>
            </div>
          );
        })}
      </Card>
    </li>
  );
}
