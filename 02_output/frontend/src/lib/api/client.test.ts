import { ApiError, createApiClient } from './client';

function respond(status: number, body?: unknown, headers: Record<string, string> = {}) {
  return Promise.resolve(new Response(body === undefined ? null : JSON.stringify(body), { status, headers }));
}

describe('API client', () => {
  it('sends the ID token as a bearer token and returns the JSON body', async () => {
    const fetcher = vi.fn(() => respond(200, [{ id: 'g1' }]));
    const client = createApiClient('/api', async () => 'tok', fetcher as unknown as typeof fetch);
    expect(await client.request('GET', '/groups')).toEqual([{ id: 'g1' }]);
    const [url, init] = fetcher.mock.calls[0] as unknown as [string, RequestInit];
    expect(url).toBe('/api/groups');
    expect((init.headers as Record<string, string>).Authorization).toBe('Bearer tok');
  });

  it('sends no Authorization header without a token and returns nothing for 204', async () => {
    const fetcher = vi.fn(() => respond(204));
    const client = createApiClient('/api', async () => null, fetcher as unknown as typeof fetch);
    expect(await client.request('POST', '/groups/1/leave')).toBeUndefined();
    const init = (fetcher.mock.calls[0] as unknown as [string, RequestInit])[1];
    expect((init.headers as Record<string, string>).Authorization).toBeUndefined();
  });

  it('turns a failure into an ApiError with the code and Retry-After (R-017-10)', async () => {
    const body = { status: 429, code: 'RATE_LIMITED', message: 'Too many requests.' };
    const client = createApiClient('/api', async () => 't', (() =>
      respond(429, body, { 'Retry-After': '17' })) as unknown as typeof fetch);
    const error = await client.request('GET', '/groups').catch((e: unknown) => e);
    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).code).toBe('RATE_LIMITED');
    expect((error as ApiError).retryAfter).toBe(17);
  });

  it('keeps going when the failure body is not JSON', async () => {
    const client = createApiClient('/api', async () => 't', (() =>
      Promise.resolve(new Response('oops', { status: 502 }))) as unknown as typeof fetch);
    const error = (await client.request('GET', '/x').catch((e: unknown) => e)) as ApiError;
    expect(error.status).toBe(502);
    expect(error.code).toBe('INTERNAL_ERROR');
  });

  it('reads the file name of an export from Content-Disposition (R-011-06)', async () => {
    const client = createApiClient('/api', async () => 't', (() =>
      Promise.resolve(
        new Response('x', {
          status: 200,
          headers: {
            'Content-Disposition': "attachment; filename*=UTF-8''Konferenca%202026_2026-11-09.xlsx",
          },
        }),
      )) as unknown as typeof fetch);
    const { fileName } = await client.download('/groups/1/exports', { language: 'sl' });
    expect(fileName).toBe('Konferenca 2026_2026-11-09.xlsx');
  });

  it('falls back to export.xlsx without a Content-Disposition file name', async () => {
    const client = createApiClient('/api', async () => 't', (() =>
      Promise.resolve(new Response('x', { status: 200 }))) as unknown as typeof fetch);
    const { fileName, blob } = await client.download('/groups/1/exports', {});
    expect(fileName).toBe('export.xlsx');
    expect(blob.size).toBe(1);
  });

  it('sends the export request as a POST with a JSON body', async () => {
    const fetcher = vi.fn(() => Promise.resolve(new Response('x', { status: 200 })));
    const client = createApiClient('/api', async () => 't', fetcher as unknown as typeof fetch);
    await client.download('/groups/1/exports', { language: 'sl' });
    const [, init] = fetcher.mock.calls[0] as unknown as [string, RequestInit];
    expect(init.method).toBe('POST');
    expect(init.body).toBe('{"language":"sl"}');
  });

  it('asks for JSON and sets Content-Type only when it sends a body', async () => {
    const fetcher = vi.fn(() => respond(200, {}));
    const client = createApiClient('/api', async () => 't', fetcher as unknown as typeof fetch);
    await client.request('GET', '/a');
    await client.request('PUT', '/b', { n: 1 });
    const [, get] = fetcher.mock.calls[0] as unknown as [string, RequestInit];
    const [, put] = fetcher.mock.calls[1] as unknown as [string, RequestInit];
    expect(get.headers).toEqual({ Accept: 'application/json', Authorization: 'Bearer t' });
    expect(get.body).toBeUndefined();
    expect(put.headers).toEqual({
      Accept: 'application/json',
      Authorization: 'Bearer t',
      'Content-Type': 'application/json',
    });
    expect(put.body).toBe('{"n":1}');
    expect(put.method).toBe('PUT');
  });
});
