import type { TFunction } from 'i18next';
import type { components } from '@/lib/api/schema';
import { countryName } from '@/lib/format';
import type { Language } from '@/lib/i18n';

export type Condition = components['schemas']['Condition'];
export type ConditionType = Condition['type'];

/** The seven conditions, in the order of the dialog (R-010-01 to R-010-07); no other, no saving (R-010-09). */
export const CONDITION_TYPES: readonly ConditionType[] = [
  'TAGS_ALL',
  'TAGS_ANY',
  'TAG_NOT',
  'ATTRIBUTE_EQUALS',
  'ATTRIBUTE_EXISTS',
  'COUNTRY',
  'COMPANY',
];

/** The text of an active condition on its chip (R-010-11). */
export function conditionText(condition: Condition, t: TFunction, language: Language): string {
  switch (condition.type) {
    case 'TAGS_ALL':
      return t('list.filter.chip.all', { tags: condition.tags.join(', ') });
    case 'TAGS_ANY':
      return t('list.filter.chip.any', { tags: condition.tags.join(', ') });
    case 'TAG_NOT':
      return t('list.filter.chip.not', { tag: condition.tag });
    case 'ATTRIBUTE_EQUALS':
      return t('list.filter.chip.equals', { key: condition.key, value: condition.value });
    case 'ATTRIBUTE_EXISTS':
      return t('list.filter.chip.exists', { key: condition.key });
    case 'COUNTRY':
      return t('list.filter.chip.country', { country: countryName(condition.country, language) });
    case 'COMPANY':
      return t('list.filter.chip.company', { company: condition.company });
  }
}
