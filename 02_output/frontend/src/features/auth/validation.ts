/** Checks of the registration and sign-in forms, done before Firebase is called (specification 4.1). */
export const NAME_MAX_LENGTH = 100;
export const PASSWORD_MIN_LENGTH = 8;

/** The suffix of the message `auth.error.<key>` in the language files. */
export type FieldError = 'nameRequired' | 'nameTooLong' | 'emailInvalid' | 'passwordTooShort';

export interface RegistrationInput {
  name: string;
  email: string;
  password: string;
}

export type RegistrationErrors = Partial<Record<keyof RegistrationInput, FieldError>>;

export function validateEmail(email: string): FieldError | undefined {
  return email.includes('@') ? undefined : 'emailInvalid';
}

export function validateName(name: string): FieldError | undefined {
  if (name.trim().length === 0) return 'nameRequired';
  return name.length > NAME_MAX_LENGTH ? 'nameTooLong' : undefined;
}

export function validatePassword(password: string): FieldError | undefined {
  return password.length < PASSWORD_MIN_LENGTH ? 'passwordTooShort' : undefined;
}

export function validateRegistration(input: RegistrationInput): RegistrationErrors {
  const errors: RegistrationErrors = {
    name: validateName(input.name),
    email: validateEmail(input.email),
    password: validatePassword(input.password),
  };
  return Object.fromEntries(Object.entries(errors).filter(([, error]) => error)) as RegistrationErrors;
}
