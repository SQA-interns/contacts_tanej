import { validateEmail, validateRegistration } from './validation';

describe('registration form validation (R-001-02, R-001-03)', () => {
  const valid = { name: 'Ana Novak', email: 'ana.novak@example.com', password: 'Geslo1234' };

  it('accepts a valid form', () => {
    expect(validateRegistration(valid)).toEqual({});
  });

  it('accepts a password of exactly 8 characters (EX-001-01)', () => {
    expect(validateRegistration({ ...valid, password: 'Geslo123' })).toEqual({});
  });

  it('refuses a password of 7 characters with the reason (AC-001-07, EX-001-02)', () => {
    expect(validateRegistration({ ...valid, password: 'Geslo12' })).toEqual({
      password: 'passwordTooShort',
    });
  });

  it('refuses an empty name and a name of 101 characters (EX-001-03)', () => {
    expect(validateRegistration({ ...valid, name: '' }).name).toBe('nameRequired');
    expect(validateRegistration({ ...valid, name: '   ' }).name).toBe('nameRequired');
    expect(validateRegistration({ ...valid, name: 'x'.repeat(101) }).name).toBe('nameTooLong');
  });

  it('accepts a name of exactly 100 characters (EX-001-04)', () => {
    expect(validateRegistration({ ...valid, name: 'x'.repeat(100) })).toEqual({});
  });

  it('refuses an e-mail address without an @ sign', () => {
    expect(validateEmail('ana.novak')).toBe('emailInvalid');
    expect(validateEmail('')).toBe('emailInvalid');
    expect(validateEmail('ana.novak@example.com')).toBeUndefined();
  });
});
