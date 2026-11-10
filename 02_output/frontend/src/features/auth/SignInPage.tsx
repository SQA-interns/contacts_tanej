import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { useServices } from '@/app/services';
import { Button } from '@/components/ui/button';
import { AuthPage, FormError, TextField } from './AuthForm';
import { useAuthAction } from './useAuthAction';

/** WF-01: e-mail and password, "Sign in with Google", reset and register links (R-001-01, R-001-07). */
export function SignInPage() {
  const { t } = useTranslation();
  const { auth } = useServices();
  const { busy, error, run } = useAuthAction();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  const submit = (event: FormEvent) => {
    event.preventDefault();
    void run(() => auth.signIn(email, password));
  };

  return (
    <AuthPage title={t('auth.signIn.title')}>
      <form onSubmit={submit} className="flex flex-col gap-4" aria-label={t('auth.signIn.title')}>
        <FormError error={error} />
        <TextField
          label={t('auth.email')}
          type="email"
          autoComplete="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
        />
        <TextField
          label={t('auth.password')}
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
        />
        <Button type="submit" busy={busy}>
          {t('auth.signIn.submit')}
        </Button>
        <Button
          type="button"
          variant="secondary"
          disabled={busy}
          onClick={() => void run(auth.signInWithGoogle)}
        >
          {t('auth.google')}
        </Button>
      </form>
      <p className="text-sm">
        <Link className="underline" to="/reset-password">
          {t('auth.signIn.forgot')}
        </Link>
      </p>
      <p className="text-sm">
        {t('auth.signIn.noAccount')}{' '}
        <Link className="underline" to="/register">
          {t('auth.signIn.register')}
        </Link>
      </p>
    </AuthPage>
  );
}
