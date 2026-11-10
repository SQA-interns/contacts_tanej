import { checkGroupName, isGroupNameProblem } from './groupName';

describe('group name rule of the form (R-002-02, EX-002-01 to EX-002-03)', () => {
  it('requires a name after trimming', () => {
    expect(checkGroupName('').problem).toBe('REQUIRED');
    expect(checkGroupName('   ').problem).toBe('REQUIRED');
  });

  it('trims the name', () => {
    expect(checkGroupName('  Poroka ')).toEqual({ name: 'Poroka', problem: null });
  });

  it('accepts 100 characters and refuses 101', () => {
    expect(checkGroupName('a'.repeat(100)).problem).toBeNull();
    expect(checkGroupName('a'.repeat(101)).problem).toBe('TOO_LONG');
  });

  it('counts characters, not UTF-16 units', () => {
    expect(checkGroupName('😀'.repeat(100)).problem).toBeNull();
  });

  it('keeps accents and symbols', () => {
    expect(checkGroupName('Čebelarji & Co').name).toBe('Čebelarji & Co');
  });

  it('knows the codes the server answers for the name field', () => {
    expect(isGroupNameProblem('REQUIRED')).toBe(true);
    expect(isGroupNameProblem('TOO_LONG')).toBe(true);
    expect(isGroupNameProblem('INVALID_FORMAT')).toBe(false);
  });
});
