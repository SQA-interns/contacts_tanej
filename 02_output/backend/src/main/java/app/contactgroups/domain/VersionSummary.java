package app.contactgroups.domain;

import java.time.Instant;

/** A line of the version list of a contact: number, author's name and time (R-019-06). */
public record VersionSummary(int number, String userName, Instant createdAt) {}
