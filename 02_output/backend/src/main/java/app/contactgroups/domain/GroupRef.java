package app.contactgroups.domain;

import java.util.UUID;

/** A group named in a refusal, for example the groups that block an account deletion (R-016-02). */
public record GroupRef(UUID id, String name) {}
