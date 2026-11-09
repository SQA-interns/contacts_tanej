import { Slot } from 'radix-ui';
import { cva, type VariantProps } from 'class-variance-authority';
import { Loader2 } from 'lucide-react';
import type { ButtonHTMLAttributes } from 'react';
import { cn } from '@/lib/utils';

/** UI-12: three levels, visible hover, active, focus and disabled states, and a busy state. */
const buttonVariants = cva(
  'inline-flex min-h-11 min-w-11 items-center justify-center gap-2 rounded-control border px-4 text-base font-semibold ' +
    'transition-colors disabled:pointer-events-none disabled:opacity-50 active:translate-y-px',
  {
    variants: {
      variant: {
        primary: 'border-primary bg-primary text-primary-foreground hover:opacity-90',
        secondary: 'border-border bg-surface text-text hover:bg-surface-raised',
        ghost: 'border-transparent bg-transparent text-text hover:bg-surface',
      },
    },
    defaultVariants: { variant: 'primary' },
  },
);

export interface ButtonProps
  extends ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {
  asChild?: boolean;
  /** Shows a spinner and blocks a second click while an action runs. */
  busy?: boolean;
}

export function Button({
  className,
  variant,
  asChild = false,
  busy = false,
  children,
  disabled,
  ...props
}: ButtonProps) {
  const Component = asChild ? Slot.Root : 'button';
  return (
    <Component
      className={cn(buttonVariants({ variant }), className)}
      disabled={disabled || busy}
      aria-busy={busy || undefined}
      {...props}
    >
      {busy && <Loader2 aria-hidden="true" className="size-4 animate-spin" />}
      {children}
    </Component>
  );
}
