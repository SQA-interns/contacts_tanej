import { useState, type FormEvent } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { useServices } from '@/app/services';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import type { components } from '@/lib/api/schema';

type EventType = components['schemas']['HistoryEventType'];
type Actor = components['schemas']['HistoryActor'];

export interface Filters {
  actorId: string;
  type: EventType | '';
  from: string;
  to: string;
}

export const NO_FILTERS: Filters = { actorId: '', type: '', from: '', to: '' };

const EVENT_TYPES: EventType[] = [
  'CONTACT_CREATED',
  'CONTACT_EDITED',
  'CONTACT_DELETED',
  'CONTACT_RESTORED',
  'CONTACT_ERASED',
  'TAG_CHANGED',
  'ATTRIBUTE_CHANGED',
  'TAG_RENAMED',
  'TAG_DELETED',
  'EXPORTED',
  'MEMBER_INVITED',
  'INVITATION_CANCELLED',
  'ROLE_CHANGED',
  'MEMBER_REMOVED',
  'GROUP_RENAMED',
  'GROUP_DELETED',
  'GROUP_RESTORED',
  'CONTACTS_MERGED',
  'VERSION_RESTORED',
];

const SELECT = 'min-h-11 w-full rounded-control border border-border bg-background px-3 text-base text-text';

interface Props {
  groupId: string;
  applied: Filters;
  onApply: (filters: Filters) => void;
}

/** The filter form of WF-15: person, event type and days; Apply sends them, an invalid range is refused here (EX-014-02). */
export function HistoryFilters({ groupId, applied, onApply }: Props) {
  const { t } = useTranslation();
  const { api } = useServices();
  const [draft, setDraft] = useState<Filters>(applied);
  const [rangeError, setRangeError] = useState(false);
  const actors = useQuery({
    queryKey: ['history-actors', groupId],
    queryFn: () => api.request<Actor[]>('GET', `/groups/${encodeURIComponent(groupId)}/history/actors`),
  });

  const set = (patch: Partial<Filters>) => setDraft((d) => ({ ...d, ...patch }));
  const submit = (event: FormEvent) => {
    event.preventDefault();
    const invalid = draft.from !== '' && draft.to !== '' && draft.to < draft.from;
    setRangeError(invalid);
    if (!invalid) onApply(draft);
  };
  const clear = () => {
    setDraft(NO_FILTERS);
    setRangeError(false);
    onApply(NO_FILTERS);
  };

  return (
    <form onSubmit={submit} className="grid gap-3">
      <div className="flex flex-col gap-1">
        <Label htmlFor="history-actor">{t('history.person')}</Label>
        <select
          id="history-actor"
          className={SELECT}
          value={draft.actorId}
          onChange={(e) => set({ actorId: e.target.value })}
        >
          <option value="">{t('history.everybody')}</option>
          {(actors.data ?? []).map((a) => (
            <option key={a.id} value={a.id}>
              {a.name}
            </option>
          ))}
        </select>
      </div>
      <div className="flex flex-col gap-1">
        <Label htmlFor="history-type">{t('history.eventType')}</Label>
        <select
          id="history-type"
          className={SELECT}
          value={draft.type}
          onChange={(e) => set({ type: e.target.value as EventType | '' })}
        >
          <option value="">{t('history.allEvents')}</option>
          {EVENT_TYPES.map((type) => (
            <option key={type} value={type}>
              {t(`history.event.${type}`)}
            </option>
          ))}
        </select>
      </div>
      <div className="flex flex-col gap-1">
        <Label htmlFor="history-from">{t('history.from')}</Label>
        <Input
          id="history-from"
          type="date"
          value={draft.from}
          aria-invalid={rangeError}
          aria-describedby={rangeError ? 'history-range-error' : undefined}
          onChange={(e) => set({ from: e.target.value })}
        />
      </div>
      <div className="flex flex-col gap-1">
        <Label htmlFor="history-to">{t('history.to')}</Label>
        <Input
          id="history-to"
          type="date"
          value={draft.to}
          aria-invalid={rangeError}
          onChange={(e) => set({ to: e.target.value })}
        />
      </div>
      {rangeError && (
        <p id="history-range-error" role="alert" className="text-danger ">
          {t('history.invalidRange')}
        </p>
      )}
      <div className="flex gap-2">
        <Button type="submit">{t('history.apply')}</Button>
        <Button type="button" variant="ghost" onClick={clear}>
          {t('history.clear')}
        </Button>
      </div>
    </form>
  );
}
