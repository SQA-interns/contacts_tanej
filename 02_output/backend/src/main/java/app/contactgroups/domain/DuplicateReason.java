package app.contactgroups.domain;

/** Why contacts are linked; the order is the order shown (R-021-07). */
public enum DuplicateReason {
  SAME_EMAIL,
  SAME_NAME_AND_COMPANY,
  SAME_NAME_AND_PHONE
}
