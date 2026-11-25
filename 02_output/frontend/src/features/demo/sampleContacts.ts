import type { components } from '@/lib/api/schema';

type ContactFields = components['schemas']['ContactFields'];

/**
 * Demo data for the "Fill with test data" button (post-run, demo deployments only). Fifty contacts,
 * the same on every click, valid under the field rules (BR-05 one of name, e-mail or company;
 * ISO country codes; phone characters; tag characters), with tags and attributes for the filters
 * and three deliberate duplicate pairs for the duplicate search: the same e-mail (certain), the same
 * name and company, and the same name and phone (probable). Only example.com addresses.
 */
const FIRST = [
  'Ana',
  'Marko',
  'Petra',
  'Luka',
  'Maja',
  'Janez',
  'Nina',
  'Tomaž',
  'Eva',
  'Matej',
  'Sara',
  'Gregor',
  'Urška',
  'Rok',
  'Tina',
  'Aleš',
  'Katja',
  'Blaž',
  'Mojca',
  'Žiga',
];
const LAST = [
  'Novak',
  'Horvat',
  'Kovačič',
  'Krajnc',
  'Zupančič',
  'Potočnik',
  'Kovač',
  'Mlakar',
  'Vidmar',
  'Golob',
  'Turk',
  'Božič',
  'Kos',
  'Vidic',
  'Kralj',
  'Zupan',
  'Bizjak',
  'Hribar',
  'Kastelic',
  'Oblak',
];
const COMPANIES = [
  'Acme d.o.o.',
  'Beta Solutions',
  'Alfa IT',
  'Mercator',
  'Petrol',
  'Univerza v Mariboru',
  'Gorenje',
  'Krka',
  'Zavod Šola',
  '',
];
const PLACES: [string, string, string][] = [
  ['Ljubljana', '1000', 'SI'],
  ['Maribor', '2000', 'SI'],
  ['Celje', '3000', 'SI'],
  ['Koper', '6000', 'SI'],
  ['Kranj', '4000', 'SI'],
  ['Novo mesto', '8000', 'SI'],
  ['Wien', '1010', 'AT'],
  ['Zagreb', '10000', 'HR'],
  ['Trieste', '34121', 'IT'],
  ['München', '80331', 'DE'],
];
const JOBS = ['Project manager', 'Developer', 'Sales', 'Marketing', 'Researcher', ''];
const SKILLS = ['spletnoProgramiranje', 'oblikovanje', 'marketing', 'analitika', ''];

/** ASCII local part of an e-mail address: accents and anything but letters and digits removed. */
function plain(text: string): string {
  return text
    .normalize('NFD')
    .replace(/[^\p{L}\p{N}]/gu, '')
    .toLowerCase();
}

/** The entry at a position, wrapping around the list (the lists are never empty). */
function at<T>(list: readonly T[], position: number): T {
  return list[position % list.length] as T;
}

function contact(i: number): ContactFields {
  const firstName = at(FIRST, i);
  // shifted by one in every round of 20, so no two regular contacts share first and last name
  const lastName = at(LAST, i * 7 + Math.floor(i / FIRST.length));
  const company = at(COMPANIES, i * 3);
  const [city, postalCode, country] = at(PLACES, i * 5);
  const tags = [i % 2 === 0 ? 'udelezenec2025' : 'udelezenec2026'];
  if (i % 7 === 0) tags.push('neObvescaj');
  if (i % 9 === 0) tags.push('vip');
  const skill = at(SKILLS, i);
  return {
    firstName,
    lastName,
    email: i % 6 === 5 ? '' : `${plain(firstName)}.${plain(lastName)}${i}@example.com`,
    phone:
      i % 4 === 3
        ? ''
        : `+386 4${i % 10} ${String(100 + i).padStart(3, '0')} ${String(200 + i * 3).slice(-3)}`,
    company,
    jobTitle: at(JOBS, i),
    street: i % 3 === 0 ? '' : `Glavna ulica ${i + 1}`,
    postalCode,
    city,
    country,
    notes: i % 10 === 0 ? 'Testni kontakt za predstavitev.' : '',
    tags,
    attributes: skill ? [{ key: 'kompetenca', value: skill }] : [],
  };
}

/** 45 regular contacts, one more with the e-mail of the second one, and two duplicate pairs: 50 in all. */
export function sampleContacts(): ContactFields[] {
  const regular = Array.from({ length: 45 }, (_, i) => contact(i));
  // the same e-mail as regular[1] under another name: a certain duplicate
  const sameEmail = {
    ...regular[1],
    firstName: 'Marko',
    lastName: 'Horvat',
    notes: 'Dvojnik: isti e-naslov.',
  };
  const sameNameCompany: ContactFields = {
    firstName: 'Irena',
    lastName: 'Lah',
    company: 'Krka',
    city: 'Novo mesto',
    country: 'SI',
    tags: ['udelezenec2025'],
  };
  const sameNamePhone: ContactFields = {
    firstName: 'Simon',
    lastName: 'Pirc',
    phone: '+386 41 555 777',
    city: 'Ljubljana',
    country: 'SI',
    tags: ['udelezenec2026'],
  };
  return [
    ...regular,
    sameEmail,
    sameNameCompany,
    { ...sameNameCompany, email: 'irena.lah@example.com', notes: 'Dvojnik: isto ime in podjetje.' },
    sameNamePhone,
    { ...sameNamePhone, company: 'Petrol', notes: 'Dvojnik: isto ime in telefon.' },
  ];
}
