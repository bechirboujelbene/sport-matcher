package matchingservice.client;

import java.util.Collections;
import java.util.List;

import model.AuthHeaders;
import model.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Component
public class UserServiceClient {

    private static final Logger log = LoggerFactory.getLogger(UserServiceClient.class);

    private final WebClient webClient;
    private final Duration timeout;

    public UserServiceClient(WebClient.Builder builder,
                             @Value("${userservice.base-url:http://user-service:8080}") String baseUrl,
                             @Value("${INTERNAL_SERVICE_TOKEN:}") String internalServiceToken,
                             @Value("${service-client.timeout-seconds:5}") long timeoutSeconds) {
        WebClient.Builder clientBuilder = builder.baseUrl(baseUrl);
        if (!internalServiceToken.isBlank()) {
            clientBuilder.defaultHeader(AuthHeaders.SERVICE_TOKEN, internalServiceToken);
        }
        this.webClient = clientBuilder.build();
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    public UserDTO getUser(String userId) {
        try {
            return webClient.get()
                    .uri("/user/{id}", userId)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(UserDTO.class)
                    .timeout(timeout)
                    .block();
        } catch (Exception ex) {
            log.warn("Error fetching user: {}", ex.getMessage());
            return null;
        }
    }

    public List<UserDTO> getAllUsers() {
        try {
            return webClient.get()
                    .uri("/user")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToFlux(UserDTO.class)
                    .collectList()
                    .timeout(timeout)
                    .block();
        } catch (Exception ex) {
            log.warn("Error fetching users: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    public List<UserDTO> getNearbyUsers(String userId, double radiusKm) {
        try {
            return webClient.get()
                    .uri("/user/{id}/nearby?radius={r}", userId, radiusKm)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToFlux(UserDTO.class)
                    .collectList()
                    .timeout(timeout)
                    .block();
        } catch (Exception ex) {
            log.warn("Error fetching nearby users: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }
}
