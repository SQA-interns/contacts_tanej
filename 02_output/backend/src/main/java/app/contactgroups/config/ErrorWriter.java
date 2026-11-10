package app.contactgroups.config;

import app.contactgroups.api.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;

/** Writes the error response format from code that runs outside the controllers (SR-10). */
final class ErrorWriter {

  private ErrorWriter() {}

  static void write(ObjectMapper mapper, HttpServletResponse response, ErrorResponse body)
      throws IOException {
    response.setStatus(body.status());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    mapper.writeValue(response.getWriter(), body);
  }
}
