import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { useServices } from '@/app/services';
import { StatePanel } from '@/components/ui/state-panel';
import { useToast } from '@/components/ui/toast';
import { useSession } from '@/features/auth/session';
import { ApiError } from '@/lib/api/client';
import type { components } from '@/lib/api/schema';
import { errorText } from '@/lib/i18n';
import { ConfirmDialog } from './ConfirmDialog';
import { MemberList } from './MemberList';

type Member = components['schemas']['Member'];
type Role = components['schemas']['Role'];

/** What the open confirmation is about: a member to remove, or the person themselves leaving. */
type Pending = { kind: 'remove'; member: Member } | { kind: 'leave' } | null;

interface MemberSectionProps {
  groupId: string;
  isOwner: boolean;
}

/**
 * WF-07, the member list with its actions (US-004). A refusal is shown above the list and changes
 * nothing; "not found" means the person was removed meanwhile, so the group list is shown (EX-004-03).
 */
export function MemberSection({ groupId, isOwner }: MemberSectionProps) {
  const { t } = useTranslation();
  const toast = useToast();
  const { api } = useServices();
  const session = useSession();
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const [pending, setPending] = useState<Pending>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const base = `/groups/${encodeURIComponent(groupId)}`;
  const key = ['members', groupId];
  const members = useQuery({ queryKey: key, queryFn: () => api.request<Member[]>('GET', `${base}/members`) });

  async function attempt(action: () => Promise<unknown>, doneText?: string) {
    setNotice(null);
    try {
      await action();
      if (doneText) toast(doneText);
    } catch (failure) {
      if (failure instanceof ApiError && failure.status === 404) {
        await queryClient.invalidateQueries({ queryKey: ['groups'] });
        void navigate('/');
        return;
      }
      setNotice(failure instanceof ApiError ? errorText(t, failure.code) : t('error.INTERNAL_ERROR'));
    }
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: key }),
      queryClient.invalidateQueries({ queryKey: ['group', groupId] }),
    ]);
  }

  const changeRole = (member: Member, role: Role) =>
    attempt(
      () => api.request('PUT', `${base}/members/${encodeURIComponent(member.userId)}`, { role }),
      t('toast.roleChanged'),
    );

  async function confirm() {
    if (pending?.kind === 'remove') {
      await attempt(
        () => api.request('DELETE', `${base}/members/${encodeURIComponent(pending.member.userId)}`),
        t('toast.memberRemoved'),
      );
    } else {
      await attempt(async () => {
        await api.request('POST', `${base}/leave`);
        await queryClient.invalidateQueries({ queryKey: ['groups'] });
        void navigate('/');
      });
    }
  }

  if (members.isPending) return <StatePanel state="loading" />;
  if (members.isError) return <StatePanel state="error" onRetry={() => void members.refetch()} />;
  const self = session.status === 'signedIn' ? session.user.email : '';
  const dialog = pending?.kind === 'remove' ? 'remove' : 'leave';
  return (
    <>
      {notice && (
        <div role="alert" className="rounded-card border border-danger bg-surface-raised p-4">
          {notice}
        </div>
      )}
      <MemberList
        members={members.data}
        canManage={isOwner}
        selfEmail={self}
        onChangeRole={(member, role) => void changeRole(member, role)}
        onRemove={(member) => setPending({ kind: 'remove', member })}
        onLeave={() => setPending({ kind: 'leave' })}
      />
      {pending && (
        <ConfirmDialog
          title={t(`members.${dialog}.title`)}
          text={t(`members.${dialog}.text`)}
          confirm={t(`members.${dialog}.action`)}
          onConfirm={confirm}
          onClose={() => setPending(null)}
        />
      )}
    </>
  );
}
