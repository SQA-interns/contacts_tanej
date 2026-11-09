import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

/** Class names of the shadcn/ui components (UI-01). */
export function cn(...inputs: ClassValue[]): string {
  return twMerge(clsx(inputs));
}
