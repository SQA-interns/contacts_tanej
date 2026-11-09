import { initializeApp } from 'firebase/app';
import { connectAuthEmulator, getAuth, type Auth } from 'firebase/auth';
import type { AppConfig } from './config';

/**
 * Firebase is used for Authentication only (AR-07, Q-16d): sign-in, session, ID token and account
 * deletion. The emulator is connected only when its address is configured (test and local).
 */
export function createAuth(config: AppConfig): Auth {
  const app = initializeApp({
    apiKey: config.firebaseApiKey,
    authDomain: config.firebaseAuthDomain,
    projectId: config.firebaseProjectId,
  });
  const auth = getAuth(app);
  if (config.firebaseAuthEmulatorUrl) {
    connectAuthEmulator(auth, config.firebaseAuthEmulatorUrl, { disableWarnings: true });
  }
  return auth;
}
