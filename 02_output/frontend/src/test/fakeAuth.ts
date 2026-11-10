import type { AuthApi, Registration, SessionUser } from '@/lib/auth';

/** A stand-in for Firebase in component tests: it records the calls and lets a test change the person. */
export interface FakeAuth extends AuthApi {
  calls: string[];
  /** Makes the next call that talks to Firebase fail with this error. */
  failNextWith(error: unknown): void;
  /** Pretends the stored session is this person (or nobody) once the app observes it. */
  setUser(user: SessionUser | null): void;
}

export const ANA: SessionUser = { email: 'ana.novak@example.com', emailVerified: true, name: 'Ana Novak' };

export function createFakeAuth(initial: SessionUser | null = null): FakeAuth {
  let user = initial;
  let failure: unknown;
  const listeners = new Set<(user: SessionUser | null) => void>();
  const notify = () => listeners.forEach((listener) => listener(user));
  const calls: string[] = [];

  async function call(name: string, after?: () => void) {
    calls.push(name);
    if (failure) {
      const error = failure;
      failure = undefined;
      throw error;
    }
    after?.();
  }

  return {
    calls,
    failNextWith: (error) => {
      failure = error;
    },
    setUser(next) {
      user = next;
      notify();
    },
    observe(onChange) {
      listeners.add(onChange);
      onChange(user);
      return () => listeners.delete(onChange);
    },
    register: (input: Registration) =>
      call(`register ${input.name} ${input.email} ${input.password}`, () => {
        user = { email: input.email, emailVerified: false, name: input.name };
        notify();
      }),
    signIn: (email, password) =>
      call(`signIn ${email} ${password}`, () => {
        user = { ...ANA, email };
        notify();
      }),
    signInWithGoogle: () =>
      call('signInWithGoogle', () => {
        user = ANA;
        notify();
      }),
    sendPasswordReset: (email) => call(`sendPasswordReset ${email}`),
    resendVerification: () => call('resendVerification'),
    signOut: () =>
      call('signOut', () => {
        user = null;
        notify();
      }),
    token: async () => (user ? 'token' : null),
  };
}
