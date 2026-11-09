import { useTranslation } from 'react-i18next';
import { Link, type RouteObject } from 'react-router';
import { AppShell } from '@/components/layout/AppShell';
import { Card } from '@/components/ui/card';

function HomePage() {
  const { t } = useTranslation();
  return (
    <Card>
      <h1 className="mb-2 text-2xl font-semibold">{t('home.title')}</h1>
      <p className="text-text-muted">{t('home.intro')}</p>
    </Card>
  );
}

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
 * Route table (ADR-06). The skeleton has the shell, a home page and "not found"; every screen of
 * the specification (User interface, Screen inventory) adds its route here, under `/groups/:groupId`
 * for everything of one group (AR-04: the group id comes only from the path).
 */
export const routes: RouteObject[] = [
  {
    element: <AppShell />,
    children: [
      { index: true, element: <HomePage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
];
