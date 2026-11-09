package app.contactgroups;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Architecture rules AR-01, AR-02, AR-05 and AR-11 as tests (architecture.md, Constraints). */
@AnalyzeClasses(
    packages = "app.contactgroups",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

  private static final String API = "app.contactgroups.api..";
  private static final String SERVICE = "app.contactgroups.service..";
  private static final String REPOSITORY = "app.contactgroups.repository..";
  private static final String DOMAIN = "app.contactgroups.domain..";

  /** AR-01: api does not call repository. */
  @ArchTest
  static final ArchRule apiDoesNotCallRepository =
      noClasses()
          .that()
          .resideInAPackage(API)
          .should()
          .dependOnClassesThat()
          .resideInAPackage(REPOSITORY);

  /** AR-01: repository calls neither api nor service. */
  @ArchTest
  static final ArchRule repositoryIsBelowServiceAndApi =
      noClasses()
          .that()
          .resideInAPackage(REPOSITORY)
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(API, SERVICE);

  /** AR-01: service does not call api. */
  @ArchTest
  static final ArchRule serviceDoesNotCallApi =
      noClasses()
          .that()
          .resideInAPackage(SERVICE)
          .should()
          .dependOnClassesThat()
          .resideInAPackage(API);

  /** AR-01: domain depends on no other layer. */
  @ArchTest
  static final ArchRule domainDependsOnNothingElse =
      noClasses()
          .that()
          .resideInAPackage(DOMAIN)
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(API, SERVICE, REPOSITORY, "app.contactgroups.config..");

  /** AR-02: no Hibernate and no JPA. */
  @ArchTest
  static final ArchRule noJpaOrHibernate =
      noClasses()
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("org.hibernate..", "jakarta.persistence..", "javax.persistence..");

  /** AR-05: no server session. */
  @ArchTest
  static final ArchRule noHttpSession =
      noClasses()
          .should()
          .dependOnClassesThat()
          .haveFullyQualifiedName("jakarta.servlet.http.HttpSession");

  /** AR-11: time is read only through the injected clock. */
  @ArchTest
  static final ArchRule noDirectSystemTime =
      noClasses()
          .should()
          .callMethod(java.time.Instant.class, "now")
          .orShould()
          .callMethod(java.time.OffsetDateTime.class, "now")
          .orShould()
          .callMethod(java.time.LocalDateTime.class, "now")
          .orShould()
          .callMethod(java.time.LocalDate.class, "now")
          .orShould()
          .callMethod(System.class, "currentTimeMillis");
}
