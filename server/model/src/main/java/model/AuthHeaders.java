package model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class AuthHeaders {
    public static final String USER_ID = "X-User-Id";
    public static final String SERVICE_TOKEN = "X-Service-Token";

    private AuthHeaders() {
    }

    public static boolean isAuthenticatedUser(String userId) {
        return userId != null && !userId.isBlank();
    }

    public static boolean isSameUser(String authenticatedUserId, String requestedUserId) {
        return isAuthenticatedUser(authenticatedUserId)
                && requestedUserId != null
                && authenticatedUserId.equals(requestedUserId);
    }

    public static boolean isServiceRequest(String configuredToken, String providedToken) {
        if (configuredToken == null || configuredToken.isBlank() || providedToken == null) {
            return false;
        }
        return MessageDigest.isEqual(
                configuredToken.getBytes(StandardCharsets.UTF_8),
                providedToken.getBytes(StandardCharsets.UTF_8));
    }
}
