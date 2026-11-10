export const GROUP_NAME_MAX = 100;

export type GroupNameProblem = 'REQUIRED' | 'TOO_LONG';

/** The rule of R-002-02 for the form: trimmed, 1 to 100 characters. The server checks it again (BR-05). */
export function checkGroupName(raw: string): { name: string; problem: GroupNameProblem | null } {
  const name = raw.trim();
  if (name === '') return { name, problem: 'REQUIRED' };
  if ([...name].length > GROUP_NAME_MAX) return { name, problem: 'TOO_LONG' };
  return { name, problem: null };
}

export function isGroupNameProblem(code: string): code is GroupNameProblem {
  return code === 'REQUIRED' || code === 'TOO_LONG';
}
