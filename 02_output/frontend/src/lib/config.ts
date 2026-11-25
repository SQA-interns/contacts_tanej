/** Runtime configuration read from `/config.js` (NFR-12, ADR-06); nothing is baked into the build. */
export interface AppConfig {
  firebaseProjectId: string;
  firebaseApiKey: string;
  firebaseAuthDomain: string;
  /** Empty in production (environments.md): the Authentication Emulator exists only in test and local. */
  firebaseAuthEmulatorUrl: string;
  apiBaseUrl: string;
  /** Demo deployments only: shows "Fill with test data" (post-run). False in every customer installation. */
  demoTools: boolean;
}

declare global {
  interface Window {
    __APP_CONFIG__?: Partial<AppConfig>;
  }
}

const REQUIRED: (keyof AppConfig)[] = [
  'firebaseProjectId',
  'firebaseApiKey',
  'firebaseAuthDomain',
  'apiBaseUrl',
];

export function loadConfig(source: Partial<AppConfig> | undefined = window.__APP_CONFIG__): AppConfig {
  const missing = REQUIRED.filter((key) => !source?.[key]);
  if (missing.length > 0) {
    throw new Error(`Missing configuration: ${missing.join(', ')}`);
  }
  return { firebaseAuthEmulatorUrl: '', ...source, demoTools: source?.demoTools === true } as AppConfig;
}
