/**
 * Class names for a table that becomes one card per row below 768 px (UI-22, NFR-08; WF-05 mobile).
 * Give each value cell a `data-label` with its column name; a cell without one (the actions) gets no
 * label. The header row stays for screen readers only.
 */
export const stacked = {
  table: 'w-full text-left max-md:block',
  head: 'max-md:sr-only',
  body: 'max-md:block',
  row: 'border-b border-border last:border-b-0 max-md:mb-3 max-md:block max-md:rounded-card max-md:border max-md:p-3 max-md:last:border-b',
  cell: 'px-4 py-2 break-words max-md:flex max-md:items-center max-md:gap-2 max-md:px-0 max-md:py-1 max-md:before:min-w-24 max-md:before:shrink-0 max-md:before:font-semibold max-md:before:content-[attr(data-label)]',
  actions: 'px-4 py-2 text-right max-md:block max-md:px-0 max-md:py-1 max-md:text-left',
} as const;
