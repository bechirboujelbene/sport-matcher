package matchingservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.MatcherDTO;
import model.UserDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MVC layer tests for {@link matchingservice.MatchingController}.
 */
@ExtendWith(SpringExtension.class)
@WebMvcTest(MatchingController.class)
class MatchingControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private MatchingService matchingService;

        private final ObjectMapper mapper = new ObjectMapper();

        @Test
        @DisplayName("POST /matching/partners/{userId} returns list of matched users and triggers service call")
        void partnersEndpointReturnsMatches() throws Exception {
                List<UserDTO> mocked = List.of(
                                new UserDTO("u2", "Carol", "", "", "", java.util.Map.of(), List.of("Hiking")),
                                new UserDTO("u1", "Bob", "", "", "", java.util.Map.of(), List.of("Tennis")));
                Mockito.when(matchingService.findPartners("u0")).thenReturn(mocked);

                mockMvc.perform(post("/matching/partners/u0")
                                .header("X-User-Id", "u0"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$", hasSize(2)))
                                .andExpect(jsonPath("$[0].id", is("u2")))
                                .andExpect(jsonPath("$[1].id", is("u1")));
        }

        @Test
        @DisplayName("POST /matching/partners/{userId} rejects missing identity")
        void partnersEndpointRejectsMissingIdentity() throws Exception {
                mockMvc.perform(post("/matching/partners/u0"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST /matching/partners/{userId} rejects another user's identity")
        void partnersEndpointRejectsMismatchedIdentity() throws Exception {
                mockMvc.perform(post("/matching/partners/u0")
                                .header("X-User-Id", "u1"))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /matching/history/{userId} rejects another user's identity")
        void historyEndpointRejectsMismatchedIdentity() throws Exception {
                mockMvc.perform(get("/matching/history/u0")
                                .header("X-User-Id", "u1"))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /matching/history/{userId} returns stored matches")
        void historyEndpointReturnsStoredMatches() throws Exception {
                List<MatcherDTO> stored = List.of(
                                new MatcherDTO("u2", 0.9, "Great hiking match", List.of("Hiking"), Instant.now()));
                Mockito.when(matchingService.getMatches("u0")).thenReturn(stored);

                mockMvc.perform(get("/matching/history/u0")
                                .header("X-User-Id", "u0"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].matchedUserId", is("u2")))
                                .andExpect(jsonPath("$[0].score", is(0.9)));
        }
}
