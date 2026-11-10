import { useState } from 'react';
import { Dialog } from 'radix-ui';
import { useTranslation } from 'react-i18next';
import { useServices } from '@/app/services';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { useSession } from '@/features/auth/session';
import { ConfirmDialog } from '@/features/members/ConfirmDialog';
import { ApiError } from '@/lib/api/client';
import type { components } from '@/lib/api/schema';

type GroupName = components['schemas']['GroupName'];

/**
 * WF-19, "Account" (US-016): the person's name and e-mail and the deletion of the account. The
 * backend deletes the data first (R-016-01); then this screen deletes the Firebase account, which
 * ends the session (R-016-06). A refusal names the groups (R-016-02); an old sign-in asks for a new
 * one (R-016-07).
 */
export function AccountPage() {
  const { t } = useTranslation();
  const { api, auth } = useServices();
  const session = useSession();
  const [confirming, setConfirming] = useState(false);
  const [blocking, setBlocking] = useState<GroupName[] | null>(null);
  const [reauth, setReauth] = useState(false);
  const [failed, setFailed] = useState(false);

  async function remove() {
    setBlocking(null);
    setFailed(false);
    try {
      await api.request<void>('DELETE', '/me');
    } catch (error) {
      refuse(error);
      return;
    }
    // The data is gone; whatever Firebase answers, the person leaves the session.
    await auth.deleteAccount().catch(() => auth.signOut());
  }

  function refuse(error: unknown) {
    if (error instanceof ApiError && error.code === 'SOLE_OWNER') {
      setBlocking(error.body.groups ?? []);
    } else if (error instanceof ApiError && error.code === 'RECENT_LOGIN_REQUIRED') {
      setReauth(true);
    } else {
      setFailed(true);
    }
  }

  return (
    <>
      <h1 className="mb-4 text-2xl font-semibold">{t('nav.account')}</h1>
      <div className="flex max-w-xl flex-col gap-4">
        <Card>
          <h2 className="mb-2 text-xl font-semibold">{t('account.details')}</h2>
          {session.status === 'signedIn' && (
            <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1">
              <dt className="text-text-muted">{t('account.name')}</dt>
              <dd className="break-words">{session.user.name}</dd>
              <dt className="text-text-muted">{t('account.email')}</dt>
              <dd className="break-words">{session.user.email}</dd>
            </dl>
          )}
        </Card>
        <Card>
          <h2 className="mb-2 text-xl font-semibold">{t('account.deleteTitle')}</h2>
          <p className="mb-4">{t('account.deleteIntro')}</p>
          {blocking && (
            <div role="alert" className="mb-4 rounded-card border border-danger bg-surface-raised p-3">
              <p className="mb-2">{t('account.soleOwner')}</p>
              <ul>
                {blocking.map((group) => (
                  <li key={group.id} className="flex items-center justify-between gap-2 py-1">
                    <span className="break-words">{group.name}</span>
                    <span className="rounded-control border border-border px-2 text-sm">
                      {t('account.onlyOwner')}
                    </span>
                  </li>
                ))}
              </ul>
            </div>
          )}
          {failed && (
            <div role="alert" className="mb-4 rounded-card border border-danger bg-surface-raised p-3">
              {t('error.INTERNAL_ERROR')}
            </div>
          )}
          <Button variant="secondary" onClick={() => setConfirming(true)}>
            {t('account.deleteAction')}
          </Button>
        </Card>
      </div>
      {confirming && (
        <ConfirmDialog
          title={t('account.deleteTitle')}
          text={t('account.deleteConfirm')}
          confirm={t('account.deleteAction')}
          onConfirm={remove}
          onClose={() => setConfirming(false)}
        />
      )}
      {reauth && (
        <Dialog.Root open onOpenChange={(open) => !open && setReauth(false)}>
          <Dialog.Portal>
            <Dialog.Overlay className="fixed inset-0 bg-text/50" />
            <Dialog.Content
              aria-describedby="reauth-text"
              className="fixed top-1/2 left-1/2 w-[calc(100%-2rem)] max-w-md -translate-x-1/2 -translate-y-1/2 rounded-card border border-border bg-surface-raised p-6 shadow-card"
            >
              <Dialog.Title className="mb-2 text-xl font-semibold">{t('account.reauthTitle')}</Dialog.Title>
              <p id="reauth-text" className="mb-4">
                {t('account.reauthText')}
              </p>
              <div className="flex justify-end gap-2">
                <Button type="button" variant="secondary" onClick={() => setReauth(false)}>
                  {t('form.cancel')}
                </Button>
                <Button type="button" onClick={() => void auth.signOut()}>
                  {t('account.reauthAction')}
                </Button>
              </div>
            </Dialog.Content>
          </Dialog.Portal>
        </Dialog.Root>
      )}
    </>
  );
}
