-- Baseline schema (ADR-03, ADR-05, AR-06, NFR-13). Forward only; later changes are V2, V3, ...
-- Names follow the glossary (ES-13). All times are UTC (AR-11).

CREATE EXTENSION IF NOT EXISTS unaccent;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Text comparison "ignoring case and accents" (BR-07): used by search, filters and duplicates.
CREATE FUNCTION f_fold(value text) RETURNS text
    LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT
    AS $$ SELECT lower(public.unaccent('public.unaccent'::regdictionary, value)) $$;

CREATE TABLE users (
    id         text PRIMARY KEY,
    name       text        NOT NULL CHECK (char_length(name) BETWEEN 1 AND 100),
    email      text        NOT NULL,
    created_at timestamptz NOT NULL
);

-- Ids of deleted accounts, so that an old token is refused (R-016-06, AC-016-04). No personal data.
CREATE TABLE deleted_accounts (
    user_id    text PRIMARY KEY,
    deleted_at timestamptz NOT NULL
);

CREATE TABLE groups (
    id         uuid PRIMARY KEY,
    name       text        NOT NULL CHECK (char_length(name) BETWEEN 1 AND 100 AND name = btrim(name)),
    state      text        NOT NULL CHECK (state IN ('ACTIVE', 'TRASHED')),
    trashed_at timestamptz,
    created_at timestamptz NOT NULL,
    created_by text        NOT NULL,
    CHECK ((state = 'TRASHED') = (trashed_at IS NOT NULL))
);

CREATE TABLE memberships (
    group_id   uuid        NOT NULL REFERENCES groups (id) ON DELETE CASCADE,
    user_id    text        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role       text        NOT NULL CHECK (role IN ('OWNER', 'EDITOR', 'VIEWER')),
    created_at timestamptz NOT NULL,
    PRIMARY KEY (group_id, user_id)
);
CREATE INDEX memberships_user_idx ON memberships (user_id);

-- Only pending invitations exist as rows: accepted and cancelled ones are deleted, so the invited
-- e-mail is kept only while pending (security-requirements.md, Personal data; D-16).
CREATE TABLE invitations (
    id         uuid PRIMARY KEY,
    group_id   uuid        NOT NULL REFERENCES groups (id) ON DELETE CASCADE,
    email      text        NOT NULL CHECK (email = lower(email) AND char_length(email) <= 255),
    role       text        NOT NULL CHECK (role IN ('OWNER', 'EDITOR', 'VIEWER')),
    created_at timestamptz NOT NULL,
    created_by text        NOT NULL
);
CREATE UNIQUE INDEX invitations_group_email_idx ON invitations (group_id, email);
CREATE INDEX invitations_email_idx ON invitations (email);

CREATE TABLE contacts (
    id              uuid PRIMARY KEY,
    group_id        uuid        NOT NULL REFERENCES groups (id) ON DELETE CASCADE,
    first_name      text CHECK (char_length(first_name) <= 255),
    last_name       text CHECK (char_length(last_name) <= 255),
    email           text CHECK (email = lower(email) AND char_length(email) <= 255),
    phone           text CHECK (char_length(phone) <= 30 AND phone ~ '^[0-9 +().\-/]+$'),
    company         text CHECK (char_length(company) <= 255),
    job_title       text CHECK (char_length(job_title) <= 255),
    street          text CHECK (char_length(street) <= 255),
    postal_code     text CHECK (char_length(postal_code) <= 255),
    city            text CHECK (char_length(city) <= 255),
    country         char(2),
    notes           text CHECK (char_length(notes) <= 5000),
    state           text        NOT NULL CHECK (state IN ('ACTIVE', 'TRASHED')),
    trashed_at      timestamptz,
    created_at      timestamptz NOT NULL,
    created_by      text        NOT NULL,
    -- version number of US-019: grows by 1 per recorded version, never lowered
    current_version integer     NOT NULL DEFAULT 1 CHECK (current_version >= 1),
    -- change counter of the conflict check (AR-14, D-19): also grows when a group-wide tag
    -- rename or delete changes the contact without a version
    revision        bigint      NOT NULL DEFAULT 1 CHECK (revision >= 1),
    -- one text for search and duplicate detection (R-009-03, BR-07): own fields only; tags and
    -- attribute values are matched through their own tables
    search_fold     text GENERATED ALWAYS AS (f_fold(
        coalesce(first_name, '') || ' ' || coalesce(last_name, '') || ' ' || coalesce(email, '')
        || ' ' || coalesce(company, '') || ' ' || coalesce(city, ''))) STORED,
    CHECK ((state = 'TRASHED') = (trashed_at IS NOT NULL)),
    -- BR-05: at least one of first name, last name, e-mail and company is not empty
    CHECK (coalesce(btrim(first_name), '') <> '' OR coalesce(btrim(last_name), '') <> ''
           OR coalesce(btrim(email), '') <> '' OR coalesce(btrim(company), '') <> '')
);
CREATE INDEX contacts_group_state_idx ON contacts (group_id, state);
CREATE INDEX contacts_search_trgm_idx ON contacts USING gin (search_fold gin_trgm_ops);
CREATE INDEX contacts_trashed_idx ON contacts (trashed_at) WHERE state = 'TRASHED';

-- A tag exists only while a contact (also one in Trash) carries it (R-006-07, EX-012-08).
CREATE TABLE tags (
    id       uuid PRIMARY KEY,
    group_id uuid NOT NULL REFERENCES groups (id) ON DELETE CASCADE,
    name     text NOT NULL CHECK (char_length(name) BETWEEN 1 AND 50)  -- characters: service rule R-006-03
);
CREATE UNIQUE INDEX tags_group_name_idx ON tags (group_id, lower(name));

