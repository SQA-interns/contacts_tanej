package app.contactgroups.domain;

import java.time.Instant;
import java.util.UUID;

/** One recorded version of a contact: its number, the JSON snapshot and who saved it (ADR-05). */
public record ContactVersion(
    UUID contactId,
    int number,
    String snapshot,
    String userId,
    String userName,
    Instant createdAt) {}
