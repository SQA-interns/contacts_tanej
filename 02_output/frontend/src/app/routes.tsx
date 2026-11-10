import { useTranslation } from 'react-i18next';
import { Link, type RouteObject } from 'react-router';
import { AppShell } from '@/components/layout/AppShell';
import { Card } from '@/components/ui/card';
import { RegisterPage } from '@/features/auth/RegisterPage';
import { ResetPasswordPage } from '@/features/auth/ResetPasswordPage';
import { SignInPage } from '@/features/auth/SignInPage';
import { ContactPage } from '@/features/contacts/ContactPage';
import { ContactHistoryPage } from '@/features/history/ContactHistoryPage';
import { ContactListPage } from '@/features/contacts/ContactListPage';
import { TrashPage } from '@/features/contacts/TrashPage';
import { DeletedGroupsPage } from '@/features/groups/DeletedGroupsPage';
import { GroupPage } from '@/features/groups/GroupPage';
import { GroupsHome } from '@/features/groups/GroupsHome';
import { MembersTab } from '@/features/members/MembersTab';
import { ManageTagsPage } from '@/features/tags/ManageTagsPage';
import { PublicOnly, RequireSession, SessionRoot } from './guards';

function NotFoundPage() {
  const { t } = useTranslation();
  return (
    <Card>
      <h1 className="mb-2 text-2xl font-semibold">{t('notFound.title')}</h1>
      <p className="mb-4 text-text-muted">{t('notFound.text')}</p>
      <Link to="/" className="underline">
        {t('notFound.back')}
      </Link>
    </Card>
  );
}

/**
 * Route table (ADR-06). The sign-in screens are public; everything else sits behind the guard, under
 * `/groups/:groupId` for everything of one group (AR-04: the group id comes only from the path).
 */
export const routes: RouteObject[] = [
  {
    element: <SessionRoot />,
    children: [
      {
        element: <AppShell />,
        children: [
          {
            element: <PublicOnly />,
            children: [
              { path: 'sign-in', element: <SignInPage /> },
              { path: 'register', element: <RegisterPage /> },
              { path: 'reset-password', element: <ResetPasswordPage /> },
            ],
          },
          {
            element: <RequireSession />,
            children: [
              { index: true, element: <GroupsHome /> },
              { path: 'deleted-groups', element: <DeletedGroupsPage /> },
              {
                path: 'groups/:groupId',
                element: <GroupPage />,
                children: [
                  { index: true, element: <ContactListPage /> },
                  { path: 'trash', element: <TrashPage /> },
                  { path: 'tags', element: <ManageTagsPage /> },
                  { path: 'members', element: <MembersTab /> },
                  { path: 'contacts/new', element: <ContactPage /> },
                  { path: 'contacts/:contactId', element: <ContactPage /> },
                  { path: 'contacts/:contactId/history', element: <ContactHistoryPage /> },
                ],
              },
              { path: '*', element: <NotFoundPage /> },
            ],
          },
        ],
      },
    ],
  },
];
