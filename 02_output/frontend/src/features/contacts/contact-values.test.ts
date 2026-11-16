import { ApiError } from '@/lib/api/client';
import { contactTitle, emptyValues, FIELDS, maxLength, refusedFields, valuesOf } from './contactValues';
import { conditionText } from './conditions';
import { formatDateTime } from '@/lib/format';
import type { TFunction } from 'i18next';

describe('contact values', () => {
  it('limits notes to 5000, the phone to 30 and every other field to 255 (BR-05)', () => {
    expect(maxLength('notes')).toBe(5000);
    expect(maxLength('phone')).toBe(30);
    for (const field of FIELDS.filter((f) => f !== 'notes' && f !== 'phone')) {
      expect(maxLength(field)).toBe(255);
    }
  });

  it('returns empty values for no contact and a copy each time', () => {
    const a = valuesOf(undefined);
    expect(a).toEqual(emptyValues);
    expect(a).not.toBe(emptyValues);
    expect(Object.keys(a)).toEqual([...FIELDS]);
  });

  it('shows missing fields of a contact as empty strings', () => {
    const values = valuesOf({ id: 'c1', firstName: 'Ana' } as never);
    expect(values.firstName).toBe('Ana');
    expect(values.email).toBe('');
  });

  it('titles a contact by its name, else its e-mail, else its company', () => {
    const base = { ...emptyValues };
    expect(contactTitle({ ...base, firstName: 'Ana', lastName: 'Novak', email: 'a@x.si' })).toBe('Ana Novak');
    expect(contactTitle({ ...base, lastName: 'Novak' })).toBe('Novak');
    expect(contactTitle({ ...base, firstName: 'Ana' })).toBe('Ana');
    expect(contactTitle({ ...base, email: 'a@x.si', company: 'Acme' })).toBe('a@x.si');
    expect(contactTitle({ ...base, company: 'Acme' })).toBe('Acme');
  });

  it('maps the refused fields of a 400 to their codes and ignores other failures', () => {
    const failure = new ApiError(400, {
      status: 400,
      code: 'VALIDATION_FAILED',
      message: 'bad',
      errors: [
        { field: 'email', code: 'INVALID_EMAIL' },
        { field: 'phone', code: 'TOO_LONG' },
      ],
    } as never);
    expect(refusedFields(failure)).toEqual({ email: 'INVALID_EMAIL', phone: 'TOO_LONG' });
    expect(
      refusedFields(new ApiError(400, { status: 400, code: 'VALIDATION_FAILED', message: 'x' } as never)),
    ).toEqual({});
    expect(refusedFields(new Error('x'))).toEqual({});
  });
});

describe('filter chip texts', () => {
  const t = ((key: string, params?: Record<string, string>) =>
    `${key}|${JSON.stringify(params ?? {})}`) as unknown as TFunction;

  it('names every condition type with its values (R-010-11)', () => {
    expect(conditionText({ type: 'TAGS_ALL', tags: ['a', 'b'] }, t, 'en')).toBe(
      'list.filter.chip.all|{"tags":"a, b"}',
    );
    expect(conditionText({ type: 'TAGS_ANY', tags: ['a', 'b'] }, t, 'en')).toBe(
      'list.filter.chip.any|{"tags":"a, b"}',
    );
    expect(conditionText({ type: 'TAG_NOT', tag: 'a' }, t, 'en')).toBe('list.filter.chip.not|{"tag":"a"}');
    expect(conditionText({ type: 'ATTRIBUTE_EQUALS', key: 'k', value: 'v' }, t, 'en')).toBe(
      'list.filter.chip.equals|{"key":"k","value":"v"}',
    );
    expect(conditionText({ type: 'ATTRIBUTE_EXISTS', key: 'k' }, t, 'en')).toBe(
      'list.filter.chip.exists|{"key":"k"}',
    );
    expect(conditionText({ type: 'COUNTRY', country: 'SI' }, t, 'en')).toBe(
      'list.filter.chip.country|{"country":"Slovenia"}',
    );
    expect(conditionText({ type: 'COMPANY', company: 'Acme' }, t, 'en')).toBe(
      'list.filter.chip.company|{"company":"Acme"}',
    );
  });
});

describe('date and time in the project time zone', () => {
  it('formats a summer and a winter instant in Ljubljana time (R-018-04, R-018-05)', () => {
    expect(formatDateTime('2026-07-01T10:05:00Z', 'en')).toBe('2026-07-01 12:05');
    expect(formatDateTime('2026-01-05T10:05:00Z', 'en')).toBe('2026-01-05 11:05');
    expect(formatDateTime('2026-01-05T10:05:00Z', 'sl')).toBe('5. 1. 2026 11:05');
  });
});
