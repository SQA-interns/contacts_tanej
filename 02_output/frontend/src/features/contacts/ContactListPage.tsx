import { useEffect, useState } from 'react';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { useNavigate, useOutletContext } from 'react-router';
import { useServices } from '@/app/services';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { StatePanel } from '@/components/ui/state-panel';
import type { components } from '@/lib/api/schema';
import { ExportBar } from '@/features/export/ExportBar';
import { useExport } from '@/features/export/useExport';
import { useSelection } from '@/features/export/useSelection';
import { type Condition } from './conditions';
import { ContactTable, type Sort } from './ContactTable';
import { FilterChips } from './FilterChips';
import { FilterDialog } from './FilterDialog';
import { Pager } from './Pager';

type GroupSummary = components['schemas']['GroupSummary'];
type ContactPage = components['schemas']['ContactPage'];
type SortField = components['schemas']['SortField'];

const TYPING_PAUSE_MS = 250;

/** The search text as the request sees it: it follows the box after a short pause (R-009-03). */
function useDebounced(value: string): string {
  const [settled, setSettled] = useState(value);
  useEffect(() => {
    const timer = setTimeout(() => setSettled(value), TYPING_PAUSE_MS);
    return () => clearTimeout(timer);
  }, [value]);
  return settled;
}

/**
 * WF-08, the Contacts tab (US-009): one search box, sortable columns, 25 rows per page and the
 * total. Search text and sort stay while paging; a new search text or sort goes back to page 1
 * only where the rules say so (R-009-08). A Viewer sees the same list (R-009-10).
 */
export function ContactListPage() {
  const { t, i18n } = useTranslation();
  const { api } = useServices();
  const group = useOutletContext<GroupSummary>();
  const navigate = useNavigate();
  const [text, setText] = useState('');
  const [sort, setSort] = useState<Sort | undefined>();
  const [page, setPage] = useState(1);
  const [conditions, setConditions] = useState<Condition[]>([]);
  const [filtering, setFiltering] = useState(false);
  const q = useDebounced(text);
  const canEdit = group.role !== 'VIEWER';
  const selection = useSelection();
  const exporter = useExport(group.id);

  const list = useQuery({
    queryKey: ['contacts', group.id, { q, conditions, sort, page }],
    queryFn: () =>
      api.request<ContactPage>('POST', `/groups/${encodeURIComponent(group.id)}/contacts/search`, {
        q,
        sort,
        page,
        conditions,
      }),
    placeholderData: keepPreviousData,
  });

  function changeText(value: string) {
    setText(value);
    setPage(1);
    selection.clear();
  }

  function changeSort(field: SortField) {
    const direction = sort?.field === field && sort.direction === 'ASC' ? 'DESC' : 'ASC';
    setSort({ field, direction });
  }

  function clearFilters() {
    setText('');
    setConditions([]);
    setPage(1);
    selection.clear();
  }

  function changeConditions(next: Condition[]) {
    setConditions(next);
    setPage(1);
    selection.clear();
  }

  /** Ticked ids as ids, or the current filter for "select all N results" (R-011-04). */
  function exportSelection() {
    const language = i18n.language === 'sl' ? 'sl' : 'en';
    void exporter.run(
      selection.all
        ? { language, sort, filter: { q, conditions } }
        : { language, sort, contactIds: [...selection.ids] },
    );
  }

  const addContact = canEdit && (
    <Button onClick={() => navigate(`/groups/${group.id}/contacts/new`)}>{t('contact.add')}</Button>
  );
  const searching = q.trim() !== '' || text.trim() !== '' || conditions.length > 0;

  let body;
  let pageIds: string[] = [];
  if (list.isPending) body = <StatePanel state="loading" />;
  else if (list.isError) body = <StatePanel state="error" onRetry={() => void list.refetch()} />;
  else if (list.data.total === 0 && !searching) {
    body = (
      <Card className="flex flex-col items-start gap-2">
        <h2 className="text-xl font-semibold">{t('groups.noContacts.title')}</h2>
        <p className="text-text-muted">{t(canEdit ? 'list.emptyEditor' : 'list.emptyViewer')}</p>
        {addContact}
      </Card>
    );
  } else if (list.data.total === 0) {
    body = (
      <Card className="flex flex-col items-start gap-2">
        <h2 className="text-xl font-semibold">{t('list.noMatch.title')}</h2>
        <p className="text-text-muted">{t('list.noMatch.text')}</p>
        <Button variant="secondary" onClick={clearFilters}>
          {t('list.clearFilters')}
        </Button>
      </Card>
    );
  } else {
    const data = list.data;
    pageIds = data.items.map((row) => row.id);
    body = (
      <>
        <p role="status" className="mb-2 text-text-muted">
          {data.total === 1 ? t('list.totalOne') : t('list.total', { total: data.total })}
        </p>
        {canEdit && (
          <ExportBar
            total={data.total}
            count={selection.all ? data.total : selection.ids.size}
            pageAllTicked={pageIds.length > 0 && pageIds.every((id) => selection.ids.has(id))}
            allResults={selection.all}
            state={exporter.state}
            onTogglePage={(checked) => selection.set(pageIds, checked, pageIds)}
            onSelectAll={selection.selectAll}
            onExport={exportSelection}
          />
        )}
        <ContactTable
          rows={data.items}
          groupId={group.id}
          sort={sort}
          onSort={changeSort}
          ticks={
            canEdit
              ? {
                  isTicked: (id) => selection.all || selection.ids.has(id),
                  onTick: (id, checked) => selection.set([id], checked, pageIds),
                }
              : undefined
          }
        />
        <Pager page={data.page} last={Math.ceil(data.total / data.pageSize)} onPage={setPage} />
      </>
    );
  }

  return (
    <div className="flex flex-col gap-3">
      <div role="search" className="flex flex-wrap items-end gap-3">
        <div className="flex min-w-60 grow flex-col gap-1">
          <Label htmlFor="contact-search">{t('list.search')}</Label>
          <Input
            id="contact-search"
            type="search"
            value={text}
            placeholder={t('list.searchHint')}
            onChange={(event) => changeText(event.target.value)}
          />
        </div>
        <Button variant="secondary" onClick={() => setFiltering(true)}>
          {t('list.filter.button')}
        </Button>
        {canEdit && list.data && list.data.total > 0 && addContact}
      </div>
      <FilterChips
        conditions={conditions}
        onRemove={(index) => changeConditions(conditions.filter((_, i) => i !== index))}
        onClear={clearFilters}
      />
      {body}
      {filtering && (
        <FilterDialog
          groupId={group.id}
          onClose={() => setFiltering(false)}
          onAdd={(condition) => {
            changeConditions([...conditions, condition]);
            setFiltering(false);
          }}
        />
      )}
    </div>
  );
}
