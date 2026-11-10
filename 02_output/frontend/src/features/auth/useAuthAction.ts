import { useState } from 'react';
import { authErrorKey, type AuthErrorKey } from '@/lib/auth';

/** Runs one Firebase call with a busy flag (UI-12) and the text of its refusal (R-001-07). */
export function useAuthAction() {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<AuthErrorKey | null>(null);

  async function run(action: () => Promise<void>): Promise<boolean> {
    setBusy(true);
    setError(null);
    try {
      await action();
      return true;
    } catch (failure) {
      setError(authErrorKey(failure));
      return false;
    } finally {
      setBusy(false);
    }
  }

  return { busy, error, run };
}
