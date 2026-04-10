package co.edu.unicauca.backend.shared.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios de la configuración de mensajería ({@link RabbitMQConfig}).
 *
 * <p>Verifica que la topología declarada coincide exactamente con lo especificado 
 * en las constantes públicas y con las propiedades requeridas para producción.
 *
 * <p>Estrategia de prueba:
 * <ul>
 *   <li><b>Instanciación directa:</b> se crea un {@link RabbitMQConfig} real en cada test
 *       sin levantar contexto de Spring, lo que hace los tests rápidos y aislados.</li>
 *   <li><b>Mock de {@link ConnectionFactory}:</b> única dependencia externa, requerida
 *       únicamente por el bean {@code rabbitTemplate}; se mockea con Mockito para evitar
 *       conexión real al broker.</li>
 * </ul>
 *
 * <p>Aspectos cubiertos:
 * <ol>
 *   <li>Valores de las constantes públicas (exchange, routing keys, nombres de cola).</li>
 *   <li>{@code TopicExchange} durable con el nombre correcto.</li>
 *   <li>Las cuatro colas son durables y tienen los nombres esperados.</li>
 *   <li>Cada {@code Binding} enlaza la cola correcta al exchange con la routing key exacta.</li>
 *   <li>El {@code MessageConverter} devuelto es {@link Jackson2JsonMessageConverter}.</li>
 *   <li>El {@code RabbitTemplate} usa el conversor JSON.</li>
 * </ol>
 *
 * @see RabbitMQConfig
 */
@ExtendWith(MockitoExtension.class)
class RabbitMQConfigTest {

    @Mock
    ConnectionFactory connectionFactory;

    RabbitMQConfig config;

    @BeforeEach
    void setUp() {
        config = new RabbitMQConfig();
    }

    /**
     * Verifica que las constantes públicas tienen los valores literales esperados.
     *
     * <p>Los publishers y consumidores referencian estas constantes directamente;
     * un cambio accidental de valor rompería la topología en el broker.
     */
    @Nested
    @DisplayName("Constantes públicas")
    class Constantes {

        @Test
        @DisplayName("EXCHANGE → 'altoro.topic'")
        void exchange_tieneValorCorrecto() {
            assertThat(RabbitMQConfig.EXCHANGE).isEqualTo("altoro.topic");
        }

        @Test
        @DisplayName("RK_COMANDA_NUEVA → 'comanda.nueva'")
        void rkComandaNueva_tieneValorCorrecto() {
            assertThat(RabbitMQConfig.RK_COMANDA_NUEVA).isEqualTo("comanda.nueva");
        }

        @Test
        @DisplayName("RK_IMPRESION_TICKET → 'impresion.ticket'")
        void rkImpresionTicket_tieneValorCorrecto() {
            assertThat(RabbitMQConfig.RK_IMPRESION_TICKET).isEqualTo("impresion.ticket");
        }

        @Test
        @DisplayName("RK_NOTIFICACION_EMAIL → 'notificacion.email'")
        void rkNotificacionEmail_tieneValorCorrecto() {
            assertThat(RabbitMQConfig.RK_NOTIFICACION_EMAIL).isEqualTo("notificacion.email");
        }

        @Test
        @DisplayName("RK_NOTIFICACION_WS → 'notificacion.ws'")
        void rkNotificacionWs_tieneValorCorrecto() {
            assertThat(RabbitMQConfig.RK_NOTIFICACION_WS).isEqualTo("notificacion.ws");
        }

        @Test
        @DisplayName("Q_COMANDA_PRODUCCION → 'q.comanda.produccion'")
        void qComandaProduccion_tieneValorCorrecto() {
            assertThat(RabbitMQConfig.Q_COMANDA_PRODUCCION).isEqualTo("q.comanda.produccion");
        }

        @Test
        @DisplayName("Q_IMPRESION_TICKET → 'q.impresion.ticket'")
        void qImpresionTicket_tieneValorCorrecto() {
            assertThat(RabbitMQConfig.Q_IMPRESION_TICKET).isEqualTo("q.impresion.ticket");
        }

        @Test
        @DisplayName("Q_NOTIFICACION_EMAIL → 'q.notificacion.email'")
        void qNotificacionEmail_tieneValorCorrecto() {
            assertThat(RabbitMQConfig.Q_NOTIFICACION_EMAIL).isEqualTo("q.notificacion.email");
        }

        @Test
        @DisplayName("Q_NOTIFICACION_WS → 'q.notificacion.ws'")
        void qNotificacionWs_tieneValorCorrecto() {
            assertThat(RabbitMQConfig.Q_NOTIFICACION_WS).isEqualTo("q.notificacion.ws");
        }
    }

    /**
     * Verifica las propiedades del {@link TopicExchange} declarado.
     *
     * <p>Un exchange no durable se perdería al reiniciar el broker, lo que causaría
     * pérdida de mensajes no consumidos; el nombre incorrecto rompería la topología.
     */
    @Nested
    @DisplayName("Bean altoroExchange")
    class AltoroExchange {

