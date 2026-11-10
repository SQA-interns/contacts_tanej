import { errorText, initialLanguage, setupI18n } from './i18n';

describe('initialLanguage (R-018-01, R-018-03, EX-018-01)', () => {
  it('is Slovenian without a stored choice and for an unknown value', () => {
    expect(initialLanguage(null)).toBe('sl');
    expect(initialLanguage('fr')).toBe('sl');
  });

  it('is the stored choice', () => {
    expect(initialLanguage('en')).toBe('en');
    expect(initialLanguage('sl')).toBe('sl');
  });
});

describe('errorText (EX-018-02, EX-018-04)', () => {
  const i18n = setupI18n(null);

  it('shows the text of error.<code> in the active language', async () => {
    await i18n.changeLanguage('en');
    expect(errorText(i18n.t, 'GROUP_NAME_TAKEN')).toBe('You already have a group with this name.');
    await i18n.changeLanguage('sl');
    expect(errorText(i18n.t, 'GROUP_NAME_TAKEN')).not.toBe('You already have a group with this name.');
  });

  it('never shows a key for an unknown code', async () => {
    await i18n.changeLanguage('en');
    const text = errorText(i18n.t, 'SOMETHING_NEW');
    expect(text).toBe(i18n.t('error.INTERNAL_ERROR'));
    expect(text).not.toMatch(/error\./);
  });
});
