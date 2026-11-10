import { Menu } from 'lucide-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, NavLink, Outlet } from 'react-router';
import { Button } from '@/components/ui/button';
import { useSession } from '@/features/auth/session';
import { cn } from '@/lib/utils';
import { LanguageSwitch } from './LanguageSwitch';
import { UserMenu } from './UserMenu';

const NAV = [
  { to: '/', key: 'nav.groups' },
  { to: '/deleted-groups', key: 'nav.deletedGroups' },
  { to: '/account', key: 'nav.account' },
] as const;

/**
 * Header with the product name and navigation, one main region and a footer (UI-10, UI-19, UI-27).
 * Mobile: navigation collapses into a menu button (UI-23); content at most 1200 px wide (UI-09).
 */
export function AppShell() {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const session = useSession();
  // R-001-05: no navigation to groups until the e-mail is verified.
  const verified = session.status === 'signedIn' && session.user.emailVerified;
  return (
    <div className="flex min-h-screen flex-col bg-background text-text">
      <header className="border-b border-border bg-surface">
        <div className="mx-auto flex max-w-[1200px] flex-wrap items-center gap-4 px-4 py-3 md:px-6">
          <Link to="/" className="flex min-h-11 items-center gap-2 text-xl font-semibold">
            <span
              aria-hidden="true"
              className="size-8 rounded-control border border-border bg-surface-raised"
            />
            {t('app.name')}
          </Link>
          {verified && (
            <>
              <Button
                variant="ghost"
                className="ml-auto md:hidden"
                aria-expanded={open}
                aria-controls="main-nav"
                onClick={() => setOpen((value) => !value)}
              >
                <Menu aria-hidden="true" className="size-5" />
                {t('header.menu')}
              </Button>
              <nav
                id="main-nav"
                aria-label={t('header.menu')}
                className={cn(
                  'w-full flex-col gap-1 md:ml-4 md:flex md:w-auto md:flex-1 md:flex-row',
                  open ? 'flex' : 'hidden',
                )}
              >
                {NAV.map(({ to, key }) => (
                  <NavLink
                    key={to}
                    to={to}
                    end={to === '/'}
                    className={({ isActive }) =>
                      cn(
                        'flex min-h-11 items-center rounded-control px-3',
                        isActive ? 'bg-surface-raised font-semibold' : 'hover:bg-surface-raised',
                      )
                    }
                  >
                    {t(key)}
                  </NavLink>
                ))}
              </nav>
            </>
          )}
          <div className={cn('flex items-center gap-1', !verified && 'ml-auto')}>
            <LanguageSwitch />
            {session.status === 'signedIn' && verified && <UserMenu email={session.user.email} />}
          </div>
        </div>
      </header>
      <main className="mx-auto w-full max-w-[1200px] flex-1 px-4 py-6 md:px-6">
        <Outlet />
      </main>
      <footer className="border-t border-border bg-surface">
        <div className="mx-auto max-w-[1200px] px-4 py-4 text-sm text-text-muted md:px-6">
          {t('footer.text')}
        </div>
      </footer>
    </div>
  );
}
