import type { HTMLAttributes } from 'react';
import { cn } from '@/lib/utils';

/** UI-11, UI-07: related content in a card, 12 px corners, 1 px border, at most one shadow level. */
export function Card({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn('rounded-card border border-border bg-surface-raised p-6 shadow-card', className)}
      {...props}
    />
  );
}
