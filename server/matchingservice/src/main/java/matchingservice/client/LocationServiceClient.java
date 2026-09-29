package matchingservice.client;

import model.AuthHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class LocationServiceClient {

    private static final Logger log = LoggerFactory.getLogger(LocationServiceClient.class);

    private final WebClient webClient;
    private final Duration timeout;

    public record LocationDto(String id, String name, double latitude, double longitude) {}

    public LocationServiceClient(WebClient.Builder builder,
                                 @Value("${locationservice.base-url:http://location-service:8080}") String baseUrl,
                                 @Value("${INTERNAL_SERVICE_TOKEN:}") String internalServiceToken,
                                 @Value("${service-client.timeout-seconds:5}") long timeoutSeconds) {
        WebClient.Builder clientBuilder = builder.baseUrl(baseUrl);
        if (!internalServiceToken.isBlank()) {
            clientBuilder.defaultHeader(AuthHeaders.SERVICE_TOKEN, internalServiceToken);
        }
        this.webClient = clientBuilder.build();
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    public LocationDto getLocation(String userId) {
        try {
            return webClient.get()
                    .uri("/location/{id}", userId)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(LocationDto.class)
                    .timeout(timeout)
                    .onErrorResume(err -> Mono.empty())
                    .block();
        } catch (Exception ex) {
            log.warn("Error calling location-service: {}", ex.getMessage());
            return null;
        }
    }
}
