package co.edu.unicauca.backend.modules.reservas.service;

import co.edu.unicauca.backend.modules.reservas.entity.Decoracion;
import co.edu.unicauca.backend.modules.reservas.entity.DecoracionZona;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.reservas.repository.BloqueDisponibilidadRepository;
import co.edu.unicauca.backend.modules.reservas.repository.DecoracionZonaRepository;
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Componente de validación para el módulo de reservas.
 *
 * <p>Centraliza todas las reglas de negocio que verifican precondiciones antes de
 * crear o modificar una reserva. Cada método lanza {@link BusinessException} si la
 * validación falla, sin retornar valores — el flujo normal continúa si no se lanza.</p>
 *
 * <p>Este componente es stateless: no modifica datos, solo los valida.</p>
 */
@Component
@RequiredArgsConstructor
public class ReservaValidador {

    /** Repositorio de bloqueos administrativos de disponibilidad. */
    private final BloqueDisponibilidadRepository bloqueRepository;

    /** Repositorio de asociaciones entre decoraciones y zonas del restaurante. */
    private final DecoracionZonaRepository decoracionZonaRepository;

    // -----------------------------------------------------------------------
    // Validación de elegibilidad de modificación
    // -----------------------------------------------------------------------

    /**
     * Valida que el cliente sea el dueño de la reserva y que esta se encuentre en un estado
     * que permita modificaciones (PENDIENTE o CONFIRMADA).
     *
     * <p>Si alguna de las condiciones no se cumple, lanza {@link BusinessException} con el
     * código de error correspondiente ({@code ACCESS_DENIED} o {@code INVALID_STATE}).</p>
     *
     * @param reserva       entidad de la reserva a validar
     * @param emailCliente  correo del cliente autenticado; {@code null} si el solicitante es ADMIN
     *                      (en ese caso se omite la verificación de ownership)
     * @throws BusinessException si el cliente no es dueño de la reserva o si el estado
     *                           de la reserva no es PENDIENTE ni CONFIRMADA
     */
    public void validarElegibilidadModificacion(Reserva reserva, String emailCliente) {

        // ADMIN (emailCliente == null) puede modificar cualquier reserva sin restricción de ownership
        if (emailCliente != null
                && !reserva.getCliente().getUsuario().getUsuarioEmail().equalsIgnoreCase(emailCliente)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED,
                    "Solo puedes modificar tus propias reservas.", HttpStatus.FORBIDDEN);
        }

