import { ApiError } from '@/lib/api/client';
import type { components } from '@/lib/api/schema';

export type Contact = components['schemas']['Contact'];

/** The 11 predefined fields in the order of the form (WF-10). */
export const FIELDS = [
  'firstName',
  'lastName',
  'email',
  'company',
  'jobTitle',
  'phone',
  'street',
  'postalCode',
  'city',
  'country',
  'notes',
] as const;

export type Field = (typeof FIELDS)[number];
export type Values = Record<Field, string>;

export const emptyValues: Values = Object.fromEntries(FIELDS.map((f) => [f, ''])) as Values;

export function valuesOf(contact: Contact | undefined): Values {
  if (!contact) return { ...emptyValues };
  return Object.fromEntries(FIELDS.map((f) => [f, contact[f] ?? ''])) as Values;
}

/** The name the screen shows for a contact: its name, else its e-mail, else its company. */
export function contactTitle(values: Values): string {
  const name = `${values.firstName} ${values.lastName}`.trim();
  return name || values.email || values.company;
}

export const maxLength = (field: Field): number => (field === 'notes' ? 5000 : field === 'phone' ? 30 : 255);

/** The refused fields of a 400, as field and code (R-005-03, EX-005-10). */
export function refusedFields(failure: unknown): Partial<Record<string, string>> {
  if (!(failure instanceof ApiError)) return {};
  return Object.fromEntries((failure.body.errors ?? []).map((e) => [e.field, e.code]));
}
