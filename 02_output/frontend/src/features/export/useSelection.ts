import { useCallback, useState } from 'react';

/**
 * The ticked contacts of the list (R-011-01, R-011-02). It survives paging and sorting; the list
 * clears it when the search text or a filter changes. "All results" means every contact of the
 * current filter, not only the ones loaded.
 */
export function useSelection() {
  const [ids, setIds] = useState<ReadonlySet<string>>(new Set());
  const [all, setAll] = useState(false);

  const clear = useCallback(() => {
    setIds(new Set());
    setAll(false);
  }, []);

  /** Ticks or unticks some rows; unticking one row leaves "all results" and keeps the rest of the page. */
  const set = useCallback(
    (rowIds: string[], checked: boolean, pageIds: string[]) => {
      const base = all ? new Set(pageIds) : new Set(ids);
      for (const id of rowIds) {
        if (checked) base.add(id);
        else base.delete(id);
      }
      setIds(base);
      setAll(false);
    },
    [all, ids],
  );

  const selectAll = useCallback(() => setAll(true), []);

  return { ids, all, set, selectAll, clear };
}

export type SelectionState = ReturnType<typeof useSelection>;
