import type { components } from './schema';

export type ErrorResponse = components['schemas']['ErrorResponse'];
export type ErrorCode = components['schemas']['ErrorCode'];

/** A failed API call in the one error format (R-017-10). The text shown comes from `error.<code>` in the language files. */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly body: ErrorResponse,
    readonly retryAfter?: number,
  ) {
    super(body.message);
  }

  get code(): ErrorCode {
    return this.body.code;
  }
}

export type TokenProvider = () => Promise<string | null>;

export interface ApiClient {
  request<T>(method: string, path: string, body?: unknown): Promise<T>;
  /** For the export: the file and its name from Content-Disposition (R-011-06). */
  download(path: string, body: unknown): Promise<{ blob: Blob; fileName: string }>;
}

/** The web app reaches the backend only through this client and the generated types (AR-07, AR-13). */
export function createApiClient(
  baseUrl: string,
  token: TokenProvider,
  fetcher: typeof fetch = fetch,
): ApiClient {
  async function send(method: string, path: string, body?: unknown): Promise<Response> {
    const headers: Record<string, string> = { Accept: 'application/json' };
    const bearer = await token();
    if (bearer) headers.Authorization = `Bearer ${bearer}`;
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    const response = await fetcher(`${baseUrl}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
    if (!response.ok) {
      const retry = response.headers.get('Retry-After');
      throw new ApiError(response.status, await errorBody(response), retry ? Number(retry) : undefined);
    }
    return response;
  }

  return {
    async request<T>(method: string, path: string, body?: unknown): Promise<T> {
      const response = await send(method, path, body);
      return (response.status === 204 ? undefined : await response.json()) as T;
    },
    async download(path, body) {
      const response = await send('POST', path, body);
      const header = response.headers.get('Content-Disposition') ?? '';
      const encoded = /filename\*=UTF-8''([^;]+)/.exec(header)?.[1];
      return { blob: await response.blob(), fileName: encoded ? decodeURIComponent(encoded) : 'export.xlsx' };
    },
  };
}

async function errorBody(response: Response): Promise<ErrorResponse> {
  try {
    return (await response.json()) as ErrorResponse;
  } catch {
    return { status: response.status, code: 'INTERNAL_ERROR', message: response.statusText };
  }
}
