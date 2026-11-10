import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate, useOutletContext, useParams } from 'react-router';
import { useServices } from '@/app/services';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/features/members/ConfirmDialog';
import { StatePanel } from '@/components/ui/state-panel';
import { ApiError } from '@/lib/api/client';
import type { components } from '@/lib/api/schema';
import { errorText } from '@/lib/i18n';
import { ConflictPanel } from './ConflictPanel';
import { ContactDetails } from './ContactDetails';
import type { Attribute } from './AttributeRows';
import { ContactForm } from './ContactForm';
import { contactTitle, refusedFields, valuesOf, type Contact, type Values } from './contactValues';

type GroupSummary = components['schemas']['GroupSummary'];

function ContactNotFound() {
  const { t } = useTranslation();
  return (
    <Card className="flex max-w-xl flex-col items-start gap-3">
      <h1 className="text-2xl font-semibold">{t('notFound.title')}</h1>
      <p className="text-text-muted">{t('notFound.text')}</p>
    </Card>
  );
}

/**
 * WF-10, the contact screen (US-005): the form "New contact" at `contacts/new` and one contact at
 * `contacts/:contactId`. An Owner or Editor edits; a Viewer and a contact in Trash are read-only.
 * The group comes from the address (AR-04) and the role from the group page above.
 */
export function ContactPage() {
  const { t } = useTranslation();
  const { api } = useServices();
  const group = useOutletContext<GroupSummary>();
  const { contactId } = useParams();
  const path = `/groups/${encodeURIComponent(group.id)}/contacts/${encodeURIComponent(contactId ?? '')}`;
  const contact = useQuery({
    queryKey: ['contact', group.id, contactId],
    queryFn: () => api.request<Contact>('GET', path),
    enabled: contactId !== undefined,
    retry: false,
  });

  if (contactId === undefined) {
    return <ContactScreen group={group} title={t('contact.new')} />;
  }
  if (contact.isPending) return <StatePanel state="loading" />;
  if (contact.isError) {
    if (contact.error instanceof ApiError && contact.error.status === 404) return <ContactNotFound />;
    return <StatePanel state="error" onRetry={() => void contact.refetch()} />;
  }
  return <ContactScreen group={group} contact={contact.data} title={contactTitle(valuesOf(contact.data))} />;
}

/** A save refused as a conflict: the contact as it is now and what the user entered. */
interface Conflict {
  current: Contact;
  mine: Values;
  mineTags: string[];
  mineAttributes: Attribute[];
}

interface ContactScreenProps {
  group: GroupSummary;
  contact?: Contact;
  title: string;
}

function ContactScreen({ group, contact, title }: ContactScreenProps) {
  const { t } = useTranslation();
  const [conflict, setConflict] = useState<Conflict | null>(null);
  const save = useSave(group.id, contact, setConflict);
  const trashed = contact?.state === 'TRASHED';
  const canEdit = group.role !== 'VIEWER' && !trashed;
  const back = `/groups/${group.id}`;

  return (
    <>
      <p className="mb-2 text-sm">
        <Link to={back} className="underline">
          {t('groups.tabs.contacts')}
        </Link>{' '}
        / {t('contact.crumb')}
      </p>
      <h1 className="mb-4 text-2xl font-semibold break-words">{title}</h1>
      {contact && group.role !== 'VIEWER' && (
        <nav aria-label={t('contact.crumb')} className="mb-4 flex gap-4">
          <span aria-current="page" className="font-medium">
            {t('contact.details')}
          </span>
          <Link to={`${back}/contacts/${contact.id}/history`} className="underline">
            {t('history.tab')}
          </Link>
        </nav>
      )}
      {trashed && (
        <div role="alert" className="mb-4 rounded-card border border-danger bg-surface-raised p-4">
          {t('contact.inTrash')}
        </div>
      )}
      {save.saved && (
        <p role="status" className="mb-4 rounded-card border border-border bg-surface-raised p-3">
          {t('contact.saved')}
        </p>
      )}
      {save.failure && (
        <div role="alert" className="mb-4 rounded-card border border-danger bg-surface-raised p-4">
          {save.failure}
        </div>
      )}
      {conflict ? (
        <ConflictPanel
          current={valuesOf(conflict.current)}
          currentTags={conflict.current.tags ?? []}
          mine={conflict.mine}
          mineTags={conflict.mineTags}
          currentAttributes={conflict.current.attributes ?? []}
          mineAttributes={conflict.mineAttributes}
          onReload={() => save.reload(conflict.current, () => setConflict(null))}
          onCancel={() => save.leave(back)}
        />
      ) : canEdit ? (
        <ContactForm
          key={contact?.revision}
          groupId={group.id}
          initial={valuesOf(contact)}
          initialTags={contact?.tags ?? []}
          initialAttributes={contact?.attributes ?? []}
          problems={save.problems}
          busy={save.busy}
          onSubmit={(values, tags, attributes) => void save.submit(values, tags, attributes)}
          onCancel={() => save.leave(back)}
        />
      ) : (
        <ContactDetails
          title={t('contact.details')}
          values={valuesOf(contact)}
          tags={contact?.tags ?? []}
          attributes={contact?.attributes ?? []}
        />
      )}
      {contact && canEdit && !conflict && <DeleteContact groupId={group.id} contactId={contact.id} />}
    </>
  );
}

