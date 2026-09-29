package messagingservice.config;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final Auth0JwtVerifier jwtVerifier;
    private final String[] allowedOrigins;
    private final boolean devAuth;

    public WebSocketConfig(
            @Value("${AUTH0_DOMAIN:}") String auth0Domain,
            @Value("${AUTH0_AUDIENCE:}") String auth0Audience,
            @Value("${ALLOWED_ORIGINS:http://localhost:3000}") String allowedOrigins,
            @Value("${AUTH_MODE:auth0}") String authMode) {
        this.jwtVerifier = new Auth0JwtVerifier(auth0Domain, auth0Audience);
        this.allowedOrigins = allowedOrigins.split(",");
        this.devAuth = "dev".equalsIgnoreCase(authMode);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins)
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authorization = accessor.getFirstNativeHeader("Authorization");
                    if (authorization == null || !authorization.startsWith("Bearer ")) {
                        throw new MessagingException("Unauthorized");
                    }
                    String token = authorization.substring(7);
                    if (devAuth) {
                        if (!token.startsWith("dev-") || token.length() <= 4) {
                            throw new MessagingException("Unauthorized");
                        }
                        String subject = token.substring(4);
                        accessor.setUser(() -> subject);
                    } else {
                        DecodedJWT jwt = jwtVerifier.verify(token);
                        accessor.setUser(jwt::getSubject);
                    }
                }
                return message;
            }
        });
    }
}
