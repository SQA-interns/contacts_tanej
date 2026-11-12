import { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Link, useOutletContext, useParams } from 'react-router';
import { useServices } from '@/app/services';
import { Button } from '@/components/ui/button';
import { StatePanel } from '@/components/ui/state-panel';
import type { Attribute } from '@/features/contacts/AttributeRows';
import { ContactDetails } from '@/features/contacts/ContactDetails';
import { valuesOf, type Contact } from '@/features/contacts/contactValues';
import { ConfirmDialog } from '@/features/members/ConfirmDialog';
import { ApiError } from '@/lib/api/client';
import type { components } from '@/lib/api/schema';
import { formatDateTime } from '@/lib/format';
import { errorText, type Language } from '@/lib/i18n';
import { VersionCompare, type Content } from './VersionCompare';

type GroupSummary = components['schemas']['GroupSummary'];
type Version = components['schemas']['Version'];
type VersionDetail = components['schemas']['VersionDetail'];

/** The snapshot also holds the tags and attributes of the version. */
type Snapshot = VersionDetail['snapshot'] & { tags?: string[]; attributes?: Attribute[] };

type View = { kind: 'list' } | { kind: 'view' | 'compare'; number: number };

const contentOfContact = (contact: Contact): Content => ({
  values: valuesOf(contact),
  tags: contact.tags ?? [],
  attributes: contact.attributes ?? [],
});

const contentOfVersion = (version: VersionDetail): Content => {
  const snapshot = version.snapshot as Snapshot;
  return {
    values: valuesOf(snapshot as Contact),
    tags: snapshot.tags ?? [],
    attributes: snapshot.attributes ?? [],
  };
};

/**
 * WF-12, the Versions tab of a contact (US-020): the list, one version read-only, the comparison
 * with the current contact and the restore after a confirmation. A Viewer has no tab (R-020-07).
 * The revision of the contact is read once when the tab opens, so a change by someone else since
 * then makes the restore a conflict (R-020-08).
 */
export function VersionsPage() {
  const { t } = useTranslation();
  const { api } = useServices();
  const queryClient = useQueryClient();
  const group = useOutletContext<GroupSummary>();
  const { contactId = '' } = useParams();
  const [view, setView] = useState<View>({ kind: 'list' });
  const [restoring, setRestoring] = useState<number | null>(null);
  const [message, setMessage] = useState<{ text: string; failed: boolean } | null>(null);
  const base = `/groups/${encodeURIComponent(group.id)}/contacts/${encodeURIComponent(contactId)}`;
  const allowed = group.role !== 'VIEWER';
  const versions = useQuery({
    queryKey: ['versions', group.id, contactId],
    queryFn: () => api.request<Version[]>('GET', `${base}/versions`),
    enabled: allowed,
  });
  const opened = useQuery({
    queryKey: ['versions-contact', group.id, contactId],
    queryFn: () => api.request<Contact>('GET', base),
    enabled: allowed,
    staleTime: Infinity,
    gcTime: 0,
    refetchOnWindowFocus: false,
  });
  const number = view.kind === 'list' ? 0 : view.number;
  const detail = useQuery({
    queryKey: ['version', group.id, contactId, number],
    queryFn: () => api.request<VersionDetail>('GET', `${base}/versions/${number}`),
    enabled: allowed && number > 0,
  });

  if (!allowed) return null;

  async function restore(target: number) {
    setMessage(null);
    try {
      const contact = await api.request<Contact>('POST', `${base}/versions/${target}/restore`, {
        revision: opened.data?.revision,
      });
      queryClient.setQueryData(['versions-contact', group.id, contactId], contact);
      await queryClient.invalidateQueries({ queryKey: ['versions', group.id, contactId] });
      await queryClient.invalidateQueries({ queryKey: ['contact', group.id, contactId] });
      await queryClient.invalidateQueries({ queryKey: ['contacts', group.id] });
      setMessage({ text: t('versions.restored'), failed: false });
      setView({ kind: 'list' });
    } catch (failure) {
      const code = failure instanceof ApiError ? failure.code : 'INTERNAL_ERROR';
      setMessage({
        text:
          code === 'NOTHING_TO_CHANGE'
            ? t('versions.nothingToChange')
            : code === 'CONTACT_IN_TRASH'
              ? t('versions.inTrash')
              : errorText(t, code),
        failed: true,
      });
    }
  }

  return (
    <>
      <p className="mb-2 text-sm">
        <Link to={`/groups/${group.id}`} className="underline">
          {t('groups.tabs.contacts')}
        </Link>{' '}
        / {t('contact.crumb')}
      </p>
      <nav aria-label={t('contact.crumb')} className="mb-4 flex gap-4">
        <Link to={`/groups/${group.id}/contacts/${contactId}`} className="underline">
          {t('contact.details')}
        </Link>
        <Link to={`/groups/${group.id}/contacts/${contactId}/history`} className="underline">
          {t('history.tab')}
        </Link>
        <span aria-current="page" className="font-medium">
          {t('versions.tab')}
        </span>
      </nav>
      {message && (
        <div
          role={message.failed ? 'alert' : 'status'}
          className="mb-4 rounded-card border border-border bg-surface-raised p-3"
        >
          {message.text}
        </div>
      )}
      {view.kind === 'list' ? (
        <VersionList
          versions={versions}
          onView={(n) => setView({ kind: 'view', number: n })}
          onCompare={(n) => setView({ kind: 'compare', number: n })}
          onRestore={setRestoring}
        />
      ) : (
        <VersionDetailView
          kind={view.kind}
          number={view.number}
          detail={detail}
          current={opened.data}
          onBack={() => setView({ kind: 'list' })}
          onRestore={() => setRestoring(view.number)}
        />
      )}
      {restoring !== null && (
        <ConfirmDialog
          title={t('versions.restoreTitle')}
          text={t('versions.restoreText')}
          confirm={t('versions.restoreTitle')}
          onConfirm={() => restore(restoring)}
          onClose={() => setRestoring(null)}
        />
      )}
    </>
  );
}

