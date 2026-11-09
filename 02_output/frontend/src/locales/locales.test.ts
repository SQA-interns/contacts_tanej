import en from './en.json';
import sl from './sl.json';

function keys(value: unknown, prefix = ''): string[] {
  if (typeof value !== 'object' || value === null) return [prefix];
  return Object.entries(value).flatMap(([key, child]) => keys(child, prefix ? `${prefix}.${key}` : key));
}

describe('language files (NFR-10, EX-018-04)', () => {
  it('have the same keys in Slovenian and English', () => {
    expect(keys(sl).sort()).toEqual(keys(en).sort());
  });

  it('have no empty text', () => {
    const empty = (file: unknown) => Object.entries(flatten(file)).filter(([, text]) => text.trim() === '');
    expect(empty(sl)).toEqual([]);
    expect(empty(en)).toEqual([]);
  });

  it('give every error code of the error model a text', () => {
    expect(Object.keys(sl.error)).toEqual(Object.keys(en.error));
  });
});

function flatten(value: unknown, prefix = ''): Record<string, string> {
  if (typeof value === 'string') return { [prefix]: value };
  if (typeof value !== 'object' || value === null) return {};
  return Object.assign(
    {},
    ...Object.entries(value).map(([key, child]) => flatten(child, prefix ? `${prefix}.${key}` : key)),
  );
}
