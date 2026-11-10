import { useState } from 'react';
import { useServices } from '@/app/services';
import { ApiError } from '@/lib/api/client';
import type { components } from '@/lib/api/schema';

type ExportRequest = components['schemas']['ExportRequest'];

export type ExportState = 'idle' | 'busy' | 'done' | 'tooMany' | 'error';

/** Saves a downloaded file under its name by a click on a temporary link. */
function save(blob: Blob, fileName: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.append(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

/** Asks the backend for the .xlsx file and hands it to the browser (R-011-04, R-011-09). */
export function useExport(groupId: string) {
  const { api } = useServices();
  const [state, setState] = useState<ExportState>('idle');

  async function run(request: ExportRequest) {
    setState('busy');
    try {
      const file = await api.download(`/groups/${encodeURIComponent(groupId)}/exports`, request);
      save(file.blob, file.fileName);
      setState('done');
    } catch (error) {
      const tooMany =
        error instanceof ApiError && error.body.errors?.some((e) => e.code === 'TOO_MANY_CONTACTS');
      setState(tooMany ? 'tooMany' : 'error');
    }
  }

  return { state, run, reset: () => setState('idle') };
}