interface ListProps {
  versions: ReturnType<typeof useQuery<Version[]>>;
  onView: (number: number) => void;
  onCompare: (number: number) => void;
  onRestore: (number: number) => void;
}

function VersionList({ versions, onView, onCompare, onRestore }: ListProps) {
  const { t, i18n } = useTranslation();
  if (versions.isPending) return <StatePanel state="loading" />;
  if (versions.isError) return <StatePanel state="error" onRetry={() => void versions.refetch()} />;
  const items = versions.data;
  if (items.length === 0) {
    return (
      <section className="flex flex-col gap-1">
        <h2 className="text-xl font-semibold">{t('versions.emptyTitle')}</h2>
        <p className="text-text-muted">{t('versions.emptyText')}</p>
      </section>
    );
  }
  return (
    <table className="w-full border-collapse text-left">
      <thead>
        <tr>
          <th scope="col" className="p-2">
            {t('versions.number')}
          </th>
          <th scope="col" className="p-2">
            {t('versions.who')}
          </th>
          <th scope="col" className="p-2">
            {t('versions.when')}
          </th>
          <th scope="col" className="p-2">
            {t('versions.actions')}
          </th>
        </tr>
      </thead>
      <tbody>
        {items.map((v) => (
          <tr key={v.number} className="border-t border-border">
            <td className="p-2">{t('versions.version', { number: v.number })}</td>
            <td className="p-2 break-words">{v.userName}</td>
            <td className="p-2">
              <time dateTime={v.createdAt}>{formatDateTime(v.createdAt, i18n.language as Language)}</time>
            </td>
            <td className="flex flex-wrap gap-1 p-2">
              <Button variant="ghost" onClick={() => onView(v.number)}>
                {t('versions.view')}
              </Button>
              <Button variant="ghost" onClick={() => onCompare(v.number)}>
                {t('versions.compare')}
              </Button>
              <Button variant="ghost" onClick={() => onRestore(v.number)}>
                {t('versions.restore')}
              </Button>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

interface DetailProps {
  kind: 'view' | 'compare';
  number: number;
  detail: ReturnType<typeof useQuery<VersionDetail>>;
  current: Contact | undefined;
  onBack: () => void;
  onRestore: () => void;
}

function VersionDetailView({ kind, number, detail, current, onBack, onRestore }: DetailProps) {
  const { t } = useTranslation();
  if (detail.isPending || !current) return <StatePanel state="loading" />;
  if (detail.isError) return <StatePanel state="error" onRetry={() => void detail.refetch()} />;
  const version = contentOfVersion(detail.data);
  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h2 className="text-xl font-semibold">
          {kind === 'compare' ? t('versions.compareTitle') : t('versions.version', { number })}
        </h2>
        <Button variant="secondary" onClick={onBack}>
          {t('versions.back')}
        </Button>
      </div>
      {kind === 'compare' ? (
        <VersionCompare versionNumber={number} version={version} current={contentOfContact(current)} />
      ) : (
        <ContactDetails
          title={t('contact.details')}
          values={version.values}
          tags={version.tags}
          attributes={version.attributes}
        />
      )}
      <div>
        <Button onClick={onRestore}>{t('versions.restoreTitle')}</Button>
      </div>
    </div>
  );
}
