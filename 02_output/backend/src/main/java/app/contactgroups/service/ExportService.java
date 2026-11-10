package app.contactgroups.service;

import app.contactgroups.domain.ApiException;
import app.contactgroups.domain.ChangeSet;
import app.contactgroups.domain.ChangeSet.Event;
import app.contactgroups.domain.Condition;
import app.contactgroups.domain.ConditionInput;
import app.contactgroups.domain.Conditions;
import app.contactgroups.domain.Contact;
import app.contactgroups.domain.ContactSort;
import app.contactgroups.domain.ErrorCode;
import app.contactgroups.domain.ExportLayout;
import app.contactgroups.domain.FieldChange;
import app.contactgroups.domain.FieldProblem;
import app.contactgroups.domain.HistoryEventType;
import app.contactgroups.domain.Role;
import app.contactgroups.domain.SearchWords;
import app.contactgroups.repository.ContactSearchRepository;
import app.contactgroups.repository.ExportRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Export of the ticked contacts to an .xlsx file, Owner or Editor (US-011). Every export is written
 * in the History with its count in the same transaction (R-013-04).
 */
@Service
public class ExportService {

  /** The project time zone of the file name date (BR-01, R-011-06). */
  private static final ZoneId PROJECT_ZONE = ZoneId.of("Europe/Ljubljana");

  /** What to export: the ticked ids, or the current filter; never both. */
  public record Selection(List<UUID> contactIds, Filter filter) {}

  /** The current filter of the list ("select all N results"). */
  public record Filter(String q, List<ConditionInput> conditions) {}

  /** The finished file and its name. */
  public record ExportFile(String fileName, byte[] bytes) {}

  private final ExportRepository exports;
  private final ContactSearchRepository search;
  private final AccessService access;
  private final HistoryRecorder history;
  private final Clock clock;

  public ExportService(
      ExportRepository exports,
      ContactSearchRepository search,
      AccessService access,
      HistoryRecorder history,
      Clock clock) {
    this.exports = exports;
    this.search = search;
    this.access = access;
    this.history = history;
    this.clock = clock;
  }

  @Transactional
  public ExportFile export(
      UUID groupId, String userId, String language, ContactSort sort, Selection selection) {
    var group = access.requireMember(groupId, userId);
    if (group.role() == Role.VIEWER) {
      throw AccessService.forbidden();
    }
    if (!ExportLayout.isLanguage(language)) {
      throw invalid("language", "INVALID_VALUE", "Use sl or en.");
    }
    List<Contact> contacts = select(groupId, ContactSort.of(sort), selection);
    byte[] bytes = ExportWorkbook.write(contacts, language);
    history.record(
        ChangeSet.of(
            groupId,
            userId,
            Event.group(
                HistoryEventType.EXPORTED,
                List.of(new FieldChange("contacts", null, String.valueOf(contacts.size()))))));
    LocalDate day = clock.instant().atZone(PROJECT_ZONE).toLocalDate();
    return new ExportFile(ExportLayout.fileName(group.name(), day), bytes);
  }

  private List<Contact> select(UUID groupId, ContactSort sort, Selection selection) {
    boolean ticked = selection.contactIds() != null;
    if (ticked == (selection.filter() != null)) {
      throw invalid("contactIds", "REQUIRED", "Tick contacts or give a filter, not both.");
    }
    if (ticked) {
      List<UUID> ids = selection.contactIds().stream().distinct().toList();
      requireWithinLimit(ids.size());
      return exports.findByIds(groupId, ids, sort);
    }
    List<String> patterns = SearchWords.patterns(selection.filter().q());
    List<Condition> conditions = Conditions.parse(selection.filter().conditions());
    requireWithinLimit(search.count(groupId, patterns, conditions));
    return exports.findByFilter(groupId, patterns, conditions, sort);
  }

  private static void requireWithinLimit(int count) {
    if (count > ExportLayout.MAX_CONTACTS) {
      throw invalid(
          "contacts",
          "TOO_MANY_CONTACTS",
          "At most " + ExportLayout.MAX_CONTACTS + " contacts can be exported at once.");
    }
  }

  private static ApiException invalid(String field, String code, String message) {
    return new ApiException(
        ErrorCode.VALIDATION_FAILED,
        "The request is not valid.",
        List.of(new FieldProblem(field, code, message)));
  }
}
