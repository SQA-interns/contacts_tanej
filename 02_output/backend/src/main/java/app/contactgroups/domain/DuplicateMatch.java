package app.contactgroups.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/** A found set of contact ids with its kind and reasons. */
public record DuplicateMatch(DuplicateKind kind, List<DuplicateReason> reasons, List<UUID> ids) {

  /** The identity of a set: SHA-256 of its sorted contact ids (R-021-09), as 64 hex digits. */
  public static String keyOf(List<UUID> ids) {
    String joined = String.join(",", ids.stream().map(UUID::toString).sorted().toList());
    try {
      byte[] hash =
          MessageDigest.getInstance("SHA-256").digest(joined.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is always available", e);
    }
  }

  public String key() {
    return keyOf(ids);
  }
}
