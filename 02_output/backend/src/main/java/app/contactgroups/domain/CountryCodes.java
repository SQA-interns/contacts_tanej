package app.contactgroups.domain;

import java.util.Locale;
import java.util.Set;

/** The country list of the contact form: the ISO 3166-1 alpha-2 codes (OQ-005-02). */
public final class CountryCodes {

  private static final Set<String> CODES = Set.of(Locale.getISOCountries());

  private CountryCodes() {}

  /** True for an upper-case ISO 3166-1 alpha-2 code. */
  public static boolean isKnown(String code) {
    return CODES.contains(code);
  }
}
