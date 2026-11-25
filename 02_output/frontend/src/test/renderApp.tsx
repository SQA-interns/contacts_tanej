import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render } from '@testing-library/react';
import { I18nextProvider } from 'react-i18next';
import { createMemoryRouter, RouterProvider } from 'react-router';
import { Services, type AppServices } from '@/app/services';
import { routes } from '@/app/routes';
import type { ApiClient } from '@/lib/api/client';
import { setupI18n } from '@/lib/i18n';
import { createFakeAuth, type FakeAuth } from './fakeAuth';

export function fakeApi(groups: unknown[] = []): ApiClient {
  return {
    request: (async (_method: string, path: string) =>
      path === '/groups' ? groups : undefined) as ApiClient['request'],
    download: async () => ({ blob: new Blob(), fileName: 'x.xlsx' }),
  };
}

/** Renders the whole route table at an address with a fake Firebase; the language is English unless told. */
export function renderApp(
  path: string,
  options: { auth?: FakeAuth; api?: ApiClient; language?: string; demoTools?: boolean } = {},
) {
  const auth = options.auth ?? createFakeAuth();
  const services: AppServices = { auth, api: options.api ?? fakeApi(), demoTools: options.demoTools };
  const i18n = setupI18n(options.language ?? 'en');
  render(
    <I18nextProvider i18n={i18n}>
      <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
        <Services value={services}>
          <RouterProvider router={createMemoryRouter(routes, { initialEntries: [path] })} />
        </Services>
      </QueryClientProvider>
    </I18nextProvider>,
  );
  return { auth };
}
