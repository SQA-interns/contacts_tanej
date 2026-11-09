-- Seed data for the local environment (phase 2, environments.md). No real person: example.com
-- addresses only. Load with scripts/seed.sh, which passes the emulator's user ids as psql variables
-- :ana_uid :marko_uid :petra_uid :boris_uid. Safe to run again (fixed ids, ON CONFLICT DO NOTHING).
-- The backend must have started once so that the schema exists (Flyway, AR-06).
\set ON_ERROR_STOP on
BEGIN;

INSERT INTO users (id, name, email, created_at) VALUES
  (:'ana_uid',   'Ana Novak',   'ana.novak@example.com', now()),
  (:'marko_uid', 'Marko Pirc',  'marko@example.com',     now()),
  (:'petra_uid', 'Petra Kos',   'petra@example.com',     now()),
  (:'boris_uid', 'Boris Zupan', 'boris@example.com',     now())
ON CONFLICT (id) DO NOTHING;

-- Two groups of two different Owners (isolation, E2E-04). Ana owns "Konferenca 2026"; Marko is an
-- Editor and Petra a Viewer there. Boris owns "Poroka".
INSERT INTO groups (id, name, state, created_at, created_by) VALUES
  ('10000000-0000-4000-8000-000000000001', 'Konferenca 2026', 'ACTIVE', now(), :'ana_uid'),
  ('10000000-0000-4000-8000-000000000002', 'Poroka',          'ACTIVE', now(), :'boris_uid')
ON CONFLICT (id) DO NOTHING;

INSERT INTO memberships (group_id, user_id, role, created_at) VALUES
  ('10000000-0000-4000-8000-000000000001', :'ana_uid',   'OWNER',  now()),
  ('10000000-0000-4000-8000-000000000001', :'marko_uid', 'EDITOR', now()),
  ('10000000-0000-4000-8000-000000000001', :'petra_uid', 'VIEWER', now()),
  ('10000000-0000-4000-8000-000000000002', :'boris_uid', 'OWNER',  now())
ON CONFLICT DO NOTHING;

-- One pending invitation (an address with no account yet, EX-003-05)
INSERT INTO invitations (id, group_id, email, role, created_at, created_by) VALUES
  ('20000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001',
   'ivan@example.com', 'VIEWER', now(), :'ana_uid')
ON CONFLICT DO NOTHING;

-- Contacts of "Konferenca 2026": Slovenian letters for sorting and search, a certain duplicate
-- (same e-mail), a probable duplicate (same name and company), a contact with only a phone
-- (never matched), and one in Trash.
INSERT INTO contacts (id, group_id, first_name, last_name, email, phone, company, job_title, city, country,
                      state, trashed_at, created_at, created_by) VALUES
  ('30000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', 'Ana',   'Novak',   'ana.novak@example.com', '+386 40 123 456', 'Acme',  'Vodja',     'Ljubljana', 'SI', 'ACTIVE', NULL, now() - interval '30 days', :'ana_uid'),
  ('30000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000001', 'Ana',   'Škerlj',  'ana.skerlj@example.com', NULL,            'Šola',  NULL,        'Maribor',   'SI', 'ACTIVE', NULL, now() - interval '29 days', :'ana_uid'),
  ('30000000-0000-4000-8000-000000000003', '10000000-0000-4000-8000-000000000001', 'Marko', 'Čuk',     'marko.cuk@example.com',  '041 555 123',   'Acme',  'Razvijalec','Celje',     'SI', 'ACTIVE', NULL, now() - interval '28 days', :'ana_uid'),
  ('30000000-0000-4000-8000-000000000004', '10000000-0000-4000-8000-000000000001', 'Petra',  'Cerar',  'petra.cerar@example.com', NULL,            'Beta',  NULL,        'Koper',     'SI', 'ACTIVE', NULL, now() - interval '27 days', :'ana_uid'),
  ('30000000-0000-4000-8000-000000000005', '10000000-0000-4000-8000-000000000001', 'Janez', 'Sever',   'janez.sever@example.com', NULL,           'Beta',  NULL,        'Kranj',     'SI', 'ACTIVE', NULL, now() - interval '26 days', :'ana_uid'),
  ('30000000-0000-4000-8000-000000000006', '10000000-0000-4000-8000-000000000001', 'Maja',   'Šuster', NULL,                      NULL,            'Šola',  NULL,        'Novo mesto','SI', 'ACTIVE', NULL, now() - interval '25 days', :'ana_uid'),
  ('30000000-0000-4000-8000-000000000007', '10000000-0000-4000-8000-000000000001', 'Luka',   'Zupan',  'luka.zupan@example.com',  NULL,            NULL,    NULL,        'Ljubljana', 'SI', 'ACTIVE', NULL, now() - interval '24 days', :'ana_uid'),
  ('30000000-0000-4000-8000-000000000008', '10000000-0000-4000-8000-000000000001', 'Hans',   'Gruber', 'hans.gruber@example.com', '+43 1 234 567', 'Alpe',  'Direktor',  'Wien',      'AT', 'ACTIVE', NULL, now() - interval '23 days', :'ana_uid'),
  ('30000000-0000-4000-8000-000000000009', '10000000-0000-4000-8000-000000000001', 'Ana',   'Novak',   'ana.novak@example.com', NULL,            'Acme',  NULL,        'Ljubljana', 'SI', 'ACTIVE', NULL, now() - interval '22 days', :'marko_uid'),
  ('30000000-0000-4000-8000-000000000010', '10000000-0000-4000-8000-000000000001', 'Tina',   'Horvat', NULL,                      NULL,            'Beta',  NULL,        'Ptuj',      'SI', 'ACTIVE', NULL, now() - interval '21 days', :'marko_uid'),
  ('30000000-0000-4000-8000-000000000011', '10000000-0000-4000-8000-000000000001', 'Tina',   'Horvat', NULL,                      NULL,            'Beta',  NULL,        'Ptuj',      'SI', 'ACTIVE', NULL, now() - interval '20 days', :'marko_uid'),
  ('30000000-0000-4000-8000-000000000012', '10000000-0000-4000-8000-000000000001', NULL,    NULL,     NULL,                      '+386 41 555 123', 'Telefon d.o.o.', NULL, NULL,    NULL, 'ACTIVE', NULL, now() - interval '19 days', :'marko_uid'),
  ('30000000-0000-4000-8000-000000000013', '10000000-0000-4000-8000-000000000001', 'Stari', 'Kontakt', 'stari@example.com',        NULL,            'Acme',  NULL,        'Ljubljana', 'SI', 'TRASHED', now() - interval '3 days', now() - interval '60 days', :'ana_uid'),
  ('30000000-0000-4000-8000-000000000101', '10000000-0000-4000-8000-000000000002', 'Eva',   'Poročna','eva@example.com',          NULL,            'Poroka d.o.o.', NULL, 'Bled',      'SI', 'ACTIVE', NULL, now() - interval '10 days', :'boris_uid')
