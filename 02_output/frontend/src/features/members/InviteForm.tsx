import { UserPlus } from 'lucide-react';
import { useId, useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { ApiError } from '@/lib/api/client';
import type { components } from '@/lib/api/schema';
import { errorText } from '@/lib/i18n';

type Role = components['schemas']['Role'];

const ROLES: Role[] = ['OWNER', 'EDITOR', 'VIEWER'];
const EMAIL_PROBLEMS = ['INVALID_FORMAT', 'TOO_LONG'];

interface InviteFormProps {
  /** Sends the invitation; a refusal is thrown as `ApiError` and shown under the field. */
  onInvite: (email: string, role: Role) => Promise<void>;
}

/**
 * WF-07, the form "Invite people" (Owner only, R-003-01): e-mail and role. The server decides every
 * rule; its reason is shown with the typed values kept (UI-13).
 */
export function InviteForm({ onInvite }: InviteFormProps) {
  const { t } = useTranslation();
  const id = useId();
  const [email, setEmail] = useState('');
  const [role, setRole] = useState<Role>('EDITOR');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await onInvite(email.trim(), role);
      setEmail('');
    } catch (failure) {
      setError(refusalText(failure));
    } finally {
      setBusy(false);
    }
  }

  function refusalText(failure: unknown): string {
    if (!(failure instanceof ApiError)) return t('error.INTERNAL_ERROR');
    const field = failure.body.errors?.find((e) => e.field === 'email');
    if (field && EMAIL_PROBLEMS.includes(field.code)) return t(`members.emailError.${field.code}`);
    return errorText(t, failure.code);
  }

  return (
    <Card>
      <form aria-label={t('members.invite.title')} noValidate onSubmit={(e) => void submit(e)}>
        <h2 className="mb-3 text-xl font-semibold">{t('members.invite.title')}</h2>
        <div className="flex flex-col gap-4">
          <div className="flex flex-col gap-1">
            <Label htmlFor={`${id}-email`}>
              {t('auth.email')}
              <span className="font-normal text-text-muted"> ({t('form.required')})</span>
            </Label>
            <Input
              id={`${id}-email`}
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              autoComplete="off"
              aria-invalid={error ? true : undefined}
              aria-describedby={error ? `${id}-error` : undefined}
            />
            {error && (
              <p id={`${id}-error`} role="alert" className="text-sm text-danger">
                {error}
              </p>
            )}
          </div>
          <div className="flex flex-col gap-1">
            <div className="flex items-baseline gap-1">
              <Label htmlFor={`${id}-role`}>{t('groups.columns.role')}</Label>
              {/* visible required marker outside the label, so the label text stays exactly "Role" (D-50, D-52) */}
              <span aria-hidden="true" className="text-text-muted">
                *
              </span>
            </div>
            <select
              id={`${id}-role`}
              required
              value={role}
              onChange={(event) => setRole(event.target.value as Role)}
              className="min-h-11 w-full rounded-control border border-border bg-background px-3 text-base text-text"
            >
              {ROLES.map((value) => (
                <option key={value} value={value}>
                  {t(`role.${value}`)}
                </option>
              ))}
            </select>
          </div>
          <div>
            <Button type="submit" busy={busy}>
              <UserPlus aria-hidden="true" className="size-4 shrink-0" />
              {t('members.invite.submit')}
            </Button>
          </div>
        </div>
      </form>
    </Card>
  );
}
