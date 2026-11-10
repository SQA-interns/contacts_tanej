-- Seed data for the timing and load test (T-N01; NFR-01, NFR-02, NFR-04). Synthetic only: example.com
-- addresses, no real person (SR-18). Run by scripts/perf/load-test.mjs, or by hand:
--   docker compose --env-file ../.env exec -T db psql -q -U contacts -d contacts < scripts/perf/seed-10000.sql
-- Loads 100 users (perf-user-001 .. perf-user-100), one group "Perf 10000" with 10,000 contacts, with
-- tags and attributes, and four groups of 250 contacts ("Perf small 1" .. "Perf small 4"). All 100
-- users are members of every group (user 1 is Owner, the others alternate Editor and Viewer).
-- Safe to run again (fixed ids, ON CONFLICT DO NOTHING). The backend must have started once (Flyway).
\set ON_ERROR_STOP on
BEGIN;

INSERT INTO users (id, name, email, created_at)
SELECT 'perf-user-' || lpad(n::text, 3, '0'), 'Perf User ' || n,
       'perf' || lpad(n::text, 3, '0') || '@example.com', now()
FROM generate_series(1, 100) AS n
ON CONFLICT (id) DO NOTHING;

-- Group 0 is the big one, groups 1 to 4 are small.
INSERT INTO groups (id, name, state, created_at, created_by)
SELECT ('a0000000-0000-4000-8000-00000000000' || g)::uuid,
       CASE WHEN g = 0 THEN 'Perf 10000' ELSE 'Perf small ' || g END,
       'ACTIVE', now(), 'perf-user-001'
FROM generate_series(0, 4) AS g
ON CONFLICT (id) DO NOTHING;

INSERT INTO memberships (group_id, user_id, role, created_at)
SELECT ('a0000000-0000-4000-8000-00000000000' || g)::uuid,
       'perf-user-' || lpad(n::text, 3, '0'),
       CASE WHEN n = 1 THEN 'OWNER' WHEN n % 2 = 0 THEN 'EDITOR' ELSE 'VIEWER' END,
       now()
FROM generate_series(0, 4) AS g, generate_series(1, 100) AS n
ON CONFLICT DO NOTHING;

-- Names with Slovenian letters, so that the accent-folding search does real work (BR-07).
CREATE TEMP TABLE perf_names AS
SELECT * FROM (VALUES
  (0, 'Ana'), (1, 'Marko'), (2, 'Petra'), (3, 'Janez'), (4, 'Maja'), (5, 'Luka'), (6, 'Tina'),
  (7, 'Črt'), (8, 'Žiga'), (9, 'Špela')) AS f (k, name);
CREATE TEMP TABLE perf_surnames AS
SELECT * FROM (VALUES
  (0, 'Novak'), (1, 'Horvat'), (2, 'Kovačič'), (3, 'Krajnc'), (4, 'Zupančič'), (5, 'Potočnik'),
  (6, 'Šuster'), (7, 'Čuk'), (8, 'Škerlj'), (9, 'Mlakar'), (10, 'Kos'), (11, 'Vidmar')) AS s (k, name);
CREATE TEMP TABLE perf_companies AS
SELECT * FROM (VALUES
  (0, 'Acme'), (1, 'Beta'), (2, 'Šola'), (3, 'Alpe'), (4, 'Gama'), (5, 'Delta'), (6, 'Čebelar d.o.o.'),
  (7, 'Zvezda'), (8, 'Luna'), (9, 'Sonce'), (10, 'Reka'), (11, 'Gora'), (12, 'Morje'), (13, 'Polje'),
  (14, 'Most'), (15, 'Vrh'), (16, 'Dolina'), (17, 'Mesto'), (18, 'Vas'), (19, 'Grad')) AS c (k, name);
CREATE TEMP TABLE perf_cities AS
SELECT * FROM (VALUES
  (0, 'Ljubljana'), (1, 'Maribor'), (2, 'Celje'), (3, 'Koper'), (4, 'Kranj'), (5, 'Novo mesto'),
  (6, 'Ptuj'), (7, 'Velenje'), (8, 'Nova Gorica'), (9, 'Škofja Loka')) AS c (k, name);

