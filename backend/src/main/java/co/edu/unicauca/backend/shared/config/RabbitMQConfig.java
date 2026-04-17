package co.edu.unicauca.backend.shared.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de la infraestructura de mensajería asíncrona con RabbitMQ.
 *
 * <p>Define la topología de exchange, colas y bindings usados en la aplicación,
 * así como el conversor de mensajes JSON y el {@link RabbitTemplate} compartido.
 *
 * <p>Las routing keys y los nombres de cola se exponen como constantes públicas
 * para que los publishers las referencien sin cadenas literales dispersas.
 */
@Configuration
public class RabbitMQConfig {

    // ── Exchange ──────────────────────────────────────────────────────────────

    /** Nombre del {@code TopicExchange} central de la aplicación. */
    public static final String EXCHANGE = "altoro.topic";

    // ── Routing keys ──────────────────────────────────────────────────────────

    /** Routing key publicada cuando se crea una nueva comanda. */
    public static final String RK_COMANDA_NUEVA = "comanda.nueva";

    /** Routing key publicada para solicitar la impresión de un ticket. */
    public static final String RK_IMPRESION_TICKET = "impresion.ticket";

    /** Routing key publicada para enviar una notificación por correo. */
    public static final String RK_NOTIFICACION_EMAIL = "notificacion.email";

    /** Routing key publicada para emitir una notificación por WebSocket. */
    public static final String RK_NOTIFICACION_WS = "notificacion.ws";

    // ── Queue names ───────────────────────────────────────────────────────────

    /** Cola consumida por {@code ProduccionService} para procesar nuevas comandas. */
    public static final String Q_COMANDA_PRODUCCION = "q.comanda.produccion";

    /** Cola consumida por el bridge Node.js para la impresión de tickets. */
    public static final String Q_IMPRESION_TICKET = "q.impresion.ticket";

    /** Cola consumida por {@code NotificacionEmailService} para el envío de correos. */
    public static final String Q_NOTIFICACION_EMAIL= "q.notificacion.email";

    /** Cola consumida por el publicador WebSocket para notificaciones en tiempo real. */
    public static final String Q_NOTIFICACION_WS = "q.notificacion.ws";

    // ── Exchange bean ─────────────────────────────────────────────────────────

    /**
     * Declara el {@code TopicExchange} principal como durable.
     *
     * <p>Al ser durable, el exchange sobrevive a reinicios del broker.
     *
     * @return exchange configurado con el nombre {@value #EXCHANGE}
     */
    @Bean
    TopicExchange altoroExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    // ── Queue beans ───────────────────────────────────────────────────────────

    /**
     * Declara la cola durable para el procesamiento de nuevas comandas en producción.
     *
     * @return cola con el nombre {@value #Q_COMANDA_PRODUCCION}
     */
    @Bean
    Queue qComandaProduccion() {
        return QueueBuilder.durable(Q_COMANDA_PRODUCCION).build();
    }

    /**
     * Declara la cola durable para las solicitudes de impresión de tickets.
     *
     * @return cola con el nombre {@value #Q_IMPRESION_TICKET}
     */
    @Bean
    Queue qImpresionTicket() {
        return QueueBuilder.durable(Q_IMPRESION_TICKET).build();
    }

    /**
     * Declara la cola durable para el envío de notificaciones por correo electrónico.
     *
     * @return cola con el nombre {@value #Q_NOTIFICACION_EMAIL}
     */
    @Bean
    Queue qNotificacionEmail() {
        return QueueBuilder.durable(Q_NOTIFICACION_EMAIL).build();
    }

    /**
     * Declara la cola durable para la emisión de notificaciones por WebSocket.
     *
     * @return cola con el nombre {@value #Q_NOTIFICACION_WS}
     */
    @Bean
    Queue qNotificacionWs() {
        return QueueBuilder.durable(Q_NOTIFICACION_WS).build();
    }

    // ── Bindings ──────────────────────────────────────────────────────────────

    /**
     * Enlaza la cola de producción al exchange con la routing key {@value #RK_COMANDA_NUEVA}.
     *
     * @param qComandaProduccion cola destino
     * @param altoroExchange     exchange de la aplicación
     * @return binding configurado
     */
    @Bean
    Binding bindingComandaProduccion(Queue qComandaProduccion, TopicExchange altoroExchange) {
        return BindingBuilder.bind(qComandaProduccion).to(altoroExchange).with(RK_COMANDA_NUEVA);
    }

    /**
     * Enlaza la cola de impresión al exchange con la routing key {@value #RK_IMPRESION_TICKET}.
     *
     * @param qImpresionTicket cola destino
     * @param altoroExchange   exchange de la aplicación
     * @return binding configurado
     */
    @Bean
    Binding bindingImpresionTicket(Queue qImpresionTicket, TopicExchange altoroExchange) {
        return BindingBuilder.bind(qImpresionTicket).to(altoroExchange).with(RK_IMPRESION_TICKET);
    }

    /**
     * Enlaza la cola de notificaciones por email al exchange con la routing key
     * {@value #RK_NOTIFICACION_EMAIL}.
     *
     * @param qNotificacionEmail cola destino
     * @param altoroExchange     exchange de la aplicación
     * @return binding configurado
     */
    @Bean
    Binding bindingNotificacionEmail(Queue qNotificacionEmail, TopicExchange altoroExchange) {
        return BindingBuilder.bind(qNotificacionEmail).to(altoroExchange).with(RK_NOTIFICACION_EMAIL);
    }   

    /**
     * Enlaza la cola de notificaciones WebSocket al exchange con la routing key
     * {@value #RK_NOTIFICACION_WS}.
     *
     * @param qNotificacionWs cola destino
     * @param altoroExchange  exchange de la aplicación
     * @return binding configurado
     */
    @Bean
    Binding bindingNotificacionWs(Queue qNotificacionWs, TopicExchange altoroExchange) {
        return BindingBuilder.bind(qNotificacionWs).to(altoroExchange).with(RK_NOTIFICACION_WS);
    }

    // ── Message converter & template ──────────────────────────────────────────

    /**
     * Registra un conversor que serializa y deserializa los payloads de mensajes como JSON.
     *
     * <p>Al usar {@link Jackson2JsonMessageConverter}, los publishers pueden enviar
     * objetos Java directamente y los consumidores los reciben ya deserializados.
     *
     * @return conversor Jackson para mensajes AMQP
     */
    @Bean
    MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Configura el {@link RabbitTemplate} compartido con el conversor JSON.
     *
     * <p>Es el componente principal que usan los publishers para enviar mensajes
     * al broker mediante {@code rabbitTemplate.convertAndSend(...)}.
     *
     * @param connectionFactory fábrica de conexiones al broker RabbitMQ
     * @return template listo para publicar mensajes JSON
     */
    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jacksonMessageConverter());
        return template;
    }
}
