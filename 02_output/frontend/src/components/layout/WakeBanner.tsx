import { LoaderCircle } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useWaking } from '@/lib/api/waking';

/** Post-run: says that the sleeping backend is starting, while the API client waits for it. */
export function WakeBanner() {
  const { t } = useTranslation();
  const waking = useWaking();
  return (
    <div
      role="status"
      className="pointer-events-none fixed inset-x-4 bottom-4 z-50 flex justify-center empty:hidden"
    >
      {waking && (
        <p className="flex max-w-xl items-center gap-3 rounded-card border border-border bg-surface-raised p-4 shadow-card">
          <LoaderCircle aria-hidden="true" className="size-5 shrink-0 animate-spin" />
          {t('wake.text')}
        </p>
      )}
    </div>
  );
}
