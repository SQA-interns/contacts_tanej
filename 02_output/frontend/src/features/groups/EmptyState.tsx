import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';

/**
 * WF-05, state "Empty (first sign-in)": a verified person in no group sees one button (R-001-09).
 * `onCreate` opens the "Create group" dialog, which belongs to US-002.
 */
export function EmptyState({ onCreate }: { onCreate?: () => void }) {
  const { t } = useTranslation();
  return (
    <Card className="mx-auto flex max-w-xl flex-col items-start gap-3">
      <h1 className="text-2xl font-semibold">{t('groups.empty.title')}</h1>
      <p className="text-text-muted">{t('groups.empty.text')}</p>
      <Button onClick={onCreate}>{t('groups.create')}</Button>
    </Card>
  );
}
