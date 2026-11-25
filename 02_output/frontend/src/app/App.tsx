import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useState } from 'react';
import { createBrowserRouter, RouterProvider } from 'react-router';
import { createApiClient } from '@/lib/api/client';
import { WakeBanner } from '@/components/layout/WakeBanner';
import { ToastProvider } from '@/components/ui/toast';
import { createAuthApi } from '@/lib/auth';
import { wakeTracker } from '@/lib/api/waking';
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
  // Post-run: the free hosting plan stops the backend after 15 minutes without traffic, and its start
  // took from 1 to 5 minutes. One health request as soon as the page opens starts the wake-up while the
  // person signs in; the API client then waits up to 5 minutes instead of showing an error.
  void fetch(`${config.apiBaseUrl}/health`).catch(() => undefined);
  const api = createApiClient(config.apiBaseUrl, auth.token, fetch, {
    maxWaitMs: 300_000,
    onWaiting: wakeTracker.change,
  });
  return { auth, api, demoTools: config.demoTools };
}

export function App() {
  const [services] = useState(createServices);
  const [router] = useState(() => createBrowserRouter(routes));
  return (
    <QueryClientProvider client={queryClient}>
      <Services value={services}>
        <ToastProvider>
          <RouterProvider router={router} />
          <WakeBanner />
        </ToastProvider>
      </Services>
    </QueryClientProvider>
  );
}
