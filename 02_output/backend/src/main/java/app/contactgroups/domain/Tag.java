package app.contactgroups.domain;

import java.util.UUID;

/** A tag of a group with the number of contacts that carry it (contract {@code Tag}). */
public record Tag(UUID id, String name, int contactCount) {}