        // Lista de estados que permiten modificaciones
        List<EstadoReserva> estadosActivos = List.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA);

        // Verificar que la reserva esté en un estado activo (PENDIENTE o CONFIRMADA)
        if (!estadosActivos.contains(reserva.getReservaEstado())) {
            // El estado de la reserva no permite modificaciones
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "No es posible modificar esta reserva.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    // -----------------------------------------------------------------------
    // Validación de horario
    // -----------------------------------------------------------------------

    /**
     * Verifica que la hora de llegada solicitada esté dentro del horario de atención del
     * restaurante, definido por {@code horaApertura} (inclusive) y {@code horaCierre} (exclusivo).
     *
     * @param fechaHora    fecha y hora de llegada a validar
     * @param horaApertura hora a partir de la cual el restaurante atiende (inclusive)
     * @param horaCierre   hora a partir de la cual el restaurante cierra (exclusivo)
     * @return {@code true} si la hora de llegada está dentro del horario de atención;
     *         {@code false} en caso contrario
     */
    public boolean esHorarioValido(LocalDateTime fechaHora, LocalTime horaApertura, LocalTime horaCierre) {

        // Extrae la hora del instante para comparar contra el horario de apertura
        LocalTime horaLlegada = fechaHora.toLocalTime();
        // Verifica que la hora está dentro del rango [apertura, cierre) con precisión de minutos
        return !horaLlegada.isBefore(horaApertura) && horaLlegada.isBefore(horaCierre);
    }

    // -----------------------------------------------------------------------
    // Verificación de bloqueos administrativos
    // -----------------------------------------------------------------------

    /**
     * Verifica si existe un bloqueo administrativo que cubra la fecha y hora de llegada
     * solicitada, consultando el repositorio de bloqueos de disponibilidad.
     *
     * @param fechaHora fecha y hora de llegada a verificar contra los bloqueos existentes
     * @return {@code true} si existe al menos un bloqueo que cubre la fecha y hora indicadas;
     *         {@code false} si la fecha y hora están libres de bloqueos
     */
    public boolean estaBloqueda(LocalDateTime fechaHora) {

        // Extraer la fecha y la hora por separado para la consulta al repositorio
        LocalDate fecha = fechaHora.toLocalDate();
        LocalTime hora  = fechaHora.toLocalTime();

        // Consultar el repositorio: devuelve > 0 si hay algún bloqueo activo que cubra fecha/hora
        return bloqueRepository.countBloquesParaFechaHora(fecha, hora) > 0;
    }

    // -----------------------------------------------------------------------
    // Validación de elegibilidad de cancelación
    // -----------------------------------------------------------------------

    /**
     * Valida que el cliente sea el dueño de la reserva y que esta se encuentre en un estado
     * que permita cancelación (PENDIENTE o CONFIRMADA).
     *
     * <p>A diferencia de la modificación, la cancelación no tiene restricción de hora límite;
     * siempre es posible cancelar una reserva activa. La política de reembolso se determina
     * en {@link ReservaService#cancelarReserva} según tipo y momento de cancelación.</p>
     *
     * @param reserva       entidad de la reserva a cancelar
     * @param emailCliente  correo del cliente autenticado; {@code null} si el solicitante es ADMIN
     *                      (en ese caso se omite la verificación de ownership)
     * @throws BusinessException si el cliente no es dueño (403) o el estado no es activo (422)
     */
    public void validarElegibilidadCancelacion(Reserva reserva, String emailCliente) {

        // ADMIN (emailCliente == null) puede cancelar cualquier reserva sin restricción de ownership
        if (emailCliente != null
                && !reserva.getCliente().getUsuario().getUsuarioEmail().equalsIgnoreCase(emailCliente)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED,
                    "Solo puedes cancelar tus propias reservas.", HttpStatus.FORBIDDEN);
        }

        // Solo se pueden cancelar reservas activas (PENDIENTE o CONFIRMADA)
        List<EstadoReserva> estadosActivos = List.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA);
        if (!estadosActivos.contains(reserva.getReservaEstado())) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "No es posible cancelar esta reserva.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    // -----------------------------------------------------------------------
    // Validación de compatibilidad decoración-zona
    // -----------------------------------------------------------------------

    /**
     * Valida que la decoración seleccionada sea compatible con la zona elegida, según las
     * asociaciones registradas en la tabla {@code decoracion_zona}.
     *
     * <p>Reglas de compatibilidad:
     * <ul>
     *   <li>Si la decoración no tiene ningún enlace a zona, se considera compatible con todas
     *       las zonas (sin restricción).</li>
     *   <li>Si la decoración tiene exactamente un enlace, solo puede usarse en esa zona.</li>
     *   <li>Si la decoración tiene múltiples enlaces, solo puede usarse en alguna de las zonas
     *       vinculadas.</li>
     * </ul>
     *
     * @param decoracion la decoración seleccionada para la reserva
     * @param zonaId     el identificador de la zona seleccionada para la reserva
     * @throws BusinessException si la decoración no es compatible con la zona indicada
     */
    public void validarCompatibilidadDecoracionZona(Decoracion decoracion, Long zonaId) {

        // Obtener los enlaces entre la decoración y las zonas a las que está vinculada
        List<DecoracionZona> links = decoracionZonaRepository
                .findByDecoracionId(decoracion.getDecoracionId());

        // Si la decoración no tiene enlaces a zonas, es compatible con todas las zonas: no se valida nada.
        if (links.isEmpty()) {
            return;
        }

        // Si la decoración solo tiene un enlace a zona, solo puede usarse en esa zona.
        if (links.size() == 1) {
            Long zonaPermitida = links.get(0).getZonaId();
            if (!zonaPermitida.equals(zonaId)) {
                // La zona solicitada no coincide con la única zona permitida para esta decoración
                throw new BusinessException(ErrorCode.INVALID_STATE,
                        "La decoración seleccionada solo puede usarse en su zona asignada.",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
        } else {
            // Si la decoración tiene múltiples enlaces a zonas, verificar que la zona solicitada esté entre ellas
            boolean esCompatible = links.stream()
                    .anyMatch(l -> l.getZonaId().equals(zonaId));
            if (!esCompatible) {
                // La zona solicitada no está entre las zonas compatibles con esta decoración
                throw new BusinessException(ErrorCode.INVALID_STATE,
                        "La decoración seleccionada no es compatible con la zona elegida.",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Verificación de costo de decoración
    // -----------------------------------------------------------------------

    /**
     * Determina si una decoración tiene un costo adicional mayor a cero, lo que la convierte
     * en un componente que clasifica la reserva como ESPECIAL.
     *
     * @param decoracion la decoración a evaluar; puede ser {@code null}
     * @return {@code true} si la decoración no es {@code null}, tiene costo adicional definido
     *         y dicho costo es mayor a cero; {@code false} en cualquier otro caso
     */
    public boolean tieneDecoracionConCosto(Decoracion decoracion) {

        // Si no se seleccionó decoración, no hay costo adicional
        if (decoracion == null) {
            return false;
        }

        // Si el costo adicional es nulo, se interpreta como sin costo (equivalente a cero)
        if (decoracion.getDecoracionCostoAdicional() == null) {
            return false;
        }

        // Comparar el costo adicional con cero: retorna true solo si es estrictamente positivo
        return decoracion.getDecoracionCostoAdicional().compareTo(BigDecimal.ZERO) > 0;
    }

    // -----------------------------------------------------------------------
    // Validación de elegibilidad para marcar inasistencia
    // -----------------------------------------------------------------------

    /**
     * Valida que una reserva pueda marcarse como inasistencia según las reglas de negocio.
     *
     * <p>Requisitos para marcar inasistencia:
     * <ul>
     *   <li>La reserva debe estar en estado {@code CONFIRMADA} (las PENDIENTE no pueden marcarse).</li>
     *   <li>Deben haber transcurrido al menos 30 minutos desde {@code reservaFechaHoraLlegada}.</li>
     * </ul>
     *
     * @param reserva entidad de la reserva a validar
     * @throws BusinessException con código {@code INVALID_STATE} y status {@code 422} si:
     *         <ul>
     *           <li>La reserva no está en estado {@code CONFIRMADA}.</li>
     *           <li>No han transcurrido 30 minutos desde la hora de llegada.</li>
     *         </ul>
     */
    public void validarElegibilidadInasistencia(Reserva reserva) {

        // 1. Solo reservas CONFIRMADAS pueden marcarse como inasistencia
        if (reserva.getReservaEstado() != EstadoReserva.CONFIRMADA) {
            throw new BusinessException(
                    ErrorCode.INVALID_STATE,
                    "Solo reservas confirmadas pueden marcarse como inasistencia",
                    HttpStatus.UNPROCESSABLE_ENTITY
            );
        }

        // 2. Deben haber transcurrido al menos 30 minutos desde la hora de llegada
        LocalDateTime horaLimite = reserva.getReservaFechaHoraLlegada().plusMinutes(30);
        if (LocalDateTime.now().isBefore(horaLimite)) {
            long minutosRestantes = Duration.between(LocalDateTime.now(), horaLimite).toMinutes();
            throw new BusinessException(
                    ErrorCode.INVALID_STATE,
                    "Debe esperar " + minutosRestantes + " minuto(s) más antes de marcar inasistencia",
                    HttpStatus.UNPROCESSABLE_ENTITY
            );
        }
    }
}
