import { useQuery } from '@tanstack/react-query';
import { useId, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useServices } from '@/app/services';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import type { components } from '@/lib/api/schema';

export type Attribute = components['schemas']['Attribute'];

/** A refused attribute of the last save: its place in the submitted list, the part and the rule code. */
export interface AttributeProblem {
  index: number | null;
  code: string;
}

interface AttributeRowsProps {
  groupId: string;
  rows: Attribute[];
  problems: AttributeProblem[];
  onRows: (rows: Attribute[]) => void;
}

const KNOWN_CODES = ['REQUIRED', 'TOO_LONG', 'DUPLICATE_KEY', 'TOO_MANY'];

/**
 * WF-10, the attributes (R-008-01, R-008-05, R-008-07): one row of key and value per attribute, a
 * button that adds a row, a button that removes one, and the keys of the group as suggestions that
 * are never forced. The server decides which rows are valid; each refused row shows its reason.
 */
export function AttributeRows({ groupId, rows, problems, onRows }: AttributeRowsProps) {
  const { t } = useTranslation();
  const change = (index: number, next: Partial<Attribute>) =>
    onRows(rows.map((row, i) => (i === index ? { ...row, ...next } : row)));
  return (
    <fieldset className="flex flex-col gap-2 sm:col-span-2">
      <legend className="mb-1 text-sm font-medium">{t('contact.field.attributes')}</legend>
      {rows.map((row, index) => (
        <AttributeRow
          key={index}
          groupId={groupId}
          row={row}
          problems={problems.filter((p) => p.index === index)}
          onChange={(next) => change(index, next)}
          onRemove={() => onRows(rows.filter((_, i) => i !== index))}
        />
      ))}
      {problems
        .filter((p) => p.index === null)
        .map((problem, n) => (
          <p key={n} role="alert" className="text-sm text-danger">
            {reason(t, problem.code)}
          </p>
        ))}
      <div>
        <Button type="button" variant="secondary" onClick={() => onRows([...rows, { key: '', value: '' }])}>
          {t('contact.addAttribute')}
        </Button>
      </div>
    </fieldset>
  );
}

function reason(t: (key: string) => string, code: string): string {
  return t(`contact.attributeError.${KNOWN_CODES.includes(code) ? code : 'OTHER'}`);
}

interface AttributeRowProps {
  groupId: string;
  row: Attribute;
  problems: AttributeProblem[];
  onChange: (next: Partial<Attribute>) => void;
  onRemove: () => void;
}

function AttributeRow({ groupId, row, problems, onChange, onRemove }: AttributeRowProps) {
  const { t } = useTranslation();
  const { api } = useServices();
  const id = useId();
  const [open, setOpen] = useState(false);
  const part = row.key.trim();
  const suggestions = useQuery({
    queryKey: ['attribute-keys', groupId, part],
    queryFn: () =>
      api.request<string[]>(
        'GET',
        `/groups/${encodeURIComponent(groupId)}/attribute-keys?q=${encodeURIComponent(part)}`,
      ),
    enabled: part !== '',
    retry: false,
  });
  const keys = suggestions.data ?? [];
  const shown = open && keys.length > 0;
  return (
    <div className="flex flex-col gap-1">
      <div className="grid grid-cols-[1fr_1fr_auto] items-end gap-2">
        <div className="flex flex-col gap-1">
          <Label htmlFor={`${id}-key`}>{t('contact.attributeKey')}</Label>
          <div className="relative">
            <Input
              id={`${id}-key`}
              value={row.key}
              role="combobox"
              aria-expanded={shown}
              aria-controls={shown ? `${id}-keys` : undefined}
              aria-autocomplete="list"
              autoComplete="off"
              aria-invalid={problems.length > 0 || undefined}
              onChange={(e) => {
                setOpen(true);
                onChange({ key: e.target.value });
              }}
              onFocus={() => setOpen(true)}
              onBlur={() => setOpen(false)}
              onKeyDown={(e) => {
                if (e.key === 'Escape') setOpen(false);
              }}
            />
            {shown && (
              <ul
                id={`${id}-keys`}
                role="listbox"
                className="absolute z-10 mt-1 max-h-48 w-full overflow-auto rounded-control border border-border bg-background shadow"
              >
                {keys.map((key) => (
                  <li
                    key={key}
                    role="option"
                    aria-selected={key === row.key}
                    className="min-h-11 cursor-pointer px-3 py-2 hover:bg-surface-raised"
                    onMouseDown={(e) => {
                      e.preventDefault();
                      onChange({ key });
                      setOpen(false);
                    }}
                  >
                    {key}
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
        <div className="flex flex-col gap-1">
          <Label htmlFor={`${id}-value`}>{t('contact.attributeValue')}</Label>
          <Input
            id={`${id}-value`}
            value={row.value}
            autoComplete="off"
            onChange={(e) => onChange({ value: e.target.value })}
          />
        </div>
        <button
          type="button"
          className="min-h-11 min-w-11 rounded-control border border-border"
          aria-label={t('contact.removeAttribute', { key: row.key })}
          onClick={onRemove}
        >
          <span aria-hidden="true">×</span>
        </button>
      </div>
      {problems.map((problem, n) => (
        <p key={n} role="alert" className="text-sm text-danger">
          {reason(t, problem.code)}
        </p>
      ))}
    </div>
  );
}

/** The attribute problems of a 400: `attributes[2].key` is the third row, `attributes` the whole list. */
export function attributeProblems(refused: Partial<Record<string, string>>): AttributeProblem[] {
  return Object.entries(refused).flatMap(([field, code]) => {
    const match = /^attributes(?:\[(\d+)\](?:\.(?:key|value))?)?$/.exec(field);
    return match && code ? [{ index: match[1] === undefined ? null : Number(match[1]), code }] : [];
  });
}
