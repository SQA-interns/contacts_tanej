package app.contactgroups.domain;

/** A person who signed in: Firebase user id, name and e-mail (lowercase), as the API shows it. */
public record User(String id, String name, String email) {}
