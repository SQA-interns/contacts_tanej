import { useQueryClient } from '@tanstack/react-query';
import { createContext, use, useEffect, useState, type ReactNode } from 'react';
import { useServices } from '@/app/services';
import type { SessionUser } from '@/lib/auth';

/** `loading` until Firebase has said who is signed in (R-001-08: the session is restored on reload). */
export type Session =
  | { status: 'loading' }
  | { status: 'signedOut' }
  | { status: 'signedIn'; user: SessionUser };

const SessionContext = createContext<Session>({ status: 'loading' });

export function SessionProvider({ children }: { children: ReactNode }) {
  const { auth } = useServices();
  const queryClient = useQueryClient();
  const [session, setSession] = useState<Session>({ status: 'loading' });
  useEffect(
    () =>
      auth.observe((user) => {
        // Nothing of one person may stay in memory for the next person on the same browser (BR-03).
        if (!user) queryClient.clear();
        setSession(user ? { status: 'signedIn', user } : { status: 'signedOut' });
      }),
    [auth, queryClient],
  );
  return <SessionContext value={session}>{children}</SessionContext>;
}

export function useSession(): Session {
  return use(SessionContext);
}
