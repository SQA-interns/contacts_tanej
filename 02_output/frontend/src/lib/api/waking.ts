import { useSyncExternalStore } from 'react';

/**
 * Post-run: whether a request is waiting for a sleeping backend (the free hosting plan stops it after
 * 15 minutes without traffic; the first request then waits about a minute). The API client counts the
 * waiting requests; the banner shows while the count is above zero.
 */
let waiting = 0;
const listeners = new Set<() => void>();

export const wakeTracker = {
  change(delta: 1 | -1): void {
    waiting = Math.max(0, waiting + delta);
    listeners.forEach((listener) => listener());
  },
  subscribe(listener: () => void): () => void {
    listeners.add(listener);
    return () => listeners.delete(listener);
  },
  isWaking: (): boolean => waiting > 0,
};

export function useWaking(): boolean {
  return useSyncExternalStore(wakeTracker.subscribe, wakeTracker.isWaking);
}
