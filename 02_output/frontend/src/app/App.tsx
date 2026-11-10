import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useState } from 'react';
import { createBrowserRouter, RouterProvider } from 'react-router';
import { createApiClient } from '@/lib/api/client';
import { createAuthApi } from '@/lib/auth';
import { loadConfig } from '@/lib/config';
import { createAuth } from '@/lib/firebase';
import { routes } from './routes';
import { Services, type AppServices } from './services';

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: false, refetchOnWindowFocus: false } },
});

function createServices(): AppServices {
  const config = loadConfig();
  const auth = createAuthApi(createAuth(config));
  return { auth, api: createApiClient(config.apiBaseUrl, auth.token) };
}

export function App() {
  const [services] = useState(createServices);
  const [router] = useState(() => createBrowserRouter(routes));
  return (
    <QueryClientProvider client={queryClient}>
      <Services value={services}>
        <RouterProvider router={router} />
      </Services>
    </QueryClientProvider>
  );
}
