package matchingservice.client;

import java.util.Collections;
import java.util.List;

import matchingservice.dto.Candidate;
import matchingservice.dto.MatchRequest;
import matchingservice.dto.MatchResponse;
import matchingservice.dto.RankedMatchDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Component
public class GenAiClient {

    private static final Logger log = LoggerFactory.getLogger(GenAiClient.class);

    private final WebClient webClient;
    private final Duration timeout;

    public GenAiClient(WebClient.Builder builder,
                       @Value("${genai.base-url:http://genai:8000}") String baseUrl,
                       @Value("${genai.timeout-seconds:15}") long timeoutSeconds) {
        this.webClient = builder.baseUrl(baseUrl).build();
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    /**
     * Call GenAI service to rank candidate users for a given user profile.
     *
     * @param userProfile Natural-language profile of the active user
     * @param candidates  List of candidate DTOs (id + profile)
     * @return ordered list of candidate IDs (best match first); empty list on error
     */
    public List<RankedMatchDTO> getRankedMatches(Candidate user, List<Candidate> candidates) {
        MatchRequest request = new MatchRequest(user, candidates);
        try {
            MatchResponse response = webClient.post()
                    .uri("/genai/match")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(MatchResponse.class)
                    .timeout(timeout)
                    .block();

            return response != null ? response.matches() : Collections.emptyList();
        } catch (Exception ex) {
            log.warn("Error calling GenAI service: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }
}
