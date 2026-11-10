import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { useServices } from '@/app/services';
import { Button } from '@/components/ui/button';
import { AuthPage, FormError, TextField } from './AuthForm';
import { useAuthAction } from './useAuthAction';

/**
 * WF-04: asks for the e-mail, starts Firebase's reset flow, then says the same thing whether or not
 * an account exists (R-001-06; no account is revealed).
 */
export function ResetPasswordPage() {
  const { t } = useTranslation();
  const { auth } = useServices();
  const { busy, error, run } = useAuthAction();
  const [email, setEmail] = useState('');
  const [sent, setSent] = useState(false);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (await run(() => auth.sendPasswordReset(email))) setSent(true);
  };

  if (sent) {
    return (
      <AuthPage title={t('auth.reset.sentTitle')}>
        <p>{t('auth.reset.sentText')}</p>
        <Button asChild>
          <Link to="/sign-in">{t('auth.reset.back')}</Link>
        </Button>
      </AuthPage>
    );
  }
  return (
    <AuthPage title={t('auth.reset.title')}>
      <form
        onSubmit={(event) => void submit(event)}
        className="flex flex-col gap-4"
        aria-label={t('auth.reset.title')}
      >
        <p className="text-text-muted">{t('auth.reset.intro')}</p>
        <FormError error={error} />
        <TextField
          label={t('auth.email')}
          type="email"
          autoComplete="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
        />
        <Button type="submit" busy={busy}>
          {t('auth.reset.submit')}
        </Button>
      </form>
      <p className="text-sm">
        <Link className="underline" to="/sign-in">
          {t('auth.reset.back')}
        </Link>
      </p>
    </AuthPage>
  );
}
