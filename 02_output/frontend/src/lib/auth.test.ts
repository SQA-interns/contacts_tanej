import type { Auth } from 'firebase/auth';
import * as firebaseAuth from 'firebase/auth';
import { authErrorKey, createAuthApi } from './auth';

vi.mock('firebase/auth', () => ({
  createUserWithEmailAndPassword: vi.fn(),
  signInWithEmailAndPassword: vi.fn(),
  signInWithPopup: vi.fn(),
  sendPasswordResetEmail: vi.fn(),
  sendEmailVerification: vi.fn(),
  updateProfile: vi.fn(),
  signOut: vi.fn(),
  onAuthStateChanged: vi.fn(),
  setPersistence: vi.fn(),
  browserLocalPersistence: 'local',
  GoogleAuthProvider: class {},
}));

const fb = vi.mocked(firebaseAuth);

function fakeAuth(user: unknown = null): Auth {
  return { currentUser: user } as unknown as Auth;
}

describe('createAuthApi', () => {
  beforeEach(() => vi.clearAllMocks());

  it('registers, sets the name, sends the verification e-mail and refreshes the token (R-001-02, R-001-04)', async () => {
    const user = { getIdToken: vi.fn() };
    fb.createUserWithEmailAndPassword.mockResolvedValue({ user } as never);
    const auth = fakeAuth();
    await createAuthApi(auth).register({ name: 'Ana Novak', email: 'a@x.si', password: 'Geslo1234' });
    expect(fb.createUserWithEmailAndPassword).toHaveBeenCalledWith(auth, 'a@x.si', 'Geslo1234');
    expect(fb.updateProfile).toHaveBeenCalledWith(user, { displayName: 'Ana Novak' });
    expect(fb.sendEmailVerification).toHaveBeenCalledWith(user);
    expect(user.getIdToken).toHaveBeenCalledWith(true);
  });

  it('signs in with e-mail and password and with Google (R-001-01)', async () => {
    const auth = fakeAuth();
    const api = createAuthApi(auth);
    await api.signIn('a@x.si', 'Geslo1234');
    expect(fb.signInWithEmailAndPassword).toHaveBeenCalledWith(auth, 'a@x.si', 'Geslo1234');
    await api.signInWithGoogle();
    expect(fb.signInWithPopup).toHaveBeenCalledWith(auth, expect.any(Object));
  });

  it('starts the password reset flow (R-001-06)', async () => {
    const auth = fakeAuth();
    await createAuthApi(auth).sendPasswordReset('a@x.si');
    expect(fb.sendPasswordResetEmail).toHaveBeenCalledWith(auth, 'a@x.si');
  });

  it('sends the verification e-mail again to the signed-in user (EX-001-06)', async () => {
    const user = { email: 'a@x.si' };
    await createAuthApi(fakeAuth(user)).resendVerification();
    expect(fb.sendEmailVerification).toHaveBeenCalledWith(user);
  });

  it('ends the session on sign out (R-001-08)', async () => {
    const auth = fakeAuth();
    await createAuthApi(auth).signOut();
    expect(fb.signOut).toHaveBeenCalledWith(auth);
  });

  it('keeps the session in the SDK store across reloads (R-001-08)', () => {
    const auth = fakeAuth();
    createAuthApi(auth);
    expect(fb.setPersistence).toHaveBeenCalledWith(auth, 'local');
  });

  it('gives the ID token of the current user, or none when signed out', async () => {
    expect(await createAuthApi(fakeAuth()).token()).toBeNull();
    const user = { getIdToken: vi.fn().mockResolvedValue('tok') };
    expect(await createAuthApi(fakeAuth(user)).token()).toBe('tok');
  });

  it('maps the signed-in user and reloads an unverified one, so a verification in another tab shows (EX-001-07)', async () => {
    const reload = vi.fn().mockResolvedValue(undefined);
    const user = { email: 'a@x.si', emailVerified: false, displayName: 'Ana', reload };
    let emit: (u: unknown) => Promise<void> | void = () => undefined;
    fb.onAuthStateChanged.mockImplementation(((_auth: unknown, callback: typeof emit) => {
      emit = callback;
      return () => undefined;
    }) as never);
    const seen: unknown[] = [];
    createAuthApi(fakeAuth()).observe((u) => seen.push(u));
    await emit(user);
    expect(reload).toHaveBeenCalled();
    expect(seen).toEqual([{ email: 'a@x.si', emailVerified: false, name: 'Ana' }]);
    await emit(null);
    expect(seen[1]).toBeNull();
  });
});

describe('authErrorKey', () => {
  it.each([
    ['auth/invalid-credential', 'wrongCredentials'],
    ['auth/wrong-password', 'wrongCredentials'],
    ['auth/user-not-found', 'wrongCredentials'],
    ['auth/email-already-in-use', 'emailInUse'],
    ['auth/weak-password', 'passwordTooShort'],
    ['auth/invalid-email', 'emailInvalid'],
    ['auth/too-many-requests', 'tooManyRequests'],
    ['auth/network-request-failed', 'network'],
    ['auth/something-new', 'generic'],
  ])('maps %s to %s', (code, key) => {
    expect(authErrorKey({ code })).toBe(key);
  });

  it('keeps quiet when the person closes the Google window', () => {
    expect(authErrorKey({ code: 'auth/popup-closed-by-user' })).toBeNull();
    expect(authErrorKey({ code: 'auth/cancelled-popup-request' })).toBeNull();
  });

  it('uses the generic text for a value that is not a Firebase error', () => {
    expect(authErrorKey(new Error('boom'))).toBe('generic');
    expect(authErrorKey(undefined)).toBe('generic');
  });
});
