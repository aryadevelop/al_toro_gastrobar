package co.edu.unicauca.backend.modules.mesas_comandas.service;

import co.edu.unicauca.backend.modules.mesas_comandas.repository.MesaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ZonaRepository;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.reservas.repository.ReservaRepository;
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

/**
 * Validador de reglas de negocio para asignación de mesas.
 *
 * <p>Centraliza validaciones antes de crear una mesa:
 * <ul>
 *   <li>Identificador no duplicado en el día</li>
 *   <li>Zona existe y tiene disponibilidad</li>
 *   <li>Reserva válida (si aplica): existe, CONFIRMADA, mismo día, horario atención</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class MesaValidador {

    private final MesaRepository mesaRepository;
    private final ZonaRepository zonaRepository;
    private final ReservaRepository reservaRepository;

    /** Hora de apertura del restaurante */
    private static final LocalTime HORA_APERTURA = LocalTime.of(17, 0);
    /** Hora de cierre del restaurante */
    private static final LocalTime HORA_CIERRE = LocalTime.of(22, 0);

    /**
     * Valida que el identificador de mesa no esté duplicado en el día actual.
     *
     * @param mesaIdentificador identificador a validar
     * @throws BusinessException con ErrorCode.ENTITY_ALREADY_EXISTS si está ocupado
     */
    public void validarIdentificadorNoOcupado(String mesaIdentificador) {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime inicioDelDia = ahora.toLocalDate().atStartOfDay();
        LocalDateTime finDelDia = ahora.toLocalDate().atTime(23, 59, 59);

        boolean existe = mesaRepository.existeMesaActivaConIdentificadorEnDia(
                mesaIdentificador, inicioDelDia, finDelDia);

        if (existe) {
            throw new BusinessException(
                    ErrorCode.ENTITY_ALREADY_EXISTS,
                    "El identificador ingresado ya está ocupado en esta fecha. Por favor elige otro.",
                    HttpStatus.CONFLICT);
        }
    }

    /**
     * Valida que la zona exista en la base de datos.
     *
     * @param zonaId ID de la zona
     * @throws BusinessException con ErrorCode.ENTITY_NOT_FOUND si no existe
     */
    public void validarZonaExiste(Long zonaId) {
        boolean existe = zonaRepository.existsById(zonaId);
        if (!existe) {
            throw new BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "La zona especificada no existe",
                    HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Valida que la hora actual esté dentro del horario de atención del restaurante.
     *
     * <p>Horario: 17:00 - 22:00 (apertura inclusive, cierre exclusive)
     *
     * @throws BusinessException si la hora actual está fuera del horario
     */
    public void validarHorarioAtencion() {
        LocalTime horaActual = LocalTime.now();

        if (horaActual.isBefore(HORA_APERTURA) || !horaActual.isBefore(HORA_CIERRE)) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_ERROR,
                    "La asignación de mesa solo está permitida en horario de atención (17:00-22:00)",
                    HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Valida que una reserva sea elegible para asignación de mesa.
     *
     * <p>Requisitos:
     * <ul>
     *   <li>La reserva debe existir</li>
     *   <li>Estado debe ser CONFIRMADA</li>
     *   <li>Fecha de llegada debe ser hoy</li>
     * </ul>
     *
     * <p><b>NOTA:</b> La validación de horario (17:00-22:00) se hace
     * mediante {@link #validarHorarioAtencion()} tanto para walk-in como reservas.
     *
     * @param reservaId ID de la reserva
     * @return Reserva validada con joins cargados
     * @throws BusinessException si la reserva no cumple requisitos
     */
    public Reserva validarReservaParaAsignacion(Long reservaId) {
        // 1. Buscar reserva en estado CONFIRMADA
        Optional<Reserva> reservaOpt = reservaRepository.findByIdAndEstadoForAsignacion(
                reservaId, EstadoReserva.CONFIRMADA);

        if (reservaOpt.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "La reserva no existe o no está en estado CONFIRMADA",
                    HttpStatus.NOT_FOUND);
        }

        Reserva reserva = reservaOpt.get();

        // 2. Validar que la fecha de llegada sea hoy
        LocalDate fechaLlegada = reserva.getReservaFechaHoraLlegada().toLocalDate();
        LocalDate hoy = LocalDate.now();

        if (!fechaLlegada.equals(hoy)) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_ERROR,
                    "La reserva no es para el día de hoy",
                    HttpStatus.BAD_REQUEST);
        }

        return reserva;
    }
}
