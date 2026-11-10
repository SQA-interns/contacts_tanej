package app.contactgroups.domain;

/** A member of a group as listed to every member (contract {@code Member}, R-004-01). */
public record Member(String userId, String name, String email, Role role) {}
