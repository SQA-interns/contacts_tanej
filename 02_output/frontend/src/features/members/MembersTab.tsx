import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { MemberSection } from './MemberSection';
import { useOutletContext } from 'react-router';
import { useServices } from '@/app/services';
import { StatePanel } from '@/components/ui/state-panel';
import { useToast } from '@/components/ui/toast';
import type { components } from '@/lib/api/schema';
import { InviteForm } from './InviteForm';
import { PendingInvitations } from './PendingInvitations';

type GroupSummary = components['schemas']['GroupSummary'];
type Invitation = components['schemas']['Invitation'];
type Role = components['schemas']['Role'];

/** The limit of R-003-04, shown before the 21st invitation is tried. */
const MAX_PENDING = 20;

/**
 * WF-07, the Members tab. For an Owner: the pending invitations and the form to invite (US-003).
 * Editors and Viewers get neither, and the invitations are not even asked for (R-003-11). Every
 * member sees the member list (R-004-01); the controls on it depend on the role (US-004).
 */
export function MembersTab() {
  const group = useOutletContext<GroupSummary>();
  return (
    <div className="flex flex-col gap-4">
      <MemberSection groupId={group.id} isOwner={group.role === 'OWNER'} />
      {group.role === 'OWNER' && <OwnerInvitations groupId={group.id} />}
    </div>
  );
}

function OwnerInvitations({ groupId }: { groupId: string }) {
  const { t } = useTranslation();
  const { api } = useServices();
  const queryClient = useQueryClient();
  const base = `/groups/${encodeURIComponent(groupId)}/invitations`;
  const key = ['invitations', groupId];
  const toast = useToast();
  const pending = useQuery({ queryKey: key, queryFn: () => api.request<Invitation[]>('GET', base) });

  async function invite(email: string, role: Role) {
    await api.request<Invitation>('POST', base, { email, role });
    await queryClient.invalidateQueries({ queryKey: key });
    toast(t('toast.invitationSent'));
  }

  async function cancel(invitation: Invitation) {
    await api.request<void>('DELETE', `${base}/${encodeURIComponent(invitation.id)}`);
    await queryClient.invalidateQueries({ queryKey: key });
    toast(t('toast.invitationCancelled'));
  }

  if (pending.isPending) return <StatePanel state="loading" />;
  if (pending.isError) return <StatePanel state="error" onRetry={() => void pending.refetch()} />;
  return (
    <>
      {pending.data.length >= MAX_PENDING && (
        <div role="alert" className="rounded-card border border-danger bg-surface-raised p-4">
          {t('members.limitReached')}
        </div>
      )}
      <PendingInvitations invitations={pending.data} onCancel={(i) => void cancel(i)} />
      <InviteForm onInvite={invite} />
    </>
  );
}
