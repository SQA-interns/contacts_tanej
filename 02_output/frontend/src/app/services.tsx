import { createContext, use } from 'react';
import type { ApiClient } from '@/lib/api/client';
import type { AuthApi } from '@/lib/auth';

/** What the screens reach the outside world with: Firebase Authentication and the backend (AR-07). */
export interface AppServices {
  auth: AuthApi;
  api: ApiClient;
}

const ServicesContext = createContext<AppServices | null>(null);

export const Services = ServicesContext;

export function useServices(): AppServices {
  const services = use(ServicesContext);
  if (!services) throw new Error('Services are missing: wrap the app in <Services>');
  return services;
}
