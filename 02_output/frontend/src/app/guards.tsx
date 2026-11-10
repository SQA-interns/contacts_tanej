import { Navigate, Outlet } from 'react-router';
import { StatePanel } from '@/components/ui/state-panel';
import { VerifyEmailPage } from '@/features/auth/VerifyEmailPage';
import { SessionProvider, useSession } from '@/features/auth/session';

/** Root route: makes the session known to every screen. */
export function SessionRoot() {
  return (
    <SessionProvider>
      <Outlet />
    </SessionProvider>
  );
}

/**
 * Route guard (R-001-05, R-001-08): nobody signed in goes to the sign-in screen; a person with an
 * unverified e-mail sees only the verify screen, at every address, and no group route renders.
 */
export function RequireSession() {
  const session = useSession();
  if (session.status === 'loading') return <StatePanel state="loading" />;
  if (session.status === 'signedOut') return <Navigate to="/sign-in" replace />;
  if (!session.user.emailVerified) return <VerifyEmailPage email={session.user.email} />;
  return <Outlet />;
}

/** The sign-in, register and reset screens: a signed-in person is sent on to the app. */
export function PublicOnly() {
  const session = useSession();
  if (session.status === 'loading') return <StatePanel state="loading" />;
  if (session.status === 'signedIn') return <Navigate to="/" replace />;
  return <Outlet />;
}
