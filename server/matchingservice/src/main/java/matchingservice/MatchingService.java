package matchingservice;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import model.UserDTO;
import model.MatcherDTO;
import matchingservice.dto.RankedMatchDTO;
import matchingservice.client.GenAiClient;
import matchingservice.client.UserServiceClient;
import matchingservice.entity.Match;
import matchingservice.repository.MatchRepository;
import matchingservice.dto.Candidate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MatchingService {

    private static final double NEARBY_RADIUS_KM = 50.0;

    private final GenAiClient genAiClient;

    private final UserServiceClient userServiceClient;
    private final MatchRepository matchRepository;

    public MatchingService(GenAiClient genAiClient, UserServiceClient userServiceClient, MatchRepository matchRepository) {
        this.genAiClient = genAiClient;
        this.userServiceClient = userServiceClient;
        this.matchRepository = matchRepository;

    }

    @Transactional
    public List<UserDTO> findPartners(String userId) {

        // fetch user profile from user-service
        UserDTO user = userServiceClient.getUser(userId);
        if (user == null) {
            return null;
        }
        Candidate userCandidate = new Candidate(user.id(), user.name(), user.sportInterests(), user.bio(), user.skillLevel());
        // fetch candidate users (nearby)
        List<UserDTO> nearbyUsers = userServiceClient.getNearbyUsers(userId, NEARBY_RADIUS_KM);
        if (nearbyUsers == null) {
            nearbyUsers = Collections.emptyList();
        }
        Map<String, UserDTO> usersById = nearbyUsers.stream()
                .collect(Collectors.toMap(UserDTO::id, Function.identity(), (a, b) -> a));
        usersById.putIfAbsent(userId, user);
        List<Candidate> candidates = usersById.values().stream()
            .filter(u -> !u.id().equals(userId)) // exclude the main user from candidates
            .map(u -> new Candidate(u.id(), u.name(), u.sportInterests(), u.bio(), u.skillLevel()))
            .toList();
        List<RankedMatchDTO> ranked = genAiClient.getRankedMatches(userCandidate, candidates);
        if (ranked == null || ranked.isEmpty()) {
            return Collections.emptyList();
        }
        // Overwrite previous matches atomically with the new ranking
        matchRepository.deleteByUserId(userId);
        matchRepository.saveAll(ranked.stream()
                .map(dto -> new Match(userId, dto.id(), dto.score(), dto.explanation(),
                        String.join(",", dto.commonPreferences())))
                .toList());
        // Return matched users in ranked order using the already-fetched profiles
        return ranked.stream()
                .map(dto -> usersById.get(dto.id()))
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Retrieve previously stored matches for a user, ordered by best score.
     */
    public List<MatcherDTO> getMatches(String userId) {
        return matchRepository.findByUserIdOrderByScoreDesc(userId).stream()
                .map(matchingservice.mapper.MatchMapper::toDto)
                .toList();
    }
}