-- Contacts: 10,000 in group 0, 250 in each of groups 1 to 4. The index i makes every e-mail unique.
INSERT INTO contacts (id, group_id, first_name, last_name, email, phone, company, job_title, city, country,
                      state, trashed_at, created_at, created_by)
SELECT ('f' || lpad(x.g::text, 7, '0') || '-0000-4000-8000-' || lpad(x.i::text, 12, '0'))::uuid,
       ('a0000000-0000-4000-8000-00000000000' || x.g)::uuid,
       f.name, s.name,
       lower('c' || x.g || '.' || x.i || '@example.com'),
       '+386 40 ' || lpad((100000 + x.i)::text, 6, '0'),
       c.name, CASE WHEN x.i % 3 = 0 THEN 'Razvijalec' ELSE NULL END,
       ci.name, CASE WHEN x.i % 11 = 0 THEN 'AT' ELSE 'SI' END,
       'ACTIVE', NULL,
       now() - (x.i % 365 || ' days')::interval,
       'perf-user-' || lpad((1 + x.i % 100)::text, 3, '0')
FROM (SELECT g, generate_series(1, CASE WHEN g = 0 THEN 10000 ELSE 250 END) AS i
      FROM generate_series(0, 4) AS g) AS x
JOIN perf_names f ON f.k = x.i % 10
JOIN perf_surnames s ON s.k = x.i % 12
JOIN perf_companies c ON c.k = x.i % 20
JOIN perf_cities ci ON ci.k = x.i % 10
ON CONFLICT (id) DO NOTHING;

-- Four tags per group; a contact carries one to three of them.
INSERT INTO tags (id, group_id, name)
SELECT ('b' || lpad(g::text, 7, '0') || '-0000-4000-8000-' || lpad(t::text, 12, '0'))::uuid,
       ('a0000000-0000-4000-8000-00000000000' || g)::uuid,
       (ARRAY['udelezenec2025', 'udelezenec2026', 'neObvescaj', 'vip'])[t]
FROM generate_series(0, 4) AS g, generate_series(1, 4) AS t
ON CONFLICT DO NOTHING;

INSERT INTO contact_tags (contact_id, tag_id)
SELECT c.id,
       ('b' || substr(c.id::text, 2, 7) || '-0000-4000-8000-' || lpad(t::text, 12, '0'))::uuid
FROM contacts c
CROSS JOIN LATERAL generate_series(1, 4) AS t
WHERE c.id::text LIKE 'f%' AND (right(c.id::text, 12)::bigint + t) % 3 = 0
ON CONFLICT DO NOTHING;

-- One attribute "kompetenca" on every second contact (values rotate), "leto" on every fifth.
INSERT INTO attributes (id, contact_id, key, value)
SELECT gen_random_uuid(), c.id, 'kompetenca',
       (ARRAY['java', 'spletnoProgramiranje', 'sql', 'python', 'tehnicnoPisanje'])[1 + (n % 5)::int]
FROM (SELECT id, right(id::text, 12)::bigint AS n FROM contacts WHERE id::text LIKE 'f%') c
WHERE c.n % 2 = 0
ON CONFLICT DO NOTHING;

-- Version 1 of every contact (US-019), as the application writes it.
INSERT INTO contact_versions (contact_id, number, snapshot, user_id, user_name, created_at)
SELECT c.id, 1,
       jsonb_strip_nulls(jsonb_build_object(
         'firstName', c.first_name, 'lastName', c.last_name, 'email', c.email, 'phone', c.phone,
         'company', c.company, 'jobTitle', c.job_title, 'city', c.city, 'country', c.country,
         'tags', coalesce((SELECT jsonb_agg(t.name ORDER BY t.name) FROM contact_tags ct
                           JOIN tags t ON t.id = ct.tag_id WHERE ct.contact_id = c.id), '[]'::jsonb),
         'attributes', coalesce((SELECT jsonb_agg(jsonb_build_object('key', a.key, 'value', a.value)
                                                  ORDER BY a.key)
                                 FROM attributes a WHERE a.contact_id = c.id), '[]'::jsonb))),
       c.created_by, u.name, c.created_at
FROM contacts c JOIN users u ON u.id = c.created_by
WHERE c.id::text LIKE 'f%'
ON CONFLICT DO NOTHING;

COMMIT;
ANALYZE contacts;
ANALYZE contact_tags;
ANALYZE attributes;