/** "Delete contact" (R-012-01): after the confirmation the contact moves to Trash and the list shows. */
function DeleteContact({ groupId, contactId }: { groupId: string; contactId: string }) {
  const { t } = useTranslation();
  const { api } = useServices();
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const [asking, setAsking] = useState(false);

  async function trash() {
    try {
      await api.request<void>(
        'POST',
        `/groups/${encodeURIComponent(groupId)}/contacts/${encodeURIComponent(contactId)}/trash`,
      );
      navigate(`/groups/${groupId}`);
    } catch {
      // Already in Trash or no longer allowed: the screen below shows the contact as it is now.
    } finally {
      await queryClient.invalidateQueries({ queryKey: ['contacts', groupId] });
      await queryClient.invalidateQueries({ queryKey: ['trash', groupId] });
      await queryClient.invalidateQueries({ queryKey: ['contact', groupId, contactId] });
    }
  }

  return (
    <div className="mt-6">
      <Button type="button" variant="secondary" onClick={() => setAsking(true)}>
        {t('contact.delete')}
      </Button>
      {asking && (
        <ConfirmDialog
          title={t('contact.delete')}
          text={t('contact.deleteText')}
          confirm={t('contact.delete')}
          onConfirm={trash}
          onClose={() => setAsking(false)}
        />
      )}
    </div>
  );
}

/** The save of the form: create or save with the revision, and what each refusal does (R-005-03, R-005-08, R-005-10). */
function useSave(
  groupId: string,
  contact: Contact | undefined,
  setConflict: (conflict: Conflict | null) => void,
) {
  const { t } = useTranslation();
  const { api } = useServices();
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const [busy, setBusy] = useState(false);
  const [saved, setSaved] = useState(false);
  const [failure, setFailure] = useState<string | null>(null);
  const [problems, setProblems] = useState<Partial<Record<string, string>>>({});
  const base = `/groups/${encodeURIComponent(groupId)}/contacts`;

  async function submit(values: Values, tags: string[], attributes: Attribute[]) {
    setBusy(true);
    setSaved(false);
    setFailure(null);
    setProblems({});
    try {
      if (contact) {
        await api.request<Contact>('PUT', `${base}/${encodeURIComponent(contact.id)}`, {
          ...values,
          tags,
          attributes,
          revision: contact.revision,
        });
        await queryClient.invalidateQueries({ queryKey: ['contact', groupId, contact.id] });
        setSaved(true);
      } else {
        const created = await api.request<Contact>('POST', base, { ...values, tags, attributes });
        navigate(`/groups/${groupId}/contacts/${created.id}`, { replace: true });
      }
    } catch (error) {
      await refuse(error, values, tags, attributes);
    } finally {
      setBusy(false);
    }
  }

  async function refuse(error: unknown, values: Values, tags: string[], attributes: Attribute[]) {
    if (!(error instanceof ApiError)) return setFailure(t('error.INTERNAL_ERROR'));
    if (error.code === 'VALIDATION_FAILED') return setProblems(refusedFields(error));
    if (error.code === 'CONFLICT' && error.body.current) {
      return setConflict({
        current: error.body.current,
        mine: values,
        mineTags: tags,
        mineAttributes: attributes,
      });
    }
    if (error.code === 'CONTACT_IN_TRASH' && contact) {
      await queryClient.invalidateQueries({ queryKey: ['contact', groupId, contact.id] });
    }
    setFailure(errorText(t, error.code));
  }

  return {
    busy,
    saved,
    failure,
    problems,
    submit,
    /** Continues from the current values of the conflict (R-005-08). */
    reload(current: Contact, done: () => void) {
      queryClient.setQueryData(['contact', groupId, current.id], current);
      done();
    },
    leave: (to: string) => navigate(to),
  };
}
