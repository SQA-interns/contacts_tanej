import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useServices } from '@/app/services';
import { Button } from '@/components/ui/button';
import { AuthPage, FormError } from './AuthForm';
import { useAuthAction } from './useAuthAction';

/** WF-03: the only screen while the e-mail is not verified (R-001-05, BR-03). */
export function VerifyEmailPage({ email }: { email: string }) {
  const { t } = useTranslation();
  const { auth } = useServices();
  const { busy, error, run } = useAuthAction();
  const [resent, setResent] = useState(false);

  const sendAgain = async () => setResent(await run(auth.resendVerification));

  return (
    <AuthPage title={t('auth.verify.title')}>
      <p>{t('auth.verify.text', { email })}</p>
      <FormError error={error} />
      {resent && (
        <p role="status" className="text-success">
          {t('auth.verify.resent')}
        </p>
      )}
      <Button variant="secondary" busy={busy} onClick={() => void sendAgain()}>
        {t('auth.verify.again')}
      </Button>
      <Button variant="ghost" onClick={() => void auth.signOut()}>
        {t('auth.signOut')}
      </Button>
    </AuthPage>
  );
}
