import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/button';
import { ContactDetails } from './ContactDetails';
import type { Values } from './contactValues';

interface ConflictPanelProps {
  current: Values;
  mine: Values;
  onReload: () => void;
  onCancel: () => void;
}

/**
 * WF-10, the state "Conflict": the save was refused because someone else changed the contact
 * (R-005-08). The current values and the user's own entries are shown side by side; nothing was
 * overwritten, and the user chooses to continue from the current values.
 */
export function ConflictPanel({ current, mine, onReload, onCancel }: ConflictPanelProps) {
  const { t } = useTranslation();
  return (
    <div className="flex flex-col gap-4">
      <div role="alert" className="rounded-card border border-danger bg-surface-raised p-4">
        <strong>{t('contact.conflict.title')}</strong> {t('contact.conflict.text')}
      </div>
      <div className="grid gap-4 lg:grid-cols-2">
        <ContactDetails title={t('contact.conflict.current')} values={current} />
        <ContactDetails title={t('contact.conflict.mine')} values={mine} />
      </div>
      <div className="flex gap-3">
        <Button onClick={onReload}>{t('contact.conflict.reload')}</Button>
        <Button variant="secondary" onClick={onCancel}>
          {t('form.cancel')}
        </Button>
      </div>
    </div>
  );
}
