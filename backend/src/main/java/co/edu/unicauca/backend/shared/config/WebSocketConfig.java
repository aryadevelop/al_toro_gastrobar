package co.edu.unicauca.backend.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

/**
 * Configuración del broker STOMP sobre WebSocket para notificaciones en tiempo real.
 *
 * <p>Endpoint de conexión: {@code /ws} (con fallback SockJS).
 * Los clientes se suscriben a {@code /topic/xxx} para recibir eventos del servidor;
 * los mensajes dirigidos al servidor usan el prefijo {@code /app}.
 *
 * <p>Los orígenes permitidos se leen desde {@code cors.allowed-origins},
 * la misma propiedad que usa {@link SecurityConfig}.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Orígenes permitidos en el handshake WebSocket, inyectados desde
     * {@code cors.allowed-origins}.
     */
    @Value("${cors.allowed-origins}")
    private List<String> allowedOrigins;

    /**
     * Habilita el broker en memoria para {@code /topic} y establece el prefijo
     * {@code /app} para mensajes destinados a métodos {@code @MessageMapping}.
     *
     * @param registry registro de configuración del broker
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Registra el endpoint STOMP {@code /ws} y los orígenes
     * permitidos tomados de {@code cors.allowed-origins}.
     *
     * @param registry registro de endpoints WebSocket
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins.toArray(String[]::new));
    }
}
