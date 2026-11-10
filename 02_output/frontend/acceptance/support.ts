/**
 * Harness of the browser acceptance tests and the end-to-end tests (phase 3).
 *
 * They run against the real local stack of environments.md: the web app (PLAYWRIGHT_BASE_URL, default
 * http://localhost:5173), the backend (API_URL, default http://localhost:8080), PostgreSQL and the
 * Firebase Authentication Emulator (EMULATOR_URL, default http://localhost:9099), started with
 * `docker compose --env-file ../.env up --build -d` from 02_output. Before every test the emulator
 * accounts and every table of the database are emptied, so each test starts on an empty system.
 * People are created in the emulator with fixed e-mail addresses and the password of the seed data.
 */
import { expect, test as base, type Locator, type Page } from '@playwright/test';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { inflateRawSync } from 'node:zlib';

export const STACK_URL = process.env.PLAYWRIGHT_BASE_URL ?? 'http://localhost:5173';
export const API_URL = process.env.API_URL ?? 'http://localhost:8080';
export const EMULATOR_URL = process.env.EMULATOR_URL ?? 'http://localhost:9099';
const PROJECT = 'demo-contacts';
const API_KEY = 'fake-api-key';
const IDENTITY = `${EMULATOR_URL}/identitytoolkit.googleapis.com/v1`;

export const PASSWORD = 'Geslo1234';

export interface Person {
  email: string;
  name: string;
  password: string;
}

export const ANA: Person = { email: 'ana.novak@example.com', name: 'Ana Novak', password: PASSWORD };
export const MARKO: Person = { email: 'marko@example.com', name: 'Marko Pirc', password: PASSWORD };
export const PETRA: Person = { email: 'petra@example.com', name: 'Petra Kos', password: PASSWORD };
export const BORIS: Person = { email: 'boris@example.com', name: 'Boris Zupan', password: PASSWORD };
export const IVAN: Person = { email: 'ivan@example.com', name: 'Ivan Horvat', password: PASSWORD };

// ------------------------------------------------------------------ resetting the stack

const EMPTY_ALL_TABLES =
  "DO $$ DECLARE r record; BEGIN FOR r IN SELECT tablename FROM pg_tables WHERE schemaname = 'public' " +
  "AND tablename <> 'flyway_schema_history' LOOP EXECUTE 'TRUNCATE TABLE ' || quote_ident(r.tablename) || ' CASCADE'; " +
  'END LOOP; END $$';

export async function resetStack(): Promise<void> {
  const response = await fetch(`${EMULATOR_URL}/emulator/v1/projects/${PROJECT}/accounts`, {
    method: 'DELETE',
  });
  if (!response.ok) throw new Error(`The emulator accounts were not cleared: ${response.status}`);
  execFileSync(
    'docker',
    [
      'compose',
      '-f',
      fileURLToPath(new URL('../../docker-compose.yml', import.meta.url)),
      '--env-file',
      fileURLToPath(new URL('../../../.env', import.meta.url)),
      'exec',
      '-T',
      'db',
      'psql',
      '-q',
      '-U',
      'contacts',
      '-d',
      'contacts',
      '-c',
      EMPTY_ALL_TABLES,
    ],
    { stdio: 'pipe' },
  );
}

// ------------------------------------------------------------------ emulator

