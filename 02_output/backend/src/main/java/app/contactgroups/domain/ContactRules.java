package app.contactgroups.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The rules of the contact data (BR-05, R-005-02 to R-005-04), one class for create and save. It
 * collects every broken field so that one refusal names them all (EX-005-10).
 */
public final class ContactRules {

  public static final int TEXT_MAX = 255;
  public static final int NOTES_MAX = 5000;
  public static final int PHONE_MAX = 30;
  private static final Pattern PHONE_CHARACTERS = Pattern.compile("[0-9 +().\\-/]*");

  private ContactRules() {}

  /** The contact data in its stored form, or a 400 naming every refused field. */
  public static ContactInput validate(ContactInput raw) {
    List<FieldProblem> problems = new ArrayList<>();
    ContactInput in = trimmed(raw);
    if (in.firstName().isEmpty()
        && in.lastName().isEmpty()
        && in.email().isEmpty()
        && in.company().isEmpty()) {
      problems.add(
          problem(
              "firstName",
              "REQUIRED_ONE_OF",
              "Fill in at least one of first name, last name, e-mail and company."));
    }
    limit(problems, "firstName", in.firstName(), TEXT_MAX);
    limit(problems, "lastName", in.lastName(), TEXT_MAX);
    limit(problems, "company", in.company(), TEXT_MAX);
    limit(problems, "jobTitle", in.jobTitle(), TEXT_MAX);
    limit(problems, "street", in.street(), TEXT_MAX);
    limit(problems, "postalCode", in.postalCode(), TEXT_MAX);
    limit(problems, "city", in.city(), TEXT_MAX);
    limit(problems, "notes", in.notes(), NOTES_MAX);
    ContactInput result = checkEmail(problems, in);
    checkPhone(problems, in.phone());
    if (!in.country().isEmpty() && !CountryCodes.isKnown(in.country())) {
      problems.add(problem("country", "UNKNOWN_COUNTRY", "Choose a country from the list."));
    }
    if (!problems.isEmpty()) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "The request is not valid.", problems);
    }
    return result;
  }

  private static ContactInput trimmed(ContactInput in) {
    return new ContactInput(
        in.firstName().strip(),
        in.lastName().strip(),
        in.email().strip(),
        in.phone().strip(),
        in.company().strip(),
        in.jobTitle().strip(),
        in.street().strip(),
        in.postalCode().strip(),
        in.city().strip(),
        in.country().strip(),
        in.notes().strip());
  }

  private static ContactInput checkEmail(List<FieldProblem> problems, ContactInput in) {
    if (in.email().isEmpty()) {
      return in;
    }
    try {
      String email = EmailAddress.of(in.email());
      return new ContactInput(
          in.firstName(),
          in.lastName(),
          email,
          in.phone(),
          in.company(),
          in.jobTitle(),
          in.street(),
          in.postalCode(),
          in.city(),
          in.country(),
          in.notes());
    } catch (ApiException e) {
      problems.addAll(e.fields());
      return in;
    }
  }

  private static void checkPhone(List<FieldProblem> problems, String phone) {
    if (!PHONE_CHARACTERS.matcher(phone).matches()) {
      problems.add(
          problem("phone", "INVALID_CHARACTERS", "Use digits, spaces and + ( ) - . / only."));
    } else {
      limit(problems, "phone", phone, PHONE_MAX);
    }
  }

  private static void limit(List<FieldProblem> problems, String field, String value, int max) {
    if (value.codePointCount(0, value.length()) > max) {
      problems.add(problem(field, "TOO_LONG", "At most " + max + " characters."));
    }
  }

  private static FieldProblem problem(String field, String code, String message) {
    return new FieldProblem(field, code, message);
  }
}