        @Test
        @DisplayName("Es de tipo TopicExchange")
        void exchange_esTipoTopic() {
            TopicExchange exchange = config.altoroExchange();
            assertThat(exchange).isInstanceOf(TopicExchange.class);
        }

        @Test
        @DisplayName("Tiene el nombre 'altoro.topic'")
        void exchange_tieneNombreCorrecto() {
            TopicExchange exchange = config.altoroExchange();
            assertThat(exchange.getName()).isEqualTo(RabbitMQConfig.EXCHANGE);
        }

        @Test
        @DisplayName("Es durable (sobrevive a reinicios del broker)")
        void exchange_esDurable() {
            TopicExchange exchange = config.altoroExchange();
            assertThat(exchange.isDurable()).isTrue();
        }
    }
    
    /**
     * Verifica que cada cola declarada es durable y tiene el nombre correcto.
     *
     * <p>Las colas no durables se eliminan al reiniciar el broker; los mensajes
     * en vuelo se perderían sin posibilidad de reintento.
     */
    @Nested
    @DisplayName("Beans de colas")
    class Colas {

        @Nested
        @DisplayName("qComandaProduccion")
        class QComandaProduccion {

            @Test
            @DisplayName("Nombre → '" + RabbitMQConfig.Q_COMANDA_PRODUCCION + "'")
            void nombre_correcto() {
                Queue q = config.qComandaProduccion();
                assertThat(q.getName()).isEqualTo(RabbitMQConfig.Q_COMANDA_PRODUCCION);
            }

            @Test
            @DisplayName("Es durable")
            void esDurable() {
                assertThat(config.qComandaProduccion().isDurable()).isTrue();
            }
        }

        @Nested
        @DisplayName("qImpresionTicket")
        class QImpresionTicket {

            @Test
            @DisplayName("Nombre → '" + RabbitMQConfig.Q_IMPRESION_TICKET + "'")
            void nombre_correcto() {
                Queue q = config.qImpresionTicket();
                assertThat(q.getName()).isEqualTo(RabbitMQConfig.Q_IMPRESION_TICKET);
            }

            @Test
            @DisplayName("Es durable")
            void esDurable() {
                assertThat(config.qImpresionTicket().isDurable()).isTrue();
            }
        }

        @Nested
        @DisplayName("qNotificacionEmail")
        class QNotificacionEmail {

            @Test
            @DisplayName("Nombre → '" + RabbitMQConfig.Q_NOTIFICACION_EMAIL + "'")
            void nombre_correcto() {
                Queue q = config.qNotificacionEmail();
                assertThat(q.getName()).isEqualTo(RabbitMQConfig.Q_NOTIFICACION_EMAIL);
            }

            @Test
            @DisplayName("Es durable")
            void esDurable() {
                assertThat(config.qNotificacionEmail().isDurable()).isTrue();
            }
        }

        @Nested
        @DisplayName("qNotificacionWs")
        class QNotificacionWs {

            @Test
            @DisplayName("Nombre → '" + RabbitMQConfig.Q_NOTIFICACION_WS + "'")
            void nombre_correcto() {
                Queue q = config.qNotificacionWs();
                assertThat(q.getName()).isEqualTo(RabbitMQConfig.Q_NOTIFICACION_WS);
            }

            @Test
            @DisplayName("Es durable")
            void esDurable() {
                assertThat(config.qNotificacionWs().isDurable()).isTrue();
            }
        }
    }

    // ── Bindings ──────────────────────────────────────────────────────────────

    /**
     * Verifica que cada {@link Binding} conecta la cola correcta al exchange
     * con la routing key exacta.
     *
     * <p>Una routing key errónea haría que los mensajes publicados no lleguen
     * a la cola consumidora, silenciando operaciones críticas como la creación
     * de comandas o el envío de notificaciones.
     */
    @Nested
    @DisplayName("Bindings")
    class Bindings {

        TopicExchange exchange;
        Queue qComandaProduccion;
        Queue qImpresionTicket;
        Queue qNotificacionEmail;
        Queue qNotificacionWs;

        @BeforeEach
        void setUpBeans() {
            exchange = config.altoroExchange();
            qComandaProduccion = config.qComandaProduccion();
            qImpresionTicket = config.qImpresionTicket();
            qNotificacionEmail = config.qNotificacionEmail();
            qNotificacionWs = config.qNotificacionWs();
        }

        @Test
        @DisplayName("bindingComandaProduccion → routing key 'comanda.nueva'")
        void bindingComandaProduccion_tieneRoutingKeyCorrecta() {
            Binding binding = config.bindingComandaProduccion(qComandaProduccion, exchange);
            assertThat(binding.getRoutingKey()).isEqualTo(RabbitMQConfig.RK_COMANDA_NUEVA);
        }

