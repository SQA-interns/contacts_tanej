import { X } from 'lucide-react';
import { createContext, useCallback, useContext, useEffect, useRef, useState, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';

/**
 * Toasts (post-run, D-83 option 3): a short confirmation, bottom corner, for actions that have no
 * visible confirmation of their own. Success only: errors stay inline (`role="alert"`), and actions
 * with an inline confirmation keep it. A toast closes after TOAST_MS, waits while hovered or
 * focused, and has a close button. Without a ToastProvider, notify does nothing.
 */
export const TOAST_MS = 5000;
const MAX_TOASTS = 3;

type Notify = (text: string) => void;
const ToastContext = createContext<Notify>(() => undefined);

export function useToast(): Notify {
  return useContext(ToastContext);
}

interface Toast {
  id: number;
  text: string;
}

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);
  const next = useRef(0);
  const close = useCallback((id: number) => setToasts((all) => all.filter((toast) => toast.id !== id)), []);
  const notify = useCallback<Notify>((text) => {
    next.current += 1;
    const id = next.current;
    setToasts((all) => [...all.slice(1 - MAX_TOASTS), { id, text }]);
  }, []);
  return (
    <ToastContext.Provider value={notify}>
      {children}
      <ToastList toasts={toasts} onClose={close} />
    </ToastContext.Provider>
  );
}

function ToastList({ toasts, onClose }: { toasts: Toast[]; onClose: (id: number) => void }) {
  const { t } = useTranslation();
  // The live region is always present, so a screen reader announces each toast that is added.
  return (
    <section
      aria-label={t('toast.region')}
      className="pointer-events-none fixed inset-x-4 bottom-4 z-50 flex flex-col items-end gap-2 sm:left-auto sm:w-96"
    >
      <div role="status" className="flex w-full flex-col gap-2">
        {toasts.map((toast) => (
          <ToastItem key={toast.id} toast={toast} onClose={onClose} />
        ))}
      </div>
    </section>
  );
}

function ToastItem({ toast, onClose }: { toast: Toast; onClose: (id: number) => void }) {
  const { t } = useTranslation();
  const [paused, setPaused] = useState(false);
  useEffect(() => {
    if (paused) return undefined;
    const timer = window.setTimeout(() => onClose(toast.id), TOAST_MS);
    return () => window.clearTimeout(timer);
  }, [paused, onClose, toast.id]);
  return (
    <div
      className="pointer-events-auto flex w-full items-start gap-3 rounded-card border border-border border-l-4 border-l-success bg-surface-raised p-4 shadow-card"
      onMouseEnter={() => setPaused(true)}
      onMouseLeave={() => setPaused(false)}
      onFocus={() => setPaused(true)}
      onBlur={() => setPaused(false)}
    >
      <p className="flex-1">{toast.text}</p>
      <button
        type="button"
        aria-label={t('toast.close')}
        onClick={() => onClose(toast.id)}
        className="rounded-control p-1 text-text-muted hover:text-text focus-visible:outline-2 focus-visible:outline-offset-2"
      >
        <X aria-hidden="true" className="size-4" />
      </button>
    </div>
  );
}
