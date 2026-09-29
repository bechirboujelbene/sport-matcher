package locationservice;

import model.LocationDTO;
import model.UserDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = LocationController.class, properties = "INTERNAL_SERVICE_TOKEN=test-service-token")
class LocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LocationService locationService;

    @MockBean
    private LocationMapper locationMapper;

    @Test
    void testUpdateLocation() throws Exception {
        String userId = "user1";
        double latitude = 48.13;
        double longitude = 11.57;
        Location location = new Location(userId, latitude, longitude );
        
        when(locationService.updateLocation(userId, latitude, longitude)).thenReturn(location);
        
        when(locationMapper.toDTO(location)).thenReturn(new LocationDTO(userId, latitude, longitude));

        mockMvc.perform(post("/location/update")
                        .header("X-User-Id", userId)
                        .param("userId", userId) 
                        .param("latitude", String.valueOf(latitude))
                        .param("longitude", String.valueOf(longitude)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.latitude").value(latitude))
                .andExpect(jsonPath("$.longitude").value(longitude));
    }

    @Test
    void testUpdateLocationRejectsMismatchedIdentity() throws Exception {
        mockMvc.perform(post("/location/update")
                        .header("X-User-Id", "attacker")
                        .param("userId", "user1")
                        .param("latitude", "48.13")
                        .param("longitude", "11.57"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUpdateLocationRejectsMissingIdentity() throws Exception {
        mockMvc.perform(post("/location/update")
                        .param("userId", "user1")
                        .param("latitude", "48.13")
                        .param("longitude", "11.57"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetAllLocationsRejectsInvalidServiceToken() throws Exception {
        mockMvc.perform(get("/location/all")
                        .header("X-Service-Token", "wrong-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetAllLocations() throws Exception {
        List<Location> list = List.of(new Location("u1", 0.0, 0.0),
                                           new Location("u2", 1.0, 1.0));
        when(locationService.getAll()).thenReturn(list);
        when(locationMapper.toDTO(list.get(0))).thenReturn(new LocationDTO("u1", 0.0, 0.0));
        when(locationMapper.toDTO(list.get(1))).thenReturn(new LocationDTO("u2", 1.0, 1.0));

        mockMvc.perform(get("/location/all")
                        .header("X-Service-Token", "test-service-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value("u1"))
                .andExpect(jsonPath("$[1].userId").value("u2"));
    }

    @Test
    void testGetLocationById() throws Exception {
        Location alice = new Location("user1", 48.13, 11.57);
        when(locationService.getLocation("user1")).thenReturn(alice);

        when(locationMapper.toDTO(alice)).thenReturn(new LocationDTO("user1", 48.13, 11.57));

        mockMvc.perform(get("/location/user1")
                        .header("X-User-Id", "user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user1"));
    }

    @Test
    void testGetLocationByIdRejectsMismatchedIdentity() throws Exception {
        mockMvc.perform(get("/location/user1")
                        .header("X-User-Id", "attacker"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetLocationByIdAllowsServiceToken() throws Exception {
        Location alice = new Location("user1", 48.13, 11.57);
        when(locationService.getLocation("user1")).thenReturn(alice);
        when(locationMapper.toDTO(alice)).thenReturn(new LocationDTO("user1", 48.13, 11.57));

        mockMvc.perform(get("/location/user1")
                        .header("X-Service-Token", "test-service-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user1"));
    }

    @Test
    void testNearbyRejectsInvalidServiceToken() throws Exception {
        mockMvc.perform(get("/location/nearby")
                        .header("X-Service-Token", "wrong-token")
                        .param("userId", "user1")
                        .param("radius", "10"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testSearchPartnerByArea() throws Exception {
        String userId = "user1";
        double radius = 10.0;
        List<String> nearbyUserIds = Arrays.asList("user2", "user3");
        
        when(locationService.searchPartnerByArea(userId, radius)).thenReturn(nearbyUserIds);

        mockMvc.perform(get("/location/nearby")
                        .header("X-Service-Token", "test-service-token")
                        .param("userId", userId)
                        .param("radius", String.valueOf(radius)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("user2"));
    }
}