CREATE TABLE contact_tags (
    contact_id uuid NOT NULL REFERENCES contacts (id) ON DELETE CASCADE,
    tag_id     uuid NOT NULL REFERENCES tags (id) ON DELETE CASCADE,
    PRIMARY KEY (contact_id, tag_id)
);
CREATE INDEX contact_tags_tag_idx ON contact_tags (tag_id);

CREATE TABLE attributes (
    id         uuid PRIMARY KEY,
    contact_id uuid NOT NULL REFERENCES contacts (id) ON DELETE CASCADE,
    key        text NOT NULL CHECK (char_length(key) BETWEEN 1 AND 50 AND key = btrim(key)),
    value      text NOT NULL DEFAULT '' CHECK (char_length(value) <= 255)
);
CREATE UNIQUE INDEX attributes_contact_key_idx ON attributes (contact_id, lower(key));
CREATE INDEX attributes_value_trgm_idx ON attributes USING gin (f_fold(value) gin_trgm_ops);

CREATE TABLE contact_versions (
    contact_id uuid        NOT NULL REFERENCES contacts (id) ON DELETE CASCADE,
    number     integer     NOT NULL CHECK (number >= 1),
    snapshot   jsonb       NOT NULL,
    user_id    text,
    user_name  text        NOT NULL,
    created_at timestamptz NOT NULL,
    PRIMARY KEY (contact_id, number)
);

-- History (US-013). Append-only: see the guard triggers below (PP-03, SR-14).
CREATE TABLE history_entries (
    id             uuid PRIMARY KEY,
    group_id       uuid        NOT NULL REFERENCES groups (id) ON DELETE CASCADE,
    contact_id     uuid,           -- no foreign key: the entry outlives an erased contact (BR-06)
    contact_label  text,           -- display name of the contact; erased with it
    change_set_id  uuid        NOT NULL,
    event_type     text        NOT NULL,
    actor_id       text,
    actor_name     text        NOT NULL,
    actor_email    text,           -- removed when the account is deleted (D-16)
    subject_name   text,           -- member named by name only (D-16)
    role           text,
    erased         boolean     NOT NULL DEFAULT false,
    created_at     timestamptz NOT NULL
);
CREATE INDEX history_group_time_idx ON history_entries (group_id, created_at DESC, id);
CREATE INDEX history_contact_idx ON history_entries (contact_id);
CREATE INDEX history_actor_idx ON history_entries (group_id, actor_id);

CREATE TABLE history_changes (
    id         bigserial PRIMARY KEY,
    entry_id   uuid NOT NULL REFERENCES history_entries (id) ON DELETE CASCADE,
    field_name text NOT NULL,
    old_value  text,
    new_value  text
);
CREATE INDEX history_changes_entry_idx ON history_changes (entry_id);

-- Only the anonymisation of an erasure or an account deletion may change an entry (R-013-10).
CREATE FUNCTION history_entries_guard() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF (NEW.id, NEW.group_id, NEW.contact_id, NEW.change_set_id, NEW.event_type, NEW.actor_id,
        NEW.actor_name, NEW.subject_name, NEW.role, NEW.created_at)
       IS DISTINCT FROM
       (OLD.id, OLD.group_id, OLD.contact_id, OLD.change_set_id, OLD.event_type, OLD.actor_id,
        OLD.actor_name, OLD.subject_name, OLD.role, OLD.created_at)
       OR (NEW.actor_email IS NOT NULL AND NEW.actor_email IS DISTINCT FROM OLD.actor_email)
       OR (NEW.contact_label IS NOT NULL AND NEW.contact_label IS DISTINCT FROM OLD.contact_label)
       OR (OLD.erased AND NOT NEW.erased) THEN
        RAISE EXCEPTION 'history entries are append-only';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER history_entries_guard BEFORE UPDATE ON history_entries
    FOR EACH ROW EXECUTE FUNCTION history_entries_guard();

CREATE FUNCTION history_changes_guard() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.id <> OLD.id OR NEW.entry_id <> OLD.entry_id OR NEW.field_name <> OLD.field_name
       OR (NEW.old_value IS NOT NULL AND NEW.old_value IS DISTINCT FROM OLD.old_value)
       OR (NEW.new_value IS NOT NULL AND NEW.new_value IS DISTINCT FROM OLD.new_value) THEN
        RAISE EXCEPTION 'history changes are append-only';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER history_changes_guard BEFORE UPDATE ON history_changes
    FOR EACH ROW EXECUTE FUNCTION history_changes_guard();

-- "Not a duplicate" marks (R-021-09): identified by exactly the contacts in the set.
CREATE TABLE duplicate_dismissals (
    id         uuid PRIMARY KEY,
    group_id   uuid        NOT NULL REFERENCES groups (id) ON DELETE CASCADE,
    set_key    text        NOT NULL,   -- sha-256 of the sorted contact ids
    contact_ids uuid[]     NOT NULL,
    user_id    text        NOT NULL,
    created_at timestamptz NOT NULL,
    UNIQUE (group_id, set_key)
);

-- Link from a merged-away contact to its survivor, so the survivor's History shows theirs (R-022-06).
CREATE TABLE contact_merges (
    merged_id   uuid PRIMARY KEY REFERENCES contacts (id) ON DELETE CASCADE,
    survivor_id uuid        NOT NULL REFERENCES contacts (id) ON DELETE CASCADE,
    merged_at   timestamptz NOT NULL
);
CREATE INDEX contact_merges_survivor_idx ON contact_merges (survivor_id);
