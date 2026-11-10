package app.contactgroups.domain;

import java.util.List;

/** One filter condition; exactly the seven of R-010-01 to R-010-07, nothing else (R-010-09). */
public sealed interface Condition {

  /** Has every named tag, ignoring case. */
  record TagsAll(List<String> tags) implements Condition {}

  /** Has at least one named tag, ignoring case. */
  record TagsAny(List<String> tags) implements Condition {}

  /** Does not have the tag; a contact without tags passes. */
  record TagNot(String tag) implements Condition {}

  /** Has the key (ignoring case) with the value (ignoring case and accents). */
  record AttributeEquals(String key, String value) implements Condition {}

  /** Has the key (ignoring case), whatever its value. */
  record AttributeExists(String key) implements Condition {}

  /** Has the country (ISO code). */
  record Country(String code) implements Condition {}

  /** Has the company, ignoring case and accents. */
  record Company(String name) implements Condition {}
}
