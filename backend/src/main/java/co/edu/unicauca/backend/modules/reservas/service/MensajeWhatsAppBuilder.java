package co.edu.unicauca.backend.modules.reservas.service;

import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/**
 * Componente encargado de construir los mensajes precompuestos de WhatsApp
 * que se retornan al cliente tras operaciones sobre una reserva.
 *
 * <p>Centraliza la lógica de formateo extraída de {@link ReservaService}: arma el
 * cuerpo del mensaje con los datos clave de la reserva (cliente, identificador,
 * fecha y hora, número de personas, decoración y zona) y permite concatenar una
 * de las tres notas predefinidas según la transición ocurrida.</p>
 *
 * <p>Las notas predefinidas son:</p>
 * <ul>
 *   <li>{@link #MSG_WA_ANTICIPO}: se usa cuando la reserva pasa a ESPECIAL y el
 *       cliente debe abonar un anticipo.</li>
 *   <li>{@link #MSG_WA_CAMBIO_ESPECIAL}: se usa cuando una reserva ESPECIAL
 *       cambia de valor y el abono existente debe ajustarse manualmente.</li>
 *   <li>{@link #MSG_WA_ABONO_AJUSTE}: se usa cuando la reserva se modifica
 *       correctamente y el abono puede ajustarse si el cliente lo desea.</li>
 * </ul>
 *
 * <p>Este componente es stateless y no depende de repositorios ni servicios
 * externos; únicamente formatea texto a partir de la entidad recibida.</p>
 *
 * @see ReservaService
 * @see ReservaValidador
 * @see PreOrdenGestor
 */
@Component
public class MensajeWhatsAppBuilder {

    /**
     * Nota para cuando una reserva pasa a estado ESPECIAL y el cliente debe
     * abonar un anticipo antes de confirmar la reserva.
     */
    public static final String MSG_WA_ANTICIPO =
            "Para confirmar tu reserva especial, debes abonar un valor anticipado, comunicate para definirlo.";

    /**
     * Nota para cuando una reserva ESPECIAL cambia de valor (por ejemplo por
     * cambio de decoración o número de personas) y el abono existente debe
     * ajustarse manualmente con el restaurante.
     */
    public static final String MSG_WA_CAMBIO_ESPECIAL =
            "La reserva cambió de valor, comunicate para definir el estado de tu cuenta.";

    /**
     * Nota para cuando la reserva se modifica correctamente y el cliente puede
     * comunicarse si desea ajustar el abono ya realizado.
     */
    public static final String MSG_WA_ABONO_AJUSTE =
            "La reserva ha sido modificada correctamente. Puede comunicarse si desea una modificacion en el abono.";

    /**
     * Formateador usado para presentar la fecha y hora de la reserva en el
     * mensaje de WhatsApp. Patrón {@code dd/MM/yyyy HH:mm} (por ejemplo,
     * {@code 19/04/2026 19:30}).
     */
    private static final DateTimeFormatter FORMATTER_WA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // -----------------------------------------------------------------------
    // Construcción del mensaje
    // -----------------------------------------------------------------------

    /**
     * Construye el mensaje precompuesto para el chat de WhatsApp incluyendo el detalle
     * de la reserva y una nota específica según el tipo de transición.
     *
     * <p>La fecha y hora de la reserva se formatean con {@link #FORMATTER_WA}
     * ({@code dd/MM/yyyy HH:mm}). Los campos de decoración y zona solo aparecen
     * en el mensaje si la reserva los tiene asignados.</p>
     *
     * @param reserva entidad resultante de la operación
     * @param nota    texto final del mensaje que varía según la transición
     * @return mensaje formateado listo para enviar por WhatsApp
     */
    public String construirMensaje(Reserva reserva, String nota) {
        // StringBuilder acumulador del cuerpo del mensaje para evitar concatenaciones costosas
        StringBuilder sb = new StringBuilder();
        // Saludo inicial con el nombre del cliente dueño de la reserva
        sb.append("Hola, soy ").append(reserva.getCliente().getClienteNombre()).append(".\n");
        // Identificador de la reserva para que el personal la ubique rápidamente
        sb.append("Reserva #").append(reserva.getReservaId()).append(":\n");
        // Fecha y hora formateadas con el patrón dd/MM/yyyy HH:mm
        sb.append("- Fecha y hora: ")
          .append(reserva.getReservaFechaHoraLlegada().format(FORMATTER_WA)).append("\n");
        // Número de personas de la reserva
        sb.append("- Número de personas: ")
          .append(reserva.getReservaNumeroPersonas()).append("\n");
        // Incluir la decoración solo si la reserva tiene una asignada
        if (reserva.getDecoracion() != null) {
            sb.append("- Decoración: ")
              .append(reserva.getDecoracion().getDecoracionNombre()).append("\n");
        }
        // Incluir la zona solo si la reserva tiene una asignada
        if (reserva.getZona() != null) {
            sb.append("- Zona: ").append(reserva.getZona().getZonaNombre()).append("\n");
        }
        // Concatenar la nota específica de la transición en una línea separada
        sb.append("\n").append(nota);
        // Retornar el mensaje armado como String inmutable
        return sb.toString();
    }
}
