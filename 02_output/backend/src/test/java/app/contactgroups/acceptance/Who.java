package app.contactgroups.acceptance;

/** A signed-in person of the Authentication Emulator: Firebase user id, e-mail, name. */
public record Who(String uid, String email, String name, boolean verified) {

  public static final Who ANA = new Who("uid-ana", "ana.novak@example.com", "Ana Novak", true);
  public static final Who MARKO = new Who("uid-marko", "marko@example.com", "Marko Pirc", true);
  public static final Who PETRA = new Who("uid-petra", "petra@example.com", "Petra Kos", true);
  public static final Who BORIS = new Who("uid-boris", "boris@example.com", "Boris Zupan", true);
  public static final Who IVAN = new Who("uid-ivan", "ivan@example.com", "Ivan Horvat", false);

  /** A further verified person, for example {@code Who.person("p7")}. */
  public static Who person(String key) {
    return new Who("uid-" + key, key + "@example.com", "Oseba " + key, true);
  }

  public Who unverified() {
    return new Who(uid, email, name, false);
  }

  public Who withEmail(String newEmail) {
    return new Who(uid, newEmail, name, verified);
  }

  public Who withName(String newName) {
    return new Who(uid, email, newName, verified);
  }
}
