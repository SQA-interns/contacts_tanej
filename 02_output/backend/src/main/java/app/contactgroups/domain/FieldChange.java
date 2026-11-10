package app.contactgroups.domain;

/** One changed field of a History entry, old and new value kept in full (R-013-07, EX-013-07). */
public record FieldChange(String field, String oldValue, String newValue) {}
