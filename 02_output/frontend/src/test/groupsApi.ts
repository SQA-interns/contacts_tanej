import { ApiError, type ApiClient } from '@/lib/api/client';
import type { components } from '@/lib/api/schema';
import { ANA, createFakeAuth } from '@/test/fakeAuth';

export type GroupSummary = components['schemas']['GroupSummary'];

export function failure(status: number, code: components['schemas']['ErrorCode']) {
  return new ApiError(status, { status, code, message: 'x' });
}

/** A backend stand-in that keeps the groups of one person and records every call. */
export function groupsApi(initial: GroupSummary[] = [], options: { hold?: Promise<void> } = {}) {
  const groups = [...initial];
  const calls: string[] = [];
  let created = 0;
  const api: ApiClient = {
    request: (async (method: string, path: string, body?: { name?: string }) => {
      calls.push(`${method} ${path} ${body ? JSON.stringify(body) : ''}`.trim());
      if (path === '/groups' && method === 'GET') return groups.map((g) => ({ ...g }));
      if (path === '/groups' && method === 'POST') {
        await options.hold;
        const name = body?.name ?? '';
        if (groups.some((g) => g.role === 'OWNER' && g.name.toLowerCase() === name.toLowerCase())) {
          throw failure(409, 'GROUP_NAME_TAKEN');
        }
        const group: GroupSummary = { id: `new-${++created}`, name, role: 'OWNER' };
        groups.push(group);
        return { ...group };
      }
      const one = /^\/groups\/([^/]+)$/.exec(path);
      const group = groups.find((g) => g.id === one?.[1]);
      if (!group) throw failure(404, 'NOT_FOUND');
      if (method === 'PATCH') {
        if (groups.some((g) => g !== group && g.name.toLowerCase() === (body?.name ?? '').toLowerCase())) {
          throw failure(409, 'GROUP_NAME_TAKEN');
        }
        group.name = body?.name ?? group.name;
      }
      return { ...group };
    }) as ApiClient['request'],
    download: async () => ({ blob: new Blob(), fileName: 'x.xlsx' }),
  };
  return { api, calls, groups };
}

export const konferenca: GroupSummary = { id: 'g-1', name: 'Konferenca 2026', role: 'OWNER' };
export const poroka: GroupSummary = { id: 'g-2', name: 'Poroka', role: 'EDITOR' };
export const skupno: GroupSummary = { id: 'g-3', name: 'Skupno', role: 'VIEWER' };

export const signedIn = () => createFakeAuth(ANA);
