package app.contactgroups.domain;

import java.text.Collator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * What the export file looks like apart from the file format: its name, headers, country names,
 * tags cell and attribute columns (R-011-05, R-011-06, D-21).
 */
public final class ExportLayout {

  /** The most contacts one export may hold (R-011-09). */
  public static final int MAX_CONTACTS = 10_000;

  private static final Pattern FORBIDDEN_IN_FILE_NAME =
      Pattern.compile("[\\\\/:*?\"<>|\\p{Cntrl}]");
  private static final String[] SL_HEADERS = {
    "Ime",
    "Priimek",
    "E-pošta",
    "Telefon",
    "Podjetje",
    "Delovno mesto",
    "Ulica in hišna številka",
    "Poštna številka",
    "Kraj",
    "Država",
    "Opombe",
    "Oznake"
  };
  private static final String[] EN_HEADERS = {
    "First name",
    "Last name",
    "E-mail",
    "Phone",
    "Company",
    "Job title",
    "Street and house number",
    "Postal code",
    "City",
    "Country",
    "Notes",
    "Tags"
  };

  private ExportLayout() {}

  /** True for the two languages of the product. */
  public static boolean isLanguage(String language) {
    return "sl".equals(language) || "en".equals(language);
  }

  /** {@code <group name>_<YYYY-MM-DD>.xlsx} with forbidden characters replaced by "_". */
  public static String fileName(String groupName, LocalDate day) {
    return FORBIDDEN_IN_FILE_NAME.matcher(groupName).replaceAll("_") + "_" + day + ".xlsx";
  }

  /** The 11 field headers and Tags in the language, then the attribute keys. */
  public static List<String> headers(String language, List<String> attributeKeys) {
    List<String> headers =
        new ArrayList<>(List.of("sl".equals(language) ? SL_HEADERS : EN_HEADERS));
    headers.addAll(attributeKeys);
    return headers;
  }

  /** The country name in the language; an unknown code stays as written. */
  public static String countryName(String code, String language) {
    if (code.isEmpty() || !CountryCodes.isKnown(code)) {
      return code;
    }
    Locale locale = Locale.forLanguageTag(language);
    return Locale.of("", code).getDisplayCountry(locale);
  }

  /** The tags of one contact sorted by Slovenian rules and joined by comma and space. */
  public static String tagsCell(List<String> tags) {
    Collator collator = Collator.getInstance(Locale.forLanguageTag("sl-SI"));
    return tags.stream().sorted(collator).reduce((a, b) -> a + ", " + b).orElse("");
  }

  /**
   * One column per attribute key of the contacts: keys equal ignoring case are one column, named as
   * the first contact in export order spells it, in alphabetical order (R-011-05, EX-011-03).
   */
  public static List<String> attributeKeys(List<Contact> contacts) {
    Map<String, String> spelling = new LinkedHashMap<>();
    for (Contact contact : contacts) {
      for (Contact.Attribute a : contact.attributes()) {
        spelling.putIfAbsent(a.key().toLowerCase(Locale.ROOT), a.key());
      }
    }
    return spelling.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(Map.Entry::getValue)
        .toList();
  }

  /** True when a cell text starts with a character that a spreadsheet reads as a formula. */
  public static boolean looksLikeFormula(String text) {
    return !text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0;
  }
}
