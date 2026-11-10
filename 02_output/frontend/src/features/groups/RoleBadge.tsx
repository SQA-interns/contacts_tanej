import { useTranslation } from 'react-i18next';
import type { components } from '@/lib/api/schema';

/** The role of the person in a group as a small badge with the name of the role (WF-05). */
export function RoleBadge({ role }: { role: components['schemas']['Role'] }) {
  const { t } = useTranslation();
  return (
    <span className="inline-block rounded-control border border-border bg-surface px-2 py-0.5 text-sm">
      {t(`role.${role}`)}
    </span>
  );
}
