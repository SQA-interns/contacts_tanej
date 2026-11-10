import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { stacked } from '@/components/ui/stacked-table';
import type { components } from '@/lib/api/schema';

type Member = components['schemas']['Member'];
type Role = components['schemas']['Role'];

const ROLES: Role[] = ['OWNER', 'EDITOR', 'VIEWER'];

interface MemberListProps {
  members: Member[];
  /** True for an Owner: role selects and "Remove" (R-004-08). */
  canManage: boolean;
  /** The e-mail address of the signed-in person, to find their own row. */
  selfEmail: string;
  onChangeRole: (member: Member, role: Role) => void;
  onRemove: (member: Member) => void;
  onLeave: () => void;
}

/** WF-07, "Members" (R-004-01): name, e-mail and role for everyone; the controls by role. */
export function MemberList({
  members,
  canManage,
  selfEmail,
  onChangeRole,
  onRemove,
  onLeave,
}: MemberListProps) {
  const { t } = useTranslation();
  return (
    <Card aria-labelledby="members-heading" role="region" className="p-0">
      <h2 id="members-heading" className="px-4 pt-4 text-xl font-semibold">
        {t('groups.tabs.members')}
      </h2>
      <table className={stacked.table}>
        <thead className={stacked.head}>
          <tr className="border-b border-border">
            <th scope="col" className="px-4 py-3">
              {t('groups.columns.name')}
            </th>
            <th scope="col" className="px-4 py-3">
              {t('auth.email')}
            </th>
            <th scope="col" className="px-4 py-3">
              {t('groups.columns.role')}
            </th>
            <th scope="col" className="px-4 py-3">
              <span className="sr-only">{t('groups.columns.actions')}</span>
            </th>
          </tr>
        </thead>
        <tbody className={stacked.body}>
          {members.map((member) => {
            const self = member.email.toLowerCase() === selfEmail.toLowerCase();
            return (
              <tr key={member.userId} className={stacked.row}>
                <td data-label={t('groups.columns.name')} className={stacked.cell}>
                  {member.name}
                </td>
                <td data-label={t('auth.email')} className={`${stacked.cell} break-all`}>
                  {member.email}
                </td>
                <td data-label={t('groups.columns.role')} className={stacked.cell}>
                  {canManage ? (
                    <select
                      aria-label={t('members.roleOf', { name: member.name })}
                      value={member.role}
                      onChange={(event) => onChangeRole(member, event.target.value as Role)}
                      className="min-h-11 rounded-control border border-border bg-surface px-2"
                    >
                      {ROLES.map((role) => (
                        <option key={role} value={role}>
                          {t(`role.${role}`)}
                        </option>
                      ))}
                    </select>
                  ) : (
                    t(`role.${member.role}`)
                  )}
                </td>
                <td className={stacked.actions}>
                  {self ? (
                    <Button variant="ghost" onClick={onLeave}>
                      {t('members.leave.action')}
                    </Button>
                  ) : (
                    canManage && (
                      <Button variant="ghost" onClick={() => onRemove(member)}>
                        {t('members.remove.action')}
                      </Button>
                    )
                  )}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </Card>
  );
}
