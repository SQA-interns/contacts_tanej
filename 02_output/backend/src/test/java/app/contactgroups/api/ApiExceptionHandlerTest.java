package app.contactgroups.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.contactgroups.domain.ApiException;
import app.contactgroups.domain.ErrorCode;
import app.contactgroups.domain.FieldProblem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** R-017-07, R-017-10, SR-10: every failure becomes the error response, with no internals. */
class ApiExceptionHandlerTest {

  record Body(@NotBlank String name) {}

  @RestController
  static class Probe {
    @GetMapping("/probe/boom")
    String boom() {
      throw new IllegalStateException("secret internal detail SELECT * FROM users");
    }

    @GetMapping("/probe/rule")
    String rule() {
      throw new ApiException(
          ErrorCode.VALIDATION_FAILED,
          "Invalid.",
          List.of(
              new FieldProblem("email", "INVALID_FORMAT", "Not an e-mail address."),
              new FieldProblem("firstName", "TOO_LONG", "Too long.")));
    }

    @GetMapping("/probe/conflict")
    String conflict() {
      throw new ApiException(ErrorCode.CONFLICT, "Changed meanwhile.", List.of());
    }

    @GetMapping("/probe/missing")
    String missing() {
      throw ApiException.notFound();
    }

    @GetMapping("/probe/{id}")
    String byId(@PathVariable UUID id) {
      return id.toString();
    }

    @PostMapping("/probe")
    String create(@Valid @RequestBody Body body) {
      return body.name();
    }
  }

  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();
    mvc =
        MockMvcBuilders.standaloneSetup(new Probe())
            .setControllerAdvice(new ApiExceptionHandler())
            .setValidator(validator)
            .build();
  }

  @Test
  void anUnexpectedFailureIsA500WithAFixedMessageAndNoInternals() throws Exception {
    mvc.perform(get("/probe/boom"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
        .andExpect(jsonPath("$.message").value("Something went wrong."))
        .andExpect(
            content()
                .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret"))));
  }

  @Test
  void anApiExceptionKeepsItsStatusCodeAndEveryFieldError() throws Exception {
    mvc.perform(get("/probe/rule"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.errors.length()").value(2))
        .andExpect(jsonPath("$.errors[0].field").value("email"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_FORMAT"))
        .andExpect(jsonPath("$.errors[1].field").value("firstName"));
    mvc.perform(get("/probe/conflict"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONFLICT"));
  }

  @Test
  void aBeanValidationFailureNamesTheField() throws Exception {
    mvc.perform(post("/probe").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"  \"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.errors[0].field").value("name"));
  }

  @Test
  void aBodyThatIsNotJsonIsA400() throws Exception {
    mvc.perform(post("/probe").contentType(MediaType.APPLICATION_JSON).content("{not json"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  void aMalformedIdInThePathIsTheSame404AsAMissingThing() throws Exception {
    String malformed =
        mvc.perform(get("/probe/not-an-id"))
            .andExpect(status().isNotFound())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String missing =
        mvc.perform(get("/probe/missing"))
            .andExpect(status().isNotFound())
            .andReturn()
            .getResponse()
            .getContentAsString();
    org.assertj.core.api.Assertions.assertThat(malformed).isEqualTo(missing);
    org.assertj.core.api.Assertions.assertThat(missing).contains("\"code\":\"NOT_FOUND\"");
  }

  @Test
  void aPathWithoutAnyHandlerIsTheSame404() throws Exception {
    String none =
        mvc.perform(get("/nowhere/at/all"))
            .andExpect(status().isNotFound())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String missing =
        mvc.perform(get("/probe/missing")).andReturn().getResponse().getContentAsString();
    org.assertj.core.api.Assertions.assertThat(none).isEqualTo(missing);
  }
}
