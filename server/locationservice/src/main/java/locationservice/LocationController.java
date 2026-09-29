package locationservice;

import model.AuthHeaders;
import model.LocationDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

@RestController
@RequestMapping("/location")
@Tag(name = "Location API", description = "Endpoints for updating and searching user locations")
public class LocationController {
    
    private final LocationService locationService;
    private final LocationMapper dtoMapper;

    @Value("${INTERNAL_SERVICE_TOKEN:}")
    private String internalServiceToken;

    @Autowired
    public LocationController(LocationService locationService, LocationMapper locationMapper) {
        this.locationService = locationService;
        this.dtoMapper = locationMapper;
    }

    @Operation(
        summary = "Update or create a user's location",
        description = "Updates or creates the current location of a user identified by id and name",
        responses = {
            @ApiResponse(responseCode = "200", description = "Location updated successfully",
                content = @Content(schema = @Schema(implementation = Location.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input parameters")
        }
    )
    @PostMapping("/update")
    public ResponseEntity<LocationDTO> updateLocation(
            @RequestParam String userId,
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestHeader(value = AuthHeaders.USER_ID, required = false) String authenticatedUserId) {
        if (!AuthHeaders.isAuthenticatedUser(authenticatedUserId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!AuthHeaders.isSameUser(authenticatedUserId, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Location updated = locationService.updateLocation(userId, latitude, longitude);
        return ResponseEntity.ok(dtoMapper.toDTO(updated));
    }

    @Operation(
        summary = "Get location by user ID",
        description = "Returns the location of a user by their ID",
        responses = {
            @ApiResponse(responseCode = "200", description = "Location found",
                content = @Content(schema = @Schema(implementation = Location.class))),
            @ApiResponse(responseCode = "404", description = "Location not found")
        }
    )
    @GetMapping("/{id}")
    public ResponseEntity<LocationDTO> getLocation(
            @PathVariable String id,
            @RequestHeader(value = AuthHeaders.USER_ID, required = false) String authenticatedUserId,
            @RequestHeader(value = AuthHeaders.SERVICE_TOKEN, required = false) String serviceToken) {
        if (!AuthHeaders.isServiceRequest(internalServiceToken, serviceToken)
                && !AuthHeaders.isSameUser(authenticatedUserId, id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Location location = locationService.getLocation(id);
        return (location != null)
            ? ResponseEntity.ok(dtoMapper.toDTO(location))
            : ResponseEntity.notFound().build();
    }

    @Operation(
        summary = "Find locations near a given point within a radius",
        description = "Returns a list of locations within the specified radius (in kilometers) of the given coordinates",
        responses = {
            @ApiResponse(responseCode = "200", description = "Search completed successfully",
                content = @Content(schema = @Schema(implementation = Location.class)))
        }
    )
    @GetMapping("/nearby")
    public ResponseEntity<List<String>> searchPartnerByArea(
            @RequestParam String userId,
            @RequestParam double radius,
            @RequestHeader(value = AuthHeaders.SERVICE_TOKEN, required = false) String serviceToken) {
        if (!AuthHeaders.isServiceRequest(internalServiceToken, serviceToken)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(locationService.searchPartnerByArea(userId, radius));
    }

    @Operation(
        summary = "Get all locations",
        description = "Returns all location records in the database",
        responses = {
            @ApiResponse(responseCode = "200", description = "List of locations",
                content = @Content(schema = @Schema(implementation = Location.class)))
        }
    )
    @GetMapping("/all")
    public ResponseEntity<List<LocationDTO>> getAllLocations(
            @RequestHeader(value = AuthHeaders.SERVICE_TOKEN, required = false) String serviceToken) {
        if (!AuthHeaders.isServiceRequest(internalServiceToken, serviceToken)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<Location> locations = locationService.getAll();
        List<LocationDTO> dtos = locations.stream()
                                          .map(dtoMapper::toDTO)
                                          .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}