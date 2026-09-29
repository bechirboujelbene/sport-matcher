package messagingservice.client;

import model.AuthHeaders;
import model.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Component
public class UserServiceClient {

    private static final Logger log = LoggerFactory.getLogger(UserServiceClient.class);

    private final WebClient webClient;
    private final Duration timeout;

    public UserServiceClient(
            @Value("${user.service.url:http://user-service:8080}") String baseUrl,
            @Value("${INTERNAL_SERVICE_TOKEN:}") String internalServiceToken,
            @Value("${service-client.timeout-seconds:5}") long timeoutSeconds) {
        WebClient.Builder clientBuilder = WebClient.builder().baseUrl(baseUrl);
        if (!internalServiceToken.isBlank()) {
            clientBuilder.defaultHeader(AuthHeaders.SERVICE_TOKEN, internalServiceToken);
        }
        this.webClient = clientBuilder.build();
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    public UserDTO getUser(String id) {
        try {
            return webClient.get()
                    .uri("/user/{id}", id)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(UserDTO.class)
                    .timeout(timeout)
                    .block();
        } catch (Exception ex) {
            log.warn("Error fetching user {}: {}", id, ex.getMessage());
            return null;
        }
    }

    public List<UserDTO> getUsers(List<String> ids) {
        // No dedicated batch endpoint; fall back to serial fetch
        return ids == null ? Collections.emptyList()
                : ids.stream()
                        .map(this::getUser)
                        .filter(u -> u != null)
                        .toList();
    }
}
