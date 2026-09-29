package messagingservice;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import messagingservice.dto.SendMessageRequest;
import model.AuthHeaders;
import messagingservice.entity.MessageEntity;

@RestController
@Tag(name = "Messaging API", description = "API for sending and retrieving messages")
@RequestMapping("/messaging")
public class MessagingController {
    @Autowired
    private MessagingService messagingService;

    @Operation(summary = "Send a message from one user to another")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Message sent successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageEntity.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content)
    })
    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(
            @Parameter(description = "Request payload containing sender, receiver and content")
            @Valid @RequestBody SendMessageRequest req,
            @RequestHeader(value = AuthHeaders.USER_ID, required = false) String authenticatedUserId) {
        if (!AuthHeaders.isAuthenticatedUser(authenticatedUserId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
        }
        if (!AuthHeaders.isSameUser(authenticatedUserId, req.getFromUserId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Cannot send a message as another user");
        }
        return ResponseEntity.ok(messagingService.sendMessage(req.getFromUserId(), req.getToUserId(), req.getContent()));
    }

    @Operation(summary = "Get paginated conversation between two users")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved conversation",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = MessageEntity.class))))
    })
    @GetMapping("/conversation")
    public ResponseEntity<?> getConversation(
            @Parameter(description = "First user ID") @RequestParam String userA,
            @Parameter(description = "Second user ID") @RequestParam String userB,
            @Parameter(description = "Page number (zero-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "50") int size,
            @RequestHeader(value = AuthHeaders.USER_ID, required = false) String authenticatedUserId) {
        if (!AuthHeaders.isAuthenticatedUser(authenticatedUserId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
        }
        if (!authenticatedUserId.equals(userA) && !authenticatedUserId.equals(userB)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Cannot read another user's conversation");
        }
        return ResponseEntity.ok(messagingService.getConversation(userA, userB, page, size));
    }

    @Operation(summary = "Add another user to contact list")
    @PostMapping("/contact")
    public ResponseEntity<?> addContact(
            @RequestParam String userId,
            @RequestParam String contactId,
            @RequestHeader(value = AuthHeaders.USER_ID, required = false) String authenticatedUserId) {
        if (!AuthHeaders.isAuthenticatedUser(authenticatedUserId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
        }
        if (!AuthHeaders.isSameUser(authenticatedUserId, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Cannot modify another user's contacts");
        }
        return ResponseEntity.ok(messagingService.addContact(userId, contactId));
    }

    @Operation(summary = "Get all contacts of a user")
    @GetMapping("/contacts/{userId}")
    public ResponseEntity<?> getContacts(
            @PathVariable String userId,
            @RequestHeader(value = AuthHeaders.USER_ID, required = false) String authenticatedUserId) {
        if (!AuthHeaders.isAuthenticatedUser(authenticatedUserId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
        }
        if (!AuthHeaders.isSameUser(authenticatedUserId, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Cannot read another user's contacts");
        }
        return ResponseEntity.ok(messagingService.getContacts(userId));
    }
}
