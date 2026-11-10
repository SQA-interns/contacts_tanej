import { useState } from 'react';
import { Dialog } from 'radix-ui';
import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/button';

interface ConfirmDialogProps {
  title: string;
  text: string;
  /** The label of the confirming button, the same words as the action (UI-13). */
  confirm: string;
  /** Runs the action; the dialog closes when it ends, whatever the outcome. */
  onConfirm: () => Promise<void>;
  onClose: () => void;
}

/** WF-07, the dialogs "Remove member" and "Leave group": the action only runs after the second click. */
export function ConfirmDialog({ title, text, confirm, onConfirm, onClose }: ConfirmDialogProps) {
  const { t } = useTranslation();
  const [busy, setBusy] = useState(false);

  async function run() {
    setBusy(true);
    try {
      await onConfirm();
    } finally {
      onClose();
    }
  }

  return (
    <Dialog.Root open onOpenChange={(open) => !open && onClose()}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 bg-text/50" />
        <Dialog.Content
          aria-describedby="confirm-text"
          className="fixed top-1/2 left-1/2 w-[calc(100%-2rem)] max-w-md -translate-x-1/2 -translate-y-1/2 rounded-card border border-border bg-surface-raised p-6 shadow-card"
        >
          <Dialog.Title className="mb-2 text-xl font-semibold">{title}</Dialog.Title>
          <p id="confirm-text" className="mb-4">
            {text}
          </p>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="secondary" onClick={onClose}>
              {t('form.cancel')}
            </Button>
            <Button type="button" busy={busy} onClick={() => void run()}>
              {confirm}
            </Button>
          </div>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
