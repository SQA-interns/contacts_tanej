package app.contactgroups.service;

import app.contactgroups.domain.Contact;
import app.contactgroups.domain.ContactInput;
import app.contactgroups.domain.ExportLayout;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

/**
 * Writes the .xlsx file of an export: one sheet, a bold frozen header, every cell as text (R-011-04
 * to R-011-08). A text that starts with {@code = + - @} is a string with the quote-prefix style, so
 * a spreadsheet never reads it as a formula (SR-08).
 */
final class ExportWorkbook {

  private static final int EMAIL = 2;
  private static final int PHONE = 3;

  private ExportWorkbook() {}

  static byte[] write(List<Contact> contacts, String language) {
    List<String> keys = ExportLayout.attributeKeys(contacts);
    try (SXSSFWorkbook book = new SXSSFWorkbook(100);
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = book.createSheet();
      Styles styles = new Styles(book);
      header(sheet, ExportLayout.headers(language, keys), styles);
      int rowNumber = 1;
      for (Contact contact : contacts) {
        row(sheet.createRow(rowNumber++), cells(contact, keys, language), styles);
      }
      sheet.createFreezePane(0, 1);
      book.write(out);
      book.dispose();
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void header(Sheet sheet, List<String> headers, Styles styles) {
    Row row = sheet.createRow(0);
    for (int i = 0; i < headers.size(); i++) {
      var cell = row.createCell(i);
      cell.setCellValue(headers.get(i));
      cell.setCellStyle(styles.header);
    }
  }

  private static void row(Row row, List<String> values, Styles styles) {
    for (int i = 0; i < values.size(); i++) {
      String value = values.get(i);
      if (value.isEmpty()) {
        continue;
      }
      var cell = row.createCell(i);
      cell.setCellValue(value);
      cell.setCellStyle(styles.of(i, value));
    }
  }

  private static List<String> cells(Contact contact, List<String> keys, String language) {
    ContactInput f = contact.fields();
    List<String> cells =
        new ArrayList<>(
            List.of(
                f.firstName(),
                f.lastName(),
                f.email(),
                f.phone(),
                f.company(),
                f.jobTitle(),
                f.street(),
                f.postalCode(),
                f.city(),
                ExportLayout.countryName(f.country(), language),
                f.notes(),
                ExportLayout.tagsCell(contact.tags())));
    for (String key : keys) {
      cells.add(
          contact.attributes().stream()
              .filter(a -> a.key().toLowerCase(Locale.ROOT).equals(key.toLowerCase(Locale.ROOT)))
              .map(Contact.Attribute::value)
              .findFirst()
              .orElse(""));
    }
    return cells;
  }

  /** The cell styles of one workbook. */
  private static final class Styles {
    private final CellStyle header;
    private final CellStyle plain;
    private final CellStyle text;
    private final CellStyle quoted;

    Styles(SXSSFWorkbook book) {
      Font bold = book.createFont();
      bold.setBold(true);
      header = book.createCellStyle();
      header.setFont(bold);
      plain = book.createCellStyle();
      text = book.createCellStyle();
      text.setDataFormat(book.createDataFormat().getFormat("@"));
      quoted = book.createCellStyle();
      quoted.setQuotePrefixed(true);
    }

    CellStyle of(int column, String value) {
      if (ExportLayout.looksLikeFormula(value)) {
        return quoted;
      }
      return column == EMAIL || column == PHONE ? text : plain;
    }
  }
}
