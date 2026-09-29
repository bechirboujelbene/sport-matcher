package userservice;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import model.AuthHeaders;
import model.UserDTO;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/user")
@Tag(name = "User API", description = "API for managing users")
public class UserController {

    private final UserService userService;

    private final UserMapper userMapper;

    @Value("${INTERNAL_SERVICE_TOKEN:}")
    private String internalServiceToken;

    @Autowired
    public UserController(UserService userService, UserMapper mapper) {
        this.userService = userService;
        this.userMapper = mapper;
    }

    

    @Operation(summary = "Get a user by their ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found the user", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = User.class)) }),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content) })
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(
            @Parameter(description = "ID of user to be searched") @PathVariable("id") String id,
            @RequestHeader(value = AuthHeaders.USER_ID, required = false) String authenticatedUserId,
            @RequestHeader(value = AuthHeaders.SERVICE_TOKEN, required = false) String serviceToken) {
        boolean serviceRequest = AuthHeaders.isServiceRequest(internalServiceToken, serviceToken);
        if (!serviceRequest && !AuthHeaders.isAuthenticatedUser(authenticatedUserId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!serviceRequest && !AuthHeaders.isSameUser(authenticatedUserId, id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        User user = userService.getUserById(id);
        if (user != null) {
            return ResponseEntity.ok(userMapper.toDTO(user));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @Operation(summary = "Add a new user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created successfully", content = @Content(mediaType = "text/plain")),
            @ApiResponse(responseCode = "400", description = "Invalid user supplied", content = @Content) })
    @PostMapping({ "", "/" })
    public ResponseEntity<String> addUser(
            @RequestBody User user,
            @RequestHeader(value = AuthHeaders.USER_ID, required = false) String authenticatedUserId) {
        if (!AuthHeaders.isAuthenticatedUser(authenticatedUserId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
        }
        if (!AuthHeaders.isSameUser(authenticatedUserId, user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Cannot create another user's profile");
        }
        userService.addUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body("User added successfully");
    }

    @Operation(summary = "Get all users")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved list of users", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = User.class))))
    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers(
            @RequestHeader(value = AuthHeaders.SERVICE_TOKEN, required = false) String serviceToken) {
        if (!AuthHeaders.isServiceRequest(internalServiceToken, serviceToken)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<User> users = userService.getAllUsers();
        List<UserDTO> dtos = users.stream()
                .map(userMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Update a user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User updated successfully", content = @Content(mediaType = "text/plain")),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<String> updateUser(
            @PathVariable("id") String id,
            @RequestBody UpdateUserRequest req,
            @RequestHeader(value = AuthHeaders.USER_ID, required = false) String authenticatedUserId) {
        if (!AuthHeaders.isAuthenticatedUser(authenticatedUserId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
        }
        if (!AuthHeaders.isSameUser(authenticatedUserId, id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Cannot update another user's profile");
        }
        boolean ok = userService.updateUser(id, req.firstName(), req.lastName(), req.bio(), req.skillLevel(), req.availability(), req.sports());
        if (ok)
            return ResponseEntity.ok("User updated");
        else
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
    }

    @Operation(summary = "Delete a user by their ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User deleted successfully", content = @Content(mediaType = "text/plain")),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content) })
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUserById(
            @Parameter(description = "ID of user to be deleted") @PathVariable("id") String id,
            @RequestHeader(value = AuthHeaders.USER_ID, required = false) String authenticatedUserId) {
        if (!AuthHeaders.isAuthenticatedUser(authenticatedUserId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
        }
        if (!AuthHeaders.isSameUser(authenticatedUserId, id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Cannot delete another user's profile");
        }
        boolean deleted = userService.deleteUserById(id);
        if (deleted) {
            return ResponseEntity.ok("User deleted successfully");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
    }

    @GetMapping("/{id}/nearby")
    public ResponseEntity<List<UserDTO>> getNearbyUsers(
            @PathVariable("id") String id,
            @RequestParam double radius,
            @RequestHeader(value = AuthHeaders.USER_ID, required = false) String authenticatedUserId,
            @RequestHeader(value = AuthHeaders.SERVICE_TOKEN, required = false) String serviceToken) {
        if (!AuthHeaders.isServiceRequest(internalServiceToken, serviceToken)
                && !AuthHeaders.isSameUser(authenticatedUserId, id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<User> users = userService.findNearbyUsers(id, radius);
        List<UserDTO> dtos = users.stream()
                .map(userMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    // TODO: Login endpoint
}
