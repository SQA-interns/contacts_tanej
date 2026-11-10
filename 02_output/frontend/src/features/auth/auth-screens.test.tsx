import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ANA, createFakeAuth } from '@/test/fakeAuth';
import { renderApp } from '@/test/renderApp';

const heading = (name: string) => screen.findByRole('heading', { name });

/** The label reads "E-mail (required)"; tests match its start. */
const field = (name: string) => screen.findByLabelText(new RegExp(`^${name}`));

describe('sign in (R-001-01, R-001-07, AC-001-05)', () => {
  it('asks a signed-out person to sign in, whatever the address', async () => {
    renderApp('/groups/42');
    expect(await heading('Sign in')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Sign in with Google' })).toBeInTheDocument();
  });

  it('signs in with e-mail and password and shows the empty state', async () => {
    const { auth } = renderApp('/sign-in');
    await userEvent.type(await field('E-mail'), ANA.email);
    await userEvent.type(screen.getByLabelText(/^Password/), 'Geslo1234');
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }));
    expect(await heading('Create a group')).toBeInTheDocument();
    expect(auth.calls).toContain(`signIn ${ANA.email} Geslo1234`);
  });

  it('shows an alert and stays when Firebase refuses the password', async () => {
    const auth = createFakeAuth();
    auth.failNextWith({ code: 'auth/invalid-credential' });
    renderApp('/sign-in', { auth });
    await userEvent.type(await field('E-mail'), ANA.email);
    await userEvent.type(screen.getByLabelText(/^Password/), 'Napacno123');
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('not correct');
    expect(screen.getByRole('heading', { name: 'Sign in' })).toBeInTheDocument();
  });

  it('signs in with Google, and stays quiet when the window is closed', async () => {
    const auth = createFakeAuth();
    auth.failNextWith({ code: 'auth/popup-closed-by-user' });
    renderApp('/sign-in', { auth });
    await userEvent.click(await screen.findByRole('button', { name: 'Sign in with Google' }));
    await waitFor(() => expect(auth.calls).toContain('signInWithGoogle'));
    expect(screen.queryByRole('alert')).toBeNull();
    await userEvent.click(screen.getByRole('button', { name: 'Sign in with Google' }));
    expect(await heading('Create a group')).toBeInTheDocument();
  });
});

describe('register (R-001-02..04, AC-001-07, EX-001-01..05)', () => {
  async function fill(name: string, email: string, password: string) {
    const nameField = await field('Name');
    if (name) await userEvent.type(nameField, name);
    await userEvent.type(screen.getByLabelText(/^E-mail/), email);
    await userEvent.type(screen.getByLabelText(/^Password/), password);
    await userEvent.click(screen.getByRole('button', { name: 'Register' }));
  }

  it('registers and shows the verify screen', async () => {
    const { auth } = renderApp('/register');
    await fill('Ana Novak', ANA.email, 'Geslo1234');
    expect(await heading('Verify your e-mail')).toBeInTheDocument();
    expect(auth.calls).toContain(`register Ana Novak ${ANA.email} Geslo1234`);
  });

  it('refuses a password of 7 characters with the reason, before Firebase is called', async () => {
    const { auth } = renderApp('/register');
    await fill('Ana Novak', ANA.email, 'Geslo12');
    expect(await screen.findByText('The password needs at least 8 characters.')).toBeInTheDocument();
    expect(auth.calls).toEqual([]);
  });

  it('refuses an empty name and keeps what was typed', async () => {
    const { auth } = renderApp('/register');
    await fill('', ANA.email, 'Geslo1234');
    expect(await screen.findByText('Enter your name.')).toBeInTheDocument();
    expect(screen.getByLabelText(/^E-mail/)).toHaveValue(ANA.email);
    expect(auth.calls).toEqual([]);
  });

  it('shows the refusal of Firebase for an e-mail that is taken', async () => {
    const auth = createFakeAuth();
    auth.failNextWith({ code: 'auth/email-already-in-use' });
    renderApp('/register', { auth });
    await fill('Druga Ana', ANA.email, 'Drugo12345');
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'An account with this e-mail address already exists.',
    );
    expect(screen.getByRole('heading', { name: 'Register' })).toBeInTheDocument();
  });
});

describe('reset password (R-001-06, AC-001-06)', () => {
  it('starts the reset flow and confirms without saying whether the account exists', async () => {
    const { auth } = renderApp('/sign-in');
    await userEvent.click(await screen.findByRole('link', { name: 'Forgot password?' }));
    await userEvent.type(await field('E-mail'), ANA.email);
    await userEvent.click(screen.getByRole('button', { name: 'Send link' }));
    expect(await heading('Check your e-mail')).toBeInTheDocument();
    expect(auth.calls).toContain(`sendPasswordReset ${ANA.email}`);
  });
});

describe('verify your e-mail (R-001-05, AC-001-02, EX-001-06)', () => {
  const unverified = { ...ANA, emailVerified: false };

  it('is the only screen for an unverified person, at every address', async () => {
    renderApp('/groups/42', { auth: createFakeAuth(unverified) });
    expect(await heading('Verify your e-mail')).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Groups' })).toBeNull();
  });

  it('sends the message again and says so', async () => {
    const auth = createFakeAuth(unverified);
    renderApp('/', { auth });
    await userEvent.click(await screen.findByRole('button', { name: 'Send again' }));
    expect(await screen.findByText('We sent the message again.')).toBeInTheDocument();
    expect(auth.calls).toContain('resendVerification');
  });

  it('shows the refusal when Firebase says too many requests', async () => {
    const auth = createFakeAuth(unverified);
    auth.failNextWith({ code: 'auth/too-many-requests' });
    renderApp('/', { auth });
    await userEvent.click(await screen.findByRole('button', { name: 'Send again' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Too many requests');
  });

  it('signs out', async () => {
    renderApp('/', { auth: createFakeAuth(unverified) });
    await userEvent.click(await screen.findByRole('button', { name: 'Sign out' }));
    expect(await heading('Sign in')).toBeInTheDocument();
  });
});

describe('verified person (R-001-08, R-001-09, AC-001-03, AC-001-08)', () => {
  it('sees the empty state with one button when in no group', async () => {
    renderApp('/', { auth: createFakeAuth(ANA) });
    expect(await heading('Create a group')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Create group' })).toBeInTheDocument();
  });

  it('signs out from the user menu and sees the sign-in screen', async () => {
    renderApp('/', { auth: createFakeAuth(ANA) });
    await userEvent.click(await screen.findByRole('button', { name: 'User menu' }));
    await userEvent.click(await screen.findByRole('menuitem', { name: 'Sign out' }));
    expect(await heading('Sign in')).toBeInTheDocument();
  });

  it('is sent from the sign-in screen to the app when already signed in', async () => {
    renderApp('/sign-in', { auth: createFakeAuth(ANA) });
    expect(await heading('Create a group')).toBeInTheDocument();
  });
});
