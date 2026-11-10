import {
  GoogleAuthProvider,
  browserLocalPersistence,
  createUserWithEmailAndPassword,
  deleteUser,
  onAuthStateChanged,
  sendEmailVerification,
  sendPasswordResetEmail,
  setPersistence,
  signInWithEmailAndPassword,
  signInWithPopup,
  signOut,
  updateProfile,
  type Auth,
  type User,
} from 'firebase/auth';

/** What the screens know about the signed-in person. */
export interface SessionUser {
  email: string;
  emailVerified: boolean;
  name: string | null;
}

export interface Registration {
  name: string;
  email: string;
  password: string;
}

/** The only door to Firebase Authentication for the screens (AR-07); tests replace it with a fake. */
export interface AuthApi {
  /** Calls back with the person now and on every change; `null` means signed out. Returns the unsubscribe function. */
  observe(onChange: (user: SessionUser | null) => void): () => void;
  register(input: Registration): Promise<void>;
  signIn(email: string, password: string): Promise<void>;
  signInWithGoogle(): Promise<void>;
  sendPasswordReset(email: string): Promise<void>;
  resendVerification(): Promise<void>;
  signOut(): Promise<void>;
  /** Deletes the Firebase account of the signed-in person (US-016); the product data is deleted first. */
  deleteAccount(): Promise<void>;
  token(): Promise<string | null>;
}

function toSessionUser(user: User): SessionUser {
  return { email: user.email ?? '', emailVerified: user.emailVerified, name: user.displayName };
}

export function createAuthApi(auth: Auth): AuthApi {
  // R-001-08: the SDK's own store keeps the session across page reloads (SR-20).
  void setPersistence(auth, browserLocalPersistence);

  function currentUser(): User {
    if (!auth.currentUser) throw new Error('Not signed in');
    return auth.currentUser;
  }

  return {
    observe(onChange) {
      return onAuthStateChanged(auth, async (user) => {
        // EX-001-07: the stored copy of the person may predate the click on the verification link.
        if (user && !user.emailVerified) await user.reload().catch(() => undefined);
        onChange(user ? toSessionUser(user) : null);
      });
    },
    async register({ name, email, password }) {
      const { user } = await createUserWithEmailAndPassword(auth, email, password);
      await updateProfile(user, { displayName: name });
      await sendEmailVerification(user);
      // The name reaches the `name` claim of the token only after a refresh (specification 4.1).
      await user.getIdToken(true);
    },
    async signIn(email, password) {
      await signInWithEmailAndPassword(auth, email, password);
    },
    async signInWithGoogle() {
      await signInWithPopup(auth, new GoogleAuthProvider());
    },
    async sendPasswordReset(email) {
      await sendPasswordResetEmail(auth, email);
    },
    async resendVerification() {
      await sendEmailVerification(currentUser());
    },
    async signOut() {
      await signOut(auth);
    },
    async deleteAccount() {
      await deleteUser(currentUser());
    },
    async token() {
      return auth.currentUser ? auth.currentUser.getIdToken() : null;
    },
  };
}

/** The suffix of `auth.error.<key>` in the language files. */
export type AuthErrorKey =
  | 'wrongCredentials'
  | 'emailInUse'
  | 'passwordTooShort'
  | 'emailInvalid'
  | 'tooManyRequests'
  | 'network'
  | 'generic';

const BY_CODE: Record<string, AuthErrorKey> = {
  'auth/invalid-credential': 'wrongCredentials',
  'auth/wrong-password': 'wrongCredentials',
  'auth/user-not-found': 'wrongCredentials',
  'auth/email-already-in-use': 'emailInUse',
  'auth/weak-password': 'passwordTooShort',
  'auth/invalid-email': 'emailInvalid',
  'auth/too-many-requests': 'tooManyRequests',
  'auth/network-request-failed': 'network',
};

const SILENT_CODES = ['auth/popup-closed-by-user', 'auth/cancelled-popup-request'];

/** R-001-07: the text of a Firebase refusal; `null` when the person only closed the Google window. */
export function authErrorKey(error: unknown): AuthErrorKey | null {
  const code = (error as { code?: unknown } | null | undefined)?.code;
  if (typeof code !== 'string') return 'generic';
  if (SILENT_CODES.includes(code)) return null;
  return BY_CODE[code] ?? 'generic';
}