ON CONFLICT (id) DO NOTHING;

INSERT INTO tags (id, group_id, name) VALUES
  ('40000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', 'udelezenec2025'),
  ('40000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000001', 'udelezenec2026'),
  ('40000000-0000-4000-8000-000000000003', '10000000-0000-4000-8000-000000000001', 'neObvescaj'),
  ('40000000-0000-4000-8000-000000000004', '10000000-0000-4000-8000-000000000001', 'stari'),
  ('40000000-0000-4000-8000-000000000101', '10000000-0000-4000-8000-000000000002', 'neObvescaj')
ON CONFLICT DO NOTHING;

INSERT INTO contact_tags (contact_id, tag_id) VALUES
  ('30000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000001'),
  ('30000000-0000-4000-8000-000000000001', '40000000-0000-4000-8000-000000000002'),
  ('30000000-0000-4000-8000-000000000002', '40000000-0000-4000-8000-000000000002'),
  ('30000000-0000-4000-8000-000000000003', '40000000-0000-4000-8000-000000000002'),
  ('30000000-0000-4000-8000-000000000003', '40000000-0000-4000-8000-000000000003'),
  ('30000000-0000-4000-8000-000000000013', '40000000-0000-4000-8000-000000000004'),
  ('30000000-0000-4000-8000-000000000101', '40000000-0000-4000-8000-000000000101')
ON CONFLICT DO NOTHING;

INSERT INTO attributes (id, contact_id, key, value) VALUES
  ('50000000-0000-4000-8000-000000000001', '30000000-0000-4000-8000-000000000001', 'kompetenca', 'spletnoProgramiranje'),
  ('50000000-0000-4000-8000-000000000002', '30000000-0000-4000-8000-000000000009', 'kompetenca', 'java'),
  ('50000000-0000-4000-8000-000000000003', '30000000-0000-4000-8000-000000000003', 'leto', '')
ON CONFLICT DO NOTHING;

-- Version 1 of every contact (US-019): created_by is the author
INSERT INTO contact_versions (contact_id, number, snapshot, user_id, user_name, created_at)
SELECT c.id, 1,
       jsonb_strip_nulls(jsonb_build_object(
         'firstName', c.first_name, 'lastName', c.last_name, 'email', c.email, 'phone', c.phone,
         'company', c.company, 'jobTitle', c.job_title, 'city', c.city, 'country', c.country,
         'tags', coalesce((SELECT jsonb_agg(t.name ORDER BY t.name) FROM contact_tags ct
                           JOIN tags t ON t.id = ct.tag_id WHERE ct.contact_id = c.id), '[]'::jsonb),
         'attributes', coalesce((SELECT jsonb_agg(jsonb_build_object('key', a.key, 'value', a.value) ORDER BY a.key)
                                 FROM attributes a WHERE a.contact_id = c.id), '[]'::jsonb))),
       c.created_by, u.name, c.created_at
FROM contacts c JOIN users u ON u.id = c.created_by
ON CONFLICT DO NOTHING;

COMMIT;
