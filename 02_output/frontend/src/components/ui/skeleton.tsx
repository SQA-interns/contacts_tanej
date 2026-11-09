import type { HTMLAttributes } from 'react';
import { cn } from '@/lib/utils';

/** Loading state of UI-14 (and the wireframe's grey placeholder bar, UI-28). */
export function Skeleton({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      aria-hidden="true"
      className={cn('h-4 animate-pulse rounded-control bg-surface-raised', className)}
      {...props}
    />
  );
}
