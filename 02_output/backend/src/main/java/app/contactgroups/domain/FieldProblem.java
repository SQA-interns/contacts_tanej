package app.contactgroups.domain;

/**
 * One refused field of a request (R-017-07, EX-005-10): the API's field name (for example {@code
 * email} or {@code tags[2]}), the code of the broken rule and a short message.
 */
public record FieldProblem(String field, String code, String message) {}
