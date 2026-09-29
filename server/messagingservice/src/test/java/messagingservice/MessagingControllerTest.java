package messagingservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import messagingservice.dto.MessageDto;
import messagingservice.dto.ContactDto;
import messagingservice.dto.SendMessageRequest;
import model.UserDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link MessagingController}.
 */
@ExtendWith(SpringExtension.class)
@WebMvcTest(MessagingController.class)
class MessagingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MessagingService messagingService;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("POST /messaging/send returns sent message")
    void sendMessageEndpointReturnsSentMessage() throws Exception {
        // Given
        SendMessageRequest request = new SendMessageRequest();
        request.setFromUserId("user1");
        request.setToUserId("user2");
        request.setContent("Hello World");
        
        MessageDto expectedResponse = new MessageDto(1L, "user1", "user2", "Hello World", LocalDateTime.now());
        
        Mockito.when(messagingService.sendMessage("user1", "user2", "Hello World"))
                .thenReturn(expectedResponse);

        // When & Then
        mockMvc.perform(post("/messaging/send")
                        .header("X-User-Id", "user1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fromUserId", is("user1")))
                .andExpect(jsonPath("$.toUserId", is("user2")))
                .andExpect(jsonPath("$.content", is("Hello World")));
    }

    @Test
    @DisplayName("POST /messaging/send rejects missing identity")
    void sendMessageRejectsMissingIdentity() throws Exception {
        SendMessageRequest request = new SendMessageRequest();
        request.setFromUserId("user1");
        request.setToUserId("user2");
        request.setContent("Hello World");

        mockMvc.perform(post("/messaging/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /messaging/send rejects sender spoofing")
    void sendMessageRejectsSenderSpoofing() throws Exception {
        SendMessageRequest request = new SendMessageRequest();
        request.setFromUserId("user1");
        request.setToUserId("user2");
        request.setContent("Hello World");

        mockMvc.perform(post("/messaging/send")
                        .header("X-User-Id", "attacker")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /messaging/conversation returns paginated conversation")
    void getConversationEndpointReturnsPaginatedMessages() throws Exception {
        // Given
        List<MessageDto> messages = Arrays.asList(
                new MessageDto(1L, "user1", "user2", "Hi", LocalDateTime.now()),
                new MessageDto(2L, "user2", "user1", "Hello", LocalDateTime.now())
        );
        Page<MessageDto> messagePage = new PageImpl<>(messages, PageRequest.of(0, 10), 2);
        
        Mockito.when(messagingService.getConversation("user1", "user2", 0, 10))
                .thenReturn(messagePage);

        // When & Then
        mockMvc.perform(get("/messaging/conversation")
                        .header("X-User-Id", "user1")
                        .param("userA", "user1")
                        .param("userB", "user2")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content[0].content", is("Hi")))
                .andExpect(jsonPath("$.content[1].content", is("Hello")));
    }

    @Test
    @DisplayName("GET /messaging/conversation rejects another user's conversation")
    void getConversationRejectsOtherUsers() throws Exception {
        mockMvc.perform(get("/messaging/conversation")
                        .header("X-User-Id", "attacker")
                        .param("userA", "user1")
                        .param("userB", "user2"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /messaging/contact adds contact and returns contact info")
    void addContactEndpointAddsContactAndReturnsInfo() throws Exception {
        // Given
        ContactDto expectedResponse = new ContactDto("user1", "user2", LocalDateTime.now());
        
        Mockito.when(messagingService.addContact("user1", "user2"))
                .thenReturn(expectedResponse);

        // When & Then
        mockMvc.perform(post("/messaging/contact")
                        .header("X-User-Id", "user1")
                        .param("userId", "user1")
                        .param("contactId", "user2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is("user1")))
                .andExpect(jsonPath("$.contactId", is("user2")));
    }

    @Test
    @DisplayName("GET /messaging/contacts/{userId} returns user contacts")
    void getContactsEndpointReturnsUserContacts() throws Exception {
        // Given
        List<UserDTO> contacts = Arrays.asList(
                new UserDTO("user2", "User Two", "", "", "", java.util.Map.of(), java.util.List.of()),
                new UserDTO("user3", "User Three", "", "", "", java.util.Map.of(), java.util.List.of())
        );
        
        Mockito.when(messagingService.getContacts("user1")).thenReturn(contacts);

        // When & Then
        mockMvc.perform(get("/messaging/contacts/user1")
                        .header("X-User-Id", "user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is("user2")))
                .andExpect(jsonPath("$[1].id", is("user3")));
    }

    @Test
    @DisplayName("GET /messaging/contacts/{userId} rejects another user's contacts")
    void getContactsRejectsMismatchedIdentity() throws Exception {
        mockMvc.perform(get("/messaging/contacts/user1")
                        .header("X-User-Id", "attacker"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /messaging/send with invalid request returns bad request")
    void sendMessageWithInvalidRequestReturnsBadRequest() throws Exception {
        // Given - Invalid request with empty fields
        SendMessageRequest invalidRequest = new SendMessageRequest();
        invalidRequest.setFromUserId("");
        invalidRequest.setToUserId("");
        invalidRequest.setContent("");

        // When & Then
        mockMvc.perform(post("/messaging/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}
