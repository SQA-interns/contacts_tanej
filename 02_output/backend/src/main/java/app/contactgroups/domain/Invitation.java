package app.contactgroups.domain;

import java.time.Instant;
import java.util.UUID;

/** A pending invitation (contract {@code Invitation}); only pending rows exist (R-003-06). */
public record Invitation(UUID id, String email, Role role, Instant createdAt, Instant expiresAt) {}
