package matchingservice;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

import model.AuthHeaders;
import model.UserDTO;
import model.MatcherDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.CrossOrigin;

@RestController
@RequestMapping("/matching")
@Tag(name = "Matching API", description = "Endpoints for matchmaking operations")
public class MatchingController {

    @Autowired
    private MatchingService matchingService;

    @Operation(summary = "Find best partners for given user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully found partners", content = @Content(schema = @Schema(implementation = UserDTO.class)))
    })
    @PostMapping("/partners/{userId}")
    public ResponseEntity<List<UserDTO>> findPartners(
            @Parameter(description = "ID of user requesting partner") @PathVariable String userId,
            @RequestHeader(value = AuthHeaders.USER_ID, required = false) String authenticatedUserId) {
        if (!AuthHeaders.isAuthenticatedUser(authenticatedUserId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!AuthHeaders.isSameUser(authenticatedUserId, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<UserDTO> partners = matchingService.findPartners(userId);
        return partners != null ? ResponseEntity.ok(partners) : ResponseEntity.notFound().build();
    }

    @Operation(summary = "Get previous matches for user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = UserDTO.class)))
    })
    @GetMapping("/history/{userId}")
    public ResponseEntity<List<MatcherDTO>> getMatches(
            @Parameter(description = "ID of user") @PathVariable String userId,
            @RequestHeader(value = AuthHeaders.USER_ID, required = false) String authenticatedUserId) {
        if (!AuthHeaders.isAuthenticatedUser(authenticatedUserId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!AuthHeaders.isSameUser(authenticatedUserId, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(matchingService.getMatches(userId));
    }
}