async function identity(path: string, body: object, headers: Record<string, string> = {}) {
  const response = await fetch(`${IDENTITY}/${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...headers },
    body: JSON.stringify(body),
  });
  return { ok: response.ok, body: (await response.json()) as Record<string, string> };
}

/** Creates the account in the emulator; with `verified` the e-mail is marked verified (BR-03). */
export async function createAccount(person: Person, options: { verified?: boolean } = {}): Promise<string> {
  const { verified = true } = options;
  const created = await identity(`accounts:signUp?key=${API_KEY}`, {
    email: person.email,
    password: person.password,
    displayName: person.name,
    returnSecureToken: true,
  });
  const uid = created.body.localId;
  if (!uid) throw new Error(`No account for ${person.email}: ${JSON.stringify(created.body)}`);
  if (verified) {
    await identity(
      `projects/${PROJECT}/accounts:update`,
      { localId: uid, emailVerified: true, displayName: person.name },
      { Authorization: 'Bearer owner' },
    );
  }
  return uid;
}

/** The codes the emulator "sent" to the address (it sends no real e-mail), newest last. */
export async function emailCodes(email: string, requestType: 'VERIFY_EMAIL' | 'PASSWORD_RESET') {
  const response = await fetch(`${EMULATOR_URL}/emulator/v1/projects/${PROJECT}/oobCodes`);
  const body = (await response.json()) as {
    oobCodes: { email: string; requestType: string; oobCode: string }[];
  };
  return body.oobCodes.filter((c) => c.email === email && c.requestType === requestType);
}

/** Follows the verification link of the last verification e-mail. */
export async function verifyEmail(email: string): Promise<void> {
  const codes = await emailCodes(email, 'VERIFY_EMAIL');
  const last = codes[codes.length - 1];
  if (!last) throw new Error(`No verification e-mail for ${email}`);
  await identity(`accounts:update?key=${API_KEY}`, { oobCode: last.oobCode });
}

async function idToken(person: Person): Promise<string> {
  const signedIn = await identity(`accounts:signInWithPassword?key=${API_KEY}`, {
    email: person.email,
    password: person.password,
    returnSecureToken: true,
  });
  const token = signedIn.body.idToken;
  if (!token) throw new Error(`No token for ${person.email}: ${JSON.stringify(signedIn.body)}`);
  return token;
}

// ------------------------------------------------------------------ the API, for setting the scene

export interface Answer {
  status: number;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  body: any;
}

/** A person's API client: creates the account (verified) when it does not exist yet. */
export class Client {
  private constructor(
    readonly person: Person,
    private readonly token: string,
  ) {}

  static async of(person: Person, options: { verified?: boolean } = {}): Promise<Client> {
    try {
      return new Client(person, await idToken(person));
    } catch {
      await createAccount(person, options);
      return new Client(person, await idToken(person));
    }
  }

  async call(method: string, path: string, body?: object): Promise<Answer> {
    const response = await fetch(`${API_URL}${path}`, {
      method,
      headers: { Authorization: `Bearer ${this.token}`, 'Content-Type': 'application/json' },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
    const text = await response.text();
    return { status: response.status, body: text ? JSON.parse(text) : undefined };
  }

  async ok(method: string, path: string, body?: object) {
    const answer = await this.call(method, path, body);
    if (answer.status >= 300)
      throw new Error(`${method} ${path} gave ${answer.status}: ${JSON.stringify(answer.body)}`);
    return answer.body;
  }

  async group(name: string): Promise<string> {
    return (await this.ok('POST', '/groups', { name })).id;
  }

  async contact(groupId: string, fields: object): Promise<{ id: string; revision: number }> {
    return this.ok('POST', `/groups/${groupId}/contacts`, fields);
  }

  /** The Owner invites the person; the person's first request accepts the invitation. */
  async invite(groupId: string, person: Person, role: 'OWNER' | 'EDITOR' | 'VIEWER'): Promise<void> {
    await this.ok('POST', `/groups/${groupId}/invitations`, { email: person.email, role });
    const theirs = await Client.of(person);
    await theirs.ok('GET', '/groups');
  }
}

// ------------------------------------------------------------------ the browser

/* Playwright fixtures call `use`, which the React hooks rule mistakes for a hook. */
/* eslint-disable react-hooks/rules-of-hooks */

/** The wireframes are drawn in English; the application opens in Slovenian (R-018-01). */
export const test = base.extend<{ english: boolean; stack: void }>({
  english: [true, { option: true }],
  // eslint-disable-next-line no-empty-pattern
  baseURL: async ({}, use) => {
    await use(STACK_URL);
  },
  stack: [
    // eslint-disable-next-line no-empty-pattern
    async ({}, use) => {
      await resetStack();
      await use();
    },
    { auto: true },
  ],
  page: async ({ page, english }, use) => {
    if (english) {
      await page.addInitScript(() => localStorage.setItem('language', 'en'));
    }
    await use(page);
  },
});

export { expect };

export async function fillForm(page: Page, fields: Record<string, string>): Promise<void> {
  for (const [label, value] of Object.entries(fields)) {
    await page.getByLabel(label).first().fill(value);
  }
}

export async function signIn(page: Page, person: Person): Promise<void> {
  await page.goto('/');
  await page.getByLabel('E-mail').fill(person.email);
  await page.getByLabel('Password').fill(person.password);
  await page.getByRole('button', { name: 'Sign in', exact: true }).click();
  // Wait for the outcome, so that a following goto does not cut the sign-in off (D-51):
  // signed in, e-mail not verified, or an error alert.
  await page
    .getByRole('button', { name: 'User menu' })
    .or(page.getByRole('heading', { name: 'Verify your e-mail' }))
    .or(page.getByRole('alert'))
    .first()
    .waitFor();
}

export async function signOut(page: Page): Promise<void> {
  await page.getByRole('button', { name: 'User menu' }).click();
  await page.getByText('Sign out', { exact: true }).click();
}

/** Signs the person in and opens the group from the groups list. */
export async function openGroup(page: Page, person: Person, groupName: string): Promise<void> {
  await signIn(page, person);
  await page.getByRole('link', { name: groupName }).click();
}

export function tab(page: Page, name: string): Locator {
  return page.getByRole('navigation', { name: 'Group' }).getByRole('link', { name });
}

export async function openTab(page: Page, name: string): Promise<void> {
  await tab(page, name).click();
}

export function dialog(page: Page): Locator {
  return page.getByRole('dialog');
}

/** The row of the contacts list or another table that holds the text. */
export function row(page: Page, text: string | RegExp): Locator {
  return page.getByRole('row').filter({ hasText: text });
}

/** An owner with a group and the people of the story, as members with their roles. */
export async function scene(
  groupName: string,
  members: { person: Person; role: 'OWNER' | 'EDITOR' | 'VIEWER' }[] = [],
): Promise<{ owner: Client; groupId: string; clients: Record<string, Client> }> {
  const owner = await Client.of(ANA);
  const groupId = await owner.group(groupName);
  const clients: Record<string, Client> = { [ANA.email]: owner };
  for (const { person, role } of members) {
    await owner.invite(groupId, person, role);
    clients[person.email] = await Client.of(person);
  }
  return { owner, groupId, clients };
}

// ------------------------------------------------------------------ reading the exported file

/** Reads the first sheet of an .xlsx file into rows of text (the file is a zip of XML parts). */
export function readXlsx(bytes: Buffer): string[][] {
  const parts = new Map<string, Buffer>();
  const end = bytes.lastIndexOf(Buffer.from([0x50, 0x4b, 0x05, 0x06]));
  const entries = bytes.readUInt16LE(end + 10);
  let at = bytes.readUInt32LE(end + 16);
  for (let i = 0; i < entries; i++) {
    const method = bytes.readUInt16LE(at + 10);
    const compressed = bytes.readUInt32LE(at + 20);
    const nameLength = bytes.readUInt16LE(at + 28);
    const extraLength = bytes.readUInt16LE(at + 30);
    const commentLength = bytes.readUInt16LE(at + 32);
    const local = bytes.readUInt32LE(at + 42);
    const name = bytes.subarray(at + 46, at + 46 + nameLength).toString('utf8');
    const dataAt = local + 30 + bytes.readUInt16LE(local + 26) + bytes.readUInt16LE(local + 28);
    const raw = bytes.subarray(dataAt, dataAt + compressed);
    parts.set(name, method === 0 ? raw : inflateRawSync(raw));
    at += 46 + nameLength + extraLength + commentLength;
  }
  const unescape = (s: string) =>
    s
      .replace(/&lt;/g, '<')
      .replace(/&gt;/g, '>')
      .replace(/&quot;/g, '"')
      .replace(/&apos;/g, "'")
      .replace(/&amp;/g, '&');
  const joinText = (xml: string) =>
    unescape([...xml.matchAll(/<t[^>]*>([\s\S]*?)<\/t>/g)].map((m) => m[1]).join(''));
  const strings: string[] = [];
  const shared = parts.get('xl/sharedStrings.xml')?.toString('utf8') ?? '';
  for (const si of shared.matchAll(/<si>([\s\S]*?)<\/si>/g)) strings.push(joinText(si[1] ?? ''));
  const sheet = parts.get('xl/worksheets/sheet1.xml')?.toString('utf8') ?? '';
  const rows: string[][] = [];
  for (const r of sheet.matchAll(/<row [^>]*>([\s\S]*?)<\/row>/g)) {
    const cells: string[] = [];
    for (const c of (r[1] ?? '').matchAll(/<c ([^>]*?)(?:\/>|>([\s\S]*?)<\/c>)/g)) {
      const attrs = c[1] ?? '';
      const column = /r="([A-Z]+)\d+"/.exec(attrs)?.[1] ?? 'A';
      const index = [...column].reduce((n, ch) => n * 26 + ch.charCodeAt(0) - 64, 0) - 1;
      const body = c[2] ?? '';
      if (/t="s"/.test(attrs)) cells[index] = strings[Number(/<v>(\d+)<\/v>/.exec(body)?.[1])] ?? '';
      else if (/t="inlineStr"/.test(attrs)) cells[index] = joinText(body);
      else cells[index] = unescape(/<v>([\s\S]*?)<\/v>/.exec(body)?.[1] ?? '');
    }
    rows.push(Array.from(cells, (v) => v ?? ''));
  }
  return rows;
}
