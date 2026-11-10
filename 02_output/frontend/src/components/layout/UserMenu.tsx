import { User } from 'lucide-react';
import { DropdownMenu } from 'radix-ui';
import { useTranslation } from 'react-i18next';
import { useServices } from '@/app/services';
import { Button } from '@/components/ui/button';

/** The person's menu in the header: sign out ends the Firebase session (R-001-08). */
export function UserMenu({ email }: { email: string }) {
  const { t } = useTranslation();
  const { auth } = useServices();
  return (
    <DropdownMenu.Root>
      <DropdownMenu.Trigger asChild>
        <Button variant="ghost" aria-label={t('header.userMenu')}>
          <User aria-hidden="true" className="size-5" />
        </Button>
      </DropdownMenu.Trigger>
      <DropdownMenu.Portal>
        <DropdownMenu.Content
          align="end"
          className="min-w-48 rounded-control border border-border bg-surface-raised p-1 shadow-card"
        >
          <DropdownMenu.Label className="px-3 py-2 text-sm text-text-muted">{email}</DropdownMenu.Label>
          <DropdownMenu.Item
            className="flex min-h-11 cursor-pointer items-center rounded-control px-3 outline-none data-[highlighted]:bg-surface"
            onSelect={() => void auth.signOut()}
          >
            {t('auth.signOut')}
          </DropdownMenu.Item>
        </DropdownMenu.Content>
      </DropdownMenu.Portal>
    </DropdownMenu.Root>
  );
}
