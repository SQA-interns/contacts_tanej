package app.contactgroups.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

/** One version with its snapshot, shown read-only (contract {@code VersionDetail}, R-020-02). */
public record VersionDetail(int number, String userName, Instant createdAt, JsonNode snapshot) {}
