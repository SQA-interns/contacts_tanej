package app.contactgroups.domain;

/** A set is certain when one of its links is an e-mail match, else probable (R-021-07). */
public enum DuplicateKind {
  CERTAIN,
  PROBABLE
}
