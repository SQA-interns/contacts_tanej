import { useId, useState, type FormEvent } from 'react';
import { Dialog } from 'radix-ui';
import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { ApiError } from '@/lib/api/client';
import { errorText } from '@/lib/i18n';

const PROBLEMS = ['INVALID_CHARACTERS', 'TOO_LONG', 'TOO_SHORT'];

interface RenameTagDialogProps {
  name: string;
  /** Sends the new name; a refusal is thrown as `ApiError` and shown in the dialog. */
  onSubmit: (name: string) => Promise<void>;
  onClose: () => void;
}

/** WF-13, "Rename tag": one field; the server's reason is shown with the typed name kept (UI-13). */
export function RenameTagDialog({ name, onSubmit, onClose }: RenameTagDialogProps) {
  const { t } = useTranslation();
  const id = useId();
  const [value, setValue] = useState(name);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await onSubmit(value.trim());
    } catch (failure) {
      setError(refusalText(failure));
      setBusy(false);
    }
  }

  function refusalText(failure: unknown): string {
    if (!(failure instanceof ApiError)) return t('error.INTERNAL_ERROR');
    const field = failure.body.errors?.find((e) => e.field === 'name');
    if (field && PROBLEMS.includes(field.code)) return t(`contact.tagError.${field.code}`);
    return errorText(t, failure.code);
  }

  return (
    <Dialog.Root open onOpenChange={(open) => !open && onClose()}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 bg-text/50" />
        <Dialog.Content
          aria-describedby={undefined}
          className="fixed top-1/2 left-1/2 w-[calc(100%-2rem)] max-w-md -translate-x-1/2 -translate-y-1/2 rounded-card border border-border bg-surface-raised p-6 shadow-card"
        >
          <Dialog.Title className="mb-4 text-xl font-semibold">{t('tags.renameTitle')}</Dialog.Title>
          <form onSubmit={(event) => void submit(event)} noValidate className="flex flex-col gap-4">
            <div className="flex flex-col gap-1">
              <Label htmlFor={id}>
                {t('tags.name')}
                <span className="font-normal text-text-muted"> ({t('form.required')})</span>
              </Label>
              <Input
                id={id}
                value={value}
                onChange={(event) => setValue(event.target.value)}
                autoComplete="off"
                aria-invalid={error ? true : undefined}
                aria-describedby={`${id}-${error ? 'error' : 'hint'}`}
              />
              {error ? (
                <p id={`${id}-error`} role="alert" className="text-sm text-danger">
                  {error}
                </p>
              ) : (
                <p id={`${id}-hint`} className="text-sm text-text-muted">
                  {t('tags.nameHint')}
                </p>
              )}
            </div>
            <div className="flex justify-end gap-2">
              <Button type="button" variant="secondary" onClick={onClose}>
                {t('form.cancel')}
              </Button>
              <Button type="submit" busy={busy}>
                {t('tags.rename')}
              </Button>
            </div>
          </form>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
