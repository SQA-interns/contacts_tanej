package app.contactgroups.domain;

import java.util.UUID;

/** The values of an active contact that duplicates are compared on, already folded (BR-07). */
public record DuplicateCandidate(
    UUID id, String firstName, String lastName, String email, String company, String phone) {}
