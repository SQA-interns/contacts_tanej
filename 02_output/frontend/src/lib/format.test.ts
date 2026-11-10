import { countryName, formatDateTime, formatDecimal } from './format';

describe('formatDateTime (R-018-04, R-018-05)', () => {
  it('AC-018-04 shows summer time of Europe/Ljubljana in both languages', () => {
    expect(formatDateTime('2026-07-01T22:30:00Z', 'sl')).toBe('2. 7. 2026 00:30');
    expect(formatDateTime('2026-07-01T22:30:00Z', 'en')).toBe('2026-07-02 00:30');
  });

  it('EX-018-03 shows winter time as 23:30', () => {
    expect(formatDateTime('2026-01-15T22:30:00Z', 'en')).toBe('2026-01-15 23:30');
    expect(formatDateTime('2026-01-15T22:30:00Z', 'sl')).toBe('15. 1. 2026 23:30');
  });

  it('pads hours and minutes and keeps the day unpadded in Slovenian', () => {
    expect(formatDateTime('2026-03-05T03:04:00Z', 'sl')).toBe('5. 3. 2026 04:04');
    expect(formatDateTime('2026-03-05T03:04:00Z', 'en')).toBe('2026-03-05 04:04');
  });

  it('does not depend on the time zone of the machine', () => {
    expect(formatDateTime('2026-12-31T23:30:00Z', 'en')).toBe('2027-01-01 00:30');
  });
});

describe('formatDecimal (R-018-04)', () => {
  it('uses a comma in Slovenian and a point in English', () => {
    expect(formatDecimal(1.5, 'sl')).toBe('1,5');
    expect(formatDecimal(1.5, 'en')).toBe('1.5');
  });
});

describe('countryName (R-018-06)', () => {
  it('AC-018-05 names Slovenia in the active language', () => {
    expect(countryName('SI', 'sl')).toBe('Slovenija');
    expect(countryName('SI', 'en')).toBe('Slovenia');
  });

  it('gives back the code of an unknown country instead of nothing', () => {
    expect(countryName('ZZZZ', 'en')).toBe('ZZZZ');
  });
});
