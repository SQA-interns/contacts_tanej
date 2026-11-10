import { useQuery } from '@tanstack/react-query';
import { useId, type KeyboardEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { useServices } from '@/app/services';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import type { components } from '@/lib/api/schema';

type Tag = components['schemas']['Tag'];

/** A refused tag of the last save: its place in the submitted list and the code of the broken rule. */
export interface TagProblem {
  index: number | null;
  code: string;
}

interface TagInputProps {
  groupId: string;
  tags: string[];
  typed: string;
  problems: TagProblem[];
  onTags: (tags: string[]) => void;
  onTyped: (text: string) => void;
}

const KNOWN_CODES = ['INVALID_CHARACTERS', 'TOO_LONG', 'TOO_SHORT', 'TOO_MANY'];

/**
 * WF-10, the tag box (R-006-01, R-006-06, R-006-10): chips for the tags, one text box that adds a tag
 * with Enter or a comma, and the tags of the group as suggestions that are never forced. The server
 * decides which names are valid; each refused tag is named with its reason.
 */
export function TagInput({ groupId, tags, typed, problems, onTags, onTyped }: TagInputProps) {
  const { t } = useTranslation();
  const { api } = useServices();
  const id = useId();
  const part = typed.trim();
  const suggestions = useQuery({
    queryKey: ['tags', groupId, part],
    queryFn: () =>
      api.request<Tag[]>('GET', `/groups/${encodeURIComponent(groupId)}/tags?q=${encodeURIComponent(part)}`),
    enabled: part !== '',
    retry: false,
  });

  function add() {
    if (part === '') return;
    onTags([...tags, part]);
    onTyped('');
  }

  function onKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'Enter' || event.key === ',') {
      event.preventDefault();
      add();
    }
  }

  return (
    <div className="flex flex-col gap-1 sm:col-span-2">
      <Label htmlFor={`${id}-tags`}>{t('contact.field.tags')}</Label>
      {tags.length > 0 && (
        <ul aria-label={t('contact.tagsOfContact')} className="flex flex-wrap gap-2">
          {tags.map((tag, index) => (
            <li
              key={`${tag}-${index}`}
              className="flex items-center gap-1 rounded-control border border-border bg-surface-raised pl-3"
            >
              <span className="break-all">{tag}</span>
              <button
                type="button"
                className="min-h-11 min-w-11 rounded-control"
                aria-label={t('contact.removeTag', { tag })}
                onClick={() => onTags(tags.filter((_, i) => i !== index))}
              >
                <span aria-hidden="true">×</span>
              </button>
            </li>
          ))}
        </ul>
      )}
      <Input
        id={`${id}-tags`}
        value={typed}
        list={`${id}-suggestions`}
        autoComplete="off"
        aria-describedby={`${id}-tags-hint`}
        aria-invalid={problems.length > 0 || undefined}
        onChange={(e) => onTyped(e.target.value)}
        onKeyDown={onKeyDown}
      />
      <datalist id={`${id}-suggestions`}>
        {(suggestions.data ?? []).map((tag) => (
          <option key={tag.id} value={tag.name} />
        ))}
      </datalist>
      <p id={`${id}-tags-hint`} className="text-sm text-text-muted">
        {t('contact.tagsHint')}
      </p>
      {problems.map((problem, n) => (
        <p key={n} role="alert" className="text-sm text-danger">
          {problem.index !== null && tags[problem.index] !== undefined && (
            <>
              <strong>{tags[problem.index]}</strong>{' '}
            </>
          )}
          <span>{t(`contact.tagError.${KNOWN_CODES.includes(problem.code) ? problem.code : 'OTHER'}`)}</span>
        </p>
      ))}
    </div>
  );
}

/** The tag problems of a 400: field `tags[2]` is the third tag, field `tags` is the whole list. */
export function tagProblems(refused: Partial<Record<string, string>>): TagProblem[] {
  return Object.entries(refused).flatMap(([field, code]) => {
    const match = /^tags(?:\[(\d+)\])?$/.exec(field);
    return match && code ? [{ index: match[1] === undefined ? null : Number(match[1]), code }] : [];
  });
}
