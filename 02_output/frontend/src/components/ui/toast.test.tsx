import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { I18nextProvider } from 'react-i18next';
import { setupI18n } from '@/lib/i18n';
import { TOAST_MS, ToastProvider, useToast } from './toast';

function Trigger({ text }: { text: string }) {
  const toast = useToast();
  return (
    <button type="button" onClick={() => toast(text)}>
      act
    </button>
  );
}

function show(text = 'The tag is deleted.') {
  render(
    <I18nextProvider i18n={setupI18n('en')}>
      <ToastProvider>
        <Trigger text={text} />
      </ToastProvider>
    </I18nextProvider>,
  );
}

describe('toasts (post-run, D-83)', () => {
  afterEach(() => vi.useRealTimers());

  it('announce the text in a status region and close with the button', async () => {
    show();
    await userEvent.click(screen.getByRole('button', { name: 'act' }));
    expect(screen.getByRole('status')).toHaveTextContent('The tag is deleted.');
    await userEvent.click(screen.getByRole('button', { name: 'Close notification' }));
    expect(screen.getByRole('status')).toBeEmptyDOMElement();
  });

  it('close by themselves after a few seconds', () => {
    vi.useFakeTimers();
    show();
    act(() => screen.getByRole('button', { name: 'act' }).click());
    expect(screen.getByRole('status')).toHaveTextContent('The tag is deleted.');
    act(() => vi.advanceTimersByTime(TOAST_MS));
    expect(screen.getByRole('status')).toBeEmptyDOMElement();
  });

  it('keep at most three at once', () => {
    show();
    const button = screen.getByRole('button', { name: 'act' });
    for (let i = 0; i < 5; i += 1) act(() => button.click());
    expect(screen.getAllByRole('button', { name: 'Close notification' })).toHaveLength(3);
  });

  it('do nothing without a provider', async () => {
    render(<Trigger text="x" />);
    await userEvent.click(screen.getByRole('button', { name: 'act' }));
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });
});
