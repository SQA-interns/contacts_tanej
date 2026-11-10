import { useId, type ComponentProps, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { Card } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import type { AuthErrorKey } from '@/lib/auth';
import type { FieldError } from './validation';

/** The narrow card with a page heading that every sign-in screen uses (WF-01 to WF-04). */
export function AuthPage({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="mx-auto w-full max-w-md">
      <h1 className="mb-4 text-2xl font-semibold">{title}</h1>
      <Card className="flex flex-col gap-4">{children}</Card>
    </div>
  );
}

interface TextFieldProps extends Omit<ComponentProps<typeof Input>, 'id'> {
  label: string;
  hint?: string;
  error?: FieldError;
}

/** A labelled input with an optional rule hint and an error that is announced (UI-13, UI-19). */
export function TextField({ label, hint, error, ...input }: TextFieldProps) {
  const { t } = useTranslation();
  const id = useId();
  const describedBy = [hint && `${id}-hint`, error && `${id}-error`].filter(Boolean).join(' ');
  return (
    <div className="flex flex-col gap-1">
      <Label htmlFor={id}>
        {label}
        <span className="font-normal text-text-muted"> ({t('form.required')})</span>
      </Label>
      <Input
        id={id}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy || undefined}
        {...input}
      />
      {hint && (
        <p id={`${id}-hint`} className="text-sm text-text-muted">
          {hint}
        </p>
      )}
      {error && (
        <p id={`${id}-error`} role="alert" className="text-sm text-danger">
          {t(`auth.error.${error}`)}
        </p>
      )}
    </div>
  );
}

/** The error that Firebase gave, shown above the buttons (R-001-07). */
export function FormError({ error }: { error: AuthErrorKey | null }) {
  const { t } = useTranslation();
  if (!error) return null;
  return (
    <div role="alert" className="rounded-control border border-danger p-3 text-danger">
      {t(`auth.error.${error}`)}
    </div>
  );
}