        @Test
        @DisplayName("bindingComandaProduccion → destino es 'q.comanda.produccion'")
        void bindingComandaProduccion_apuntaAColaCorrecta() {
            Binding binding = config.bindingComandaProduccion(qComandaProduccion, exchange);
            assertThat(binding.getDestination()).isEqualTo(RabbitMQConfig.Q_COMANDA_PRODUCCION);
        }

        @Test
        @DisplayName("bindingComandaProduccion → exchange es 'altoro.topic'")
        void bindingComandaProduccion_usaExchangeCorrecto() {
            Binding binding = config.bindingComandaProduccion(qComandaProduccion, exchange);
            assertThat(binding.getExchange()).isEqualTo(RabbitMQConfig.EXCHANGE);
        }

        @Test
        @DisplayName("bindingImpresionTicket → routing key 'impresion.ticket'")
        void bindingImpresionTicket_tieneRoutingKeyCorrecta() {
            Binding binding = config.bindingImpresionTicket(qImpresionTicket, exchange);
            assertThat(binding.getRoutingKey()).isEqualTo(RabbitMQConfig.RK_IMPRESION_TICKET);
        }

        @Test
        @DisplayName("bindingImpresionTicket → destino es 'q.impresion.ticket'")
        void bindingImpresionTicket_apuntaAColaCorrecta() {
            Binding binding = config.bindingImpresionTicket(qImpresionTicket, exchange);
            assertThat(binding.getDestination()).isEqualTo(RabbitMQConfig.Q_IMPRESION_TICKET);
        }

        @Test
        @DisplayName("bindingNotificacionEmail → routing key 'notificacion.email'")
        void bindingNotificacionEmail_tieneRoutingKeyCorrecta() {
            Binding binding = config.bindingNotificacionEmail(qNotificacionEmail, exchange);
            assertThat(binding.getRoutingKey()).isEqualTo(RabbitMQConfig.RK_NOTIFICACION_EMAIL);
        }

        @Test
        @DisplayName("bindingNotificacionEmail → destino es 'q.notificacion.email'")
        void bindingNotificacionEmail_apuntaAColaCorrecta() {
            Binding binding = config.bindingNotificacionEmail(qNotificacionEmail, exchange);
            assertThat(binding.getDestination()).isEqualTo(RabbitMQConfig.Q_NOTIFICACION_EMAIL);
        }

        @Test
        @DisplayName("bindingNotificacionWs → routing key 'notificacion.ws'")
        void bindingNotificacionWs_tieneRoutingKeyCorrecta() {
            Binding binding = config.bindingNotificacionWs(qNotificacionWs, exchange);
            assertThat(binding.getRoutingKey()).isEqualTo(RabbitMQConfig.RK_NOTIFICACION_WS);
        }

        @Test
        @DisplayName("bindingNotificacionWs → destino es 'q.notificacion.ws'")
        void bindingNotificacionWs_apuntaAColaCorrecta() {
            Binding binding = config.bindingNotificacionWs(qNotificacionWs, exchange);
            assertThat(binding.getDestination()).isEqualTo(RabbitMQConfig.Q_NOTIFICACION_WS);
        }
    }

    /**
     * Verifica que el conversor registrado usa Jackson para serializar mensajes a JSON.
     *
     * <p>Si se usara el conversor por defecto ({@code SimpleMessageConverter}),
     * los payloads se enviarían como bytes crudos y los consumidores no podrían
     * deserializar los objetos Java automáticamente.
     */
    @Nested
    @DisplayName("Bean jacksonMessageConverter")
    class JacksonMessageConverter {

        @Test
        @DisplayName("Devuelve una instancia de Jackson2JsonMessageConverter")
        void converter_esJacksonJsonMessageConverter() {
            MessageConverter converter = config.jacksonMessageConverter();
            assertThat(converter).isInstanceOf(Jackson2JsonMessageConverter.class);
        }
    }
    
    /**
     * Verifica que el {@link RabbitTemplate} compartido está configurado con el
     * conversor JSON y acepta una {@link ConnectionFactory}.
     *
     * <p>Un template sin conversor JSON enviaría los mensajes como bytes en lugar
     * de JSON, rompiendo la integración con los consumidores de la aplicación.
     */
    @Nested
    @DisplayName("Bean rabbitTemplate")
    class RabbitTemplateBean {

        @Test
        @DisplayName("No es nulo dado un ConnectionFactory")
        void template_noEsNulo() {
            RabbitTemplate template = config.rabbitTemplate(connectionFactory);
            assertThat(template).isNotNull();
        }

        @Test
        @DisplayName("Usa Jackson2JsonMessageConverter como conversor de mensajes")
        void template_usaJacksonConverter() {
            RabbitTemplate template = config.rabbitTemplate(connectionFactory);
            assertThat(template.getMessageConverter())
                    .isInstanceOf(Jackson2JsonMessageConverter.class);
        }
    }
}
