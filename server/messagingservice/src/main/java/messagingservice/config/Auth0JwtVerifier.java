package messagingservice.config;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.security.interfaces.RSAPublicKey;
import java.util.concurrent.TimeUnit;

public class Auth0JwtVerifier {
    private final String domain;
    private final String audience;
    private volatile JwkProvider jwkProvider;

    public Auth0JwtVerifier(String domain, String audience) {
        this.domain = domain;
        this.audience = audience;
    }

    public DecodedJWT verify(String token) {
        if (domain == null || domain.isBlank() || audience == null || audience.isBlank()) {
            throw new JWTVerificationException("Auth0 configuration is incomplete");
        }
        try {
            DecodedJWT unverified = JWT.decode(token);
            Jwk jwk = provider().get(unverified.getKeyId());
            Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
            return JWT.require(algorithm)
                    .withIssuer("https://" + domain + "/")
                    .withAudience(audience)
                    .acceptLeeway(60)
                    .build()
                    .verify(token);
        } catch (JWTVerificationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new JWTVerificationException("Unable to verify access token", exception);
        }
    }

    private JwkProvider provider() {
        JwkProvider provider = jwkProvider;
        if (provider == null) {
            synchronized (this) {
                provider = jwkProvider;
                if (provider == null) {
                    provider = new JwkProviderBuilder("https://" + domain)
                            .cached(10, 24, TimeUnit.HOURS)
                            .rateLimited(10, 1, TimeUnit.MINUTES)
                            .build();
                    jwkProvider = provider;
                }
            }
        }
        return provider;
    }
}
