package app.contactgroups.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.contactgroups.service.HealthService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** R-017-12: 200 while the system and its database work, 503 otherwise. */
class HealthControllerTest {

  private final HealthService service = mock(HealthService.class);
  private final MockMvc mvc =
      MockMvcBuilders.standaloneSetup(new HealthController(service)).build();

  @Test
  void answers200WithUpWhenTheDatabaseWorks() throws Exception {
    when(service.databaseWorks()).thenReturn(true);
    mvc.perform(get("/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  void answers503WithDownWhenTheDatabaseDoesNot() throws Exception {
    when(service.databaseWorks()).thenReturn(false);
    mvc.perform(get("/health"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.status").value("DOWN"));
  }
}
