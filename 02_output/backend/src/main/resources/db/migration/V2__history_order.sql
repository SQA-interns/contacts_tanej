-- US-013: "newest first" is the order in which entries were written. The sequence number makes the
-- order exact where two entries carry the same time (one save) and does not depend on the clock.
ALTER TABLE history_entries ADD COLUMN seq bigint GENERATED ALWAYS AS IDENTITY;
CREATE INDEX history_group_seq_idx ON history_entries (group_id, seq DESC);
CREATE INDEX history_contact_seq_idx ON history_entries (contact_id, seq DESC);
