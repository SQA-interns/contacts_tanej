import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import type { components } from '@/lib/api/schema';
import { formatDateTime } from '@/lib/format';
import type { Language } from '@/lib/i18n';

type Invitation = components['schemas']['Invitation'];

interface PendingInvitationsProps {
  invitations: Invitation[];
  onCancel: (invitation: Invitation) => void;
}

/** WF-07, "Pending invitations" (R-003-08): e-mail address, role and expiry date, with "Cancel invitation". */
export function PendingInvitations({ invitations, onCancel }: PendingInvitationsProps) {
  const { t, i18n } = useTranslation();
  const language = i18n.language as Language;
  return (
    <Card aria-labelledby="pending-heading" role="region" className="p-0">
      <h2 id="pending-heading" className="px-4 pt-4 text-xl font-semibold">
        {t('members.pending.title')}
      </h2>
      {invitations.length === 0 ? (
        <p className="px-4 py-3 text-text-muted">{t('members.pending.empty')}</p>
      ) : (
        <table className="w-full text-left">
          <thead>
            <tr className="border-b border-border">
              <th scope="col" className="px-4 py-3">
                {t('auth.email')}
              </th>
              <th scope="col" className="px-4 py-3">
                {t('groups.columns.role')}
              </th>
              <th scope="col" className="px-4 py-3">
                {t('members.pending.expiresColumn')}
              </th>
              <th scope="col" className="px-4 py-3">
                <span className="sr-only">{t('groups.columns.actions')}</span>
              </th>
            </tr>
          </thead>
          <tbody>
            {invitations.map((invitation) => (
              <tr key={invitation.id} className="border-b border-border last:border-b-0">
                <td className="px-4 py-2 break-all">{invitation.email}</td>
                <td className="px-4 py-2">{t(`role.${invitation.role}`)}</td>
                <td className="px-4 py-2">
                  {t('members.pending.expires', {
                    date: formatDateTime(invitation.expiresAt, language),
                  })}
                </td>
                <td className="px-4 py-2 text-right">
                  <Button variant="ghost" onClick={() => onCancel(invitation)}>
                    {t('members.pending.cancel')}
                  </Button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </Card>
  );
}
