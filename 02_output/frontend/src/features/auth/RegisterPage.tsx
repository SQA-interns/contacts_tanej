import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { useServices } from '@/app/services';
import { Button } from '@/components/ui/button';
import { AuthPage, FormError, TextField } from './AuthForm';
import { useAuthAction } from './useAuthAction';
import {
  NAME_MAX_LENGTH,
  PASSWORD_MIN_LENGTH,
  validateRegistration,
  type RegistrationErrors,
} from './validation';

/** WF-02: name, e-mail, password; the form refuses what Firebase would not see (R-001-02, R-001-03). */
export function RegisterPage() {
  const { t } = useTranslation();
  const { auth } = useServices();
  const { busy, error, run } = useAuthAction();
  const [input, setInput] = useState({ name: '', email: '', password: '' });
  const [errors, setErrors] = useState<RegistrationErrors>({});

  const change = (field: keyof typeof input) => (event: { target: { value: string } }) =>
    setInput((current) => ({ ...current, [field]: event.target.value }));

  const submit = (event: FormEvent) => {
    event.preventDefault();
    const found = validateRegistration(input);
    setErrors(found);
    if (Object.keys(found).length === 0) void run(() => auth.register(input));
  };

  return (
    <AuthPage title={t('auth.register.title')}>
      <form
        noValidate
        onSubmit={submit}
        className="flex flex-col gap-4"
        aria-label={t('auth.register.title')}
      >
        <FormError error={error} />
        <TextField
          label={t('auth.name')}
          hint={t('auth.register.nameHint', { max: NAME_MAX_LENGTH })}
          error={errors.name}
          autoComplete="name"
          value={input.name}
          onChange={change('name')}
        />
        <TextField
          label={t('auth.email')}
          type="email"
          error={errors.email}
          autoComplete="email"
          value={input.email}
          onChange={change('email')}
        />
        <TextField
          label={t('auth.password')}
          type="password"
          hint={t('auth.register.passwordHint', { min: PASSWORD_MIN_LENGTH })}
          error={errors.password}
          autoComplete="new-password"
          value={input.password}
          onChange={change('password')}
        />
        <Button type="submit" busy={busy}>
          {t('auth.register.submit')}
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
        {t('auth.register.haveAccount')}{' '}
        <Link className="underline" to="/sign-in">
          {t('auth.register.signIn')}
        </Link>
      </p>
    </AuthPage>
  );
}
