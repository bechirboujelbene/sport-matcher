package userservice;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import model.AuthHeaders;
import model.*;

@Service
public class LocationServiceClient {
    private final RestTemplate restTemplate;
    private final String locationServiceUrl;
    private final String internalServiceToken;

    // Inject the base URL from your application.properties
    public LocationServiceClient(RestTemplate restTemplate,
                                 @Value("${location.service.url}") String locationServiceUrl,
                                 @Value("${INTERNAL_SERVICE_TOKEN:}") String internalServiceToken) {
        this.restTemplate = restTemplate;
        this.locationServiceUrl = locationServiceUrl;
        this.internalServiceToken = internalServiceToken;
    }

    private HttpEntity<Void> serviceRequest() {
        HttpHeaders headers = new HttpHeaders();
        if (!internalServiceToken.isBlank()) {
            headers.set(AuthHeaders.SERVICE_TOKEN, internalServiceToken);
        }
        return new HttpEntity<>(headers);
    }

    public LocationDTO getLocationByUserId(String userId) {
        try {
            String url = locationServiceUrl + "/location/" + userId;
            return restTemplate.exchange(url, HttpMethod.GET, serviceRequest(), LocationDTO.class).getBody();
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        }
    }

    public List<String> searchPartnerByArea(String userId, double radius) {
        String url = locationServiceUrl + "/location/nearby?userId={userId}&radius={radius}";

        ResponseEntity<List<String>> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            serviceRequest(),
            new ParameterizedTypeReference<List<String>>() {},
            userId,
            radius
        );
        
        return response.getBody();
    }
}

