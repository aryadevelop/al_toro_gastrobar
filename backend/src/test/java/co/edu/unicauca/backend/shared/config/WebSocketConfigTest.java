package co.edu.unicauca.backend.shared.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios de la configuración STOMP/WebSocket ({@link WebSocketConfig}).
 *
 * <p>Verifica que el broker en memoria, los prefijos de destino y el endpoint
 * STOMP se registran con los valores correctos, y que los orígenes permitidos
 * inyectados desde {@code cors.allowed-origins} se propagan al endpoint.
 *
 * <p>Estrategia de prueba:
 * <ul>
 *   <li><b>Instanciación directa:</b> se crea un {@link WebSocketConfig} real y se inyecta
 *       el campo privado {@code allowedOrigins} mediante reflexión, sin levantar
 *       contexto de Spring.</li>
 *   <li><b>Mocks de registries:</b> {@link MessageBrokerRegistry} y
 *       {@link StompEndpointRegistry} se crean con {@code Mockito.mock()} para
 *       verificar las llamadas exactas que realiza la configuración.</li>
 * </ul>
 *
 * <p>Aspectos cubiertos:
 * <ol>
 *   <li>Broker simple habilitado sobre {@code /topic}.</li>
 *   <li>Prefijo {@code /app} para mensajes dirigidos a métodos {@code @MessageMapping}.</li>
 *   <li>Solo esas dos operaciones se realizan sobre el {@code MessageBrokerRegistry}.</li>
 *   <li>Endpoint STOMP registrado en {@code /ws}.</li>
 *   <li>Solo un endpoint WebSocket registrado.</li>
 *   <li>Los orígenes inyectados se propagan íntegros a {@code setAllowedOriginPatterns}.</li>
 * </ol>
 *
 * @see WebSocketConfig
 */
@ExtendWith(MockitoExtension.class)
class WebSocketConfigTest {

    WebSocketConfig config;

    @BeforeEach
    void setUp() {
        config = new WebSocketConfig();
        inyectarAllowedOrigins(config, List.of("http://localhost:4200", "http://localhost:80"));
    }

    /**
     * Inyecta {@code allowedOrigins} en el campo privado de {@link WebSocketConfig}
     * mediante reflexión, simulando lo que hace Spring con {@code @Value}.
     *
     * @param target  instancia a la que se inyecta
     * @param origins lista de orígenes permitidos
     */
    private static void inyectarAllowedOrigins(WebSocketConfig target, List<String> origins) {
        try {
            var field = WebSocketConfig.class.getDeclaredField("allowedOrigins");
            field.setAccessible(true);
            field.set(target, origins);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Verifica las dos operaciones que {@link WebSocketConfig#configureMessageBroker}
     * debe realizar sobre el {@link MessageBrokerRegistry}.
     *
     * <p>Si el broker no se habilita en {@code /topic}, los clientes STOMP no
     * recibirían mensajes publicados con ese prefijo de destino.
     * Si el prefijo {@code /app} no se registra, los mensajes del cliente
     * no llegarían a los métodos {@code @MessageMapping}.
     */
    @Nested
    @DisplayName("configureMessageBroker")
    class ConfigureMessageBroker {

        @Test
        @DisplayName("Habilita broker simple con destino /topic")
        void habilitaBrokerSimpleConTopic() {
            MessageBrokerRegistry registry = mock(MessageBrokerRegistry.class);

            config.configureMessageBroker(registry);

            verify(registry).enableSimpleBroker("/topic");
        }

        @Test
        @DisplayName("Establece /app como prefijo de destino para la aplicación")
        void establecePrefijoApp() {
            MessageBrokerRegistry registry = mock(MessageBrokerRegistry.class);

            config.configureMessageBroker(registry);

            verify(registry).setApplicationDestinationPrefixes("/app");
        }

        @Test
        @DisplayName("Solo realiza exactamente dos operaciones sobre el registry")
        void soloDosSolicitudesAlRegistry() {
            MessageBrokerRegistry registry = mock(MessageBrokerRegistry.class);

            config.configureMessageBroker(registry);

            verify(registry, times(1)).enableSimpleBroker("/topic");
            verify(registry, times(1)).setApplicationDestinationPrefixes("/app");
            verifyNoMoreInteractions(registry);
        }
    }
    
    /**
     * Verifica que el endpoint STOMP se registra en {@code /ws} y que los orígenes
     * permitidos en el handshake son los inyectados desde {@code cors.allowed-origins}.
     *
     * <p>Un endpoint en ruta incorrecta haría que los clientes no puedan establecer
     * la conexión WebSocket. Orígenes no propagados causarían que el handshake
     * inicial sea rechazado por política CORS del broker STOMP.
     */
    @Nested
    @DisplayName("registerStompEndpoints")
    class RegisterStompEndpoints {

        StompWebSocketEndpointRegistration registration;
        StompEndpointRegistry registry;

        @BeforeEach
        void setUpMocks() {
            registration = mock(StompWebSocketEndpointRegistration.class);
            registry = mock(StompEndpointRegistry.class);
            when(registry.addEndpoint("/ws")).thenReturn(registration);
        }

        @Test
        @DisplayName("Registra el endpoint en la ruta /ws")
        void registraEndpointEnRutaWs() {
            config.registerStompEndpoints(registry);

            verify(registry).addEndpoint("/ws");
        }

        @Test
        @DisplayName("Solo registra un endpoint WebSocket")
        void soloUnEndpointRegistrado() {
            config.registerStompEndpoints(registry);

            verify(registry, times(1)).addEndpoint(anyString());
        }

        @Test
        @DisplayName("Invoca setAllowedOriginPatterns sobre el endpoint registrado")
        void invocaSetAllowedOriginPatternsEnElEndpoint() {
            config.registerStompEndpoints(registry);

            verify(registration, times(1)).setAllowedOriginPatterns(any(String[].class));
        }

        @Test
        @DisplayName("Propaga los dos orígenes configurados al endpoint")
        void propagaOrigenesAlEndpoint() {
            config.registerStompEndpoints(registry);

            // Captura el array pasado a setAllowedOriginPatterns
            var captor = org.mockito.ArgumentCaptor.forClass(String[].class);
            verify(registration).setAllowedOriginPatterns(captor.capture());
            org.assertj.core.api.Assertions.assertThat(captor.getValue())
                    .containsExactlyInAnyOrder("http://localhost:4200", "http://localhost:80");
        }

        @Test
        @DisplayName("Con un único origen, lo propaga como array de un elemento")
        void unSoloOrigen_propagaArrayDeUnElemento() {
            WebSocketConfig singleOriginConfig = new WebSocketConfig();
            inyectarAllowedOrigins(singleOriginConfig, List.of("http://localhost:3000"));

            singleOriginConfig.registerStompEndpoints(registry);

            var captor = org.mockito.ArgumentCaptor.forClass(String[].class);
            verify(registration).setAllowedOriginPatterns(captor.capture());
            org.assertj.core.api.Assertions.assertThat(captor.getValue())
                    .containsExactly("http://localhost:3000");
        }
    }
}
