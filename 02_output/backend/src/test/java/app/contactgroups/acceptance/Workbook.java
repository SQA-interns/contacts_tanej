package app.contactgroups.acceptance;

import java.io.ByteArrayInputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** The exported .xlsx file read back as a library reads it (environments.md, DoD-P04). */
public final class Workbook implements AutoCloseable {

  private final XSSFWorkbook book;
  private final String fileName;

  public Workbook(Res response) {
    try {
      this.book = new XSSFWorkbook(new ByteArrayInputStream(response.bytes()));
    } catch (Exception e) {
      throw new AssertionError("The answer is not an .xlsx file: " + e, e);
    }
    String disposition = response.headers().firstValue("Content-Disposition").orElse("");
    int at = disposition.indexOf("UTF-8''");
    this.fileName =
        at < 0
            ? disposition
            : URLDecoder.decode(
                disposition.substring(at + "UTF-8''".length()).replace("+", "%2B"),
                StandardCharsets.UTF_8);
  }

  public String fileName() {
    return fileName;
  }

  public int sheets() {
    return book.getNumberOfSheets();
  }

  public Sheet sheet() {
    return book.getSheetAt(0);
  }

  public Cell cell(int row, int col) {
    Row r = sheet().getRow(row);
    return r == null ? null : r.getCell(col);
  }

  /** The text of a cell; empty for a missing or blank cell. */
  public String text(int row, int col) {
    Cell c = cell(row, col);
    if (c == null || c.getCellType() == CellType.BLANK) {
      return "";
    }
    return c.getStringCellValue();
  }

  public List<String> headers() {
    return row(0);
  }

  public List<String> row(int index) {
    List<String> out = new ArrayList<>();
    Row r = sheet().getRow(index);
    if (r == null) {
      return out;
    }
    int width = r.getLastCellNum();
    for (int c = 0; c < width; c++) {
      out.add(text(index, c));
    }
    return out;
  }

  /** Number of data rows below the header. */
  public int dataRows() {
    return sheet().getLastRowNum();
  }

  public int column(String header) {
    return headers().indexOf(header);
  }

  @Override
  public void close() {
    try {
      book.close();
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }
}
