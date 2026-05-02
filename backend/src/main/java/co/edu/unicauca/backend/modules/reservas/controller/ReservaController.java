package co.edu.unicauca.backend.modules.reservas.controller;

import co.edu.unicauca.backend.modules.reservas.dto.request.CrearReservaRequest;
import co.edu.unicauca.backend.modules.reservas.dto.response.CancelarReservaResponse;
import co.edu.unicauca.backend.modules.reservas.dto.request.ModificarReservaRequest;
import co.edu.unicauca.backend.modules.reservas.dto.response.DisponibilidadResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.MarcarInasistenciaResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ModificarReservaResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ReservaDetalleResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ReservaResponse;
import co.edu.unicauca.backend.modules.reservas.service.ReservaService;
import co.edu.unicauca.backend.shared.dto.ApiResponse;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;


/**
 * Controlador REST para la gestión de reservas de clientes.
 *
 * <p>Expone los endpoints bajo {@code /api/reservas} y delega toda la lógica
 * de negocio en {@link ReservaService}.
 *
 * @see ReservaService
 */
@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
@Tag(name = "Reservas", description = "Gestión de reservas de clientes")
public class ReservaController {

    private final ReservaService reservaService;

    /**
     * Consulta la disponibilidad de zonas y decoraciones para una fecha y hora dadas.
     *
     * <p>Retorna {@code disponible=false} en los siguientes casos:
     * <ul>
     *   <li>La hora está fuera del horario de atención (lunes–domingo, 5 PM–10 PM).</li>
     *   <li>Existe un bloqueo activo registrado por el administrador.</li>
     * </ul>
     *
     * @param fechaHora fecha y hora a consultar en formato ISO-8601
     * @return respuesta con el detalle de disponibilidad, zonas y decoraciones libres
     */
    @GetMapping("/disponibilidad")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Consultar disponibilidad para una fecha y hora")
    public ResponseEntity<ApiResponse<DisponibilidadResponse>> consultarDisponibilidad(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHora) {

        DisponibilidadResponse response = reservaService.consultarDisponibilidad(fechaHora);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Crea una nueva reserva.
     *
     * <p>Si el solicitante tiene rol {@code CLIENTE}, el email de la reserva se toma
     * del token de autenticación, ignorando el campo {@code emailCliente} del body.
     * Otros roles pueden especificar el email en el body para crear reservas a nombre de otro cliente.
     *
     * @param request        datos de la reserva (fecha, zona, decoración y pre-orden opcional)
     * @param authentication contexto de seguridad del request
     * @return respuesta {@code 201 Created} con el detalle de la reserva registrada
     */
    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Crear nueva reserva")
    public ResponseEntity<ApiResponse<ReservaResponse>> crearReserva(
            @Valid @RequestBody CrearReservaRequest request,
            Authentication authentication) {

        String emailSiCliente = emailSiCliente(authentication);
        String emailEfectivo = emailSiCliente != null ? emailSiCliente : request.getEmailCliente();

        ReservaResponse response = reservaService.crearReserva(emailEfectivo, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Reserva creada exitosamente", response));
    }

    /**
     * Modifica una reserva futura del cliente autenticado.
     *
     * <p>Solo el cliente propietario puede modificar su reserva ({@code ROLE_CLIENTE}).
     * El email del cliente se toma del token de autenticación, nunca del body.
     *
     * <p>Reglas de negocio aplicadas:
     * <ul>
     *   <li>La reserva debe estar en estado {@code PENDIENTE} o {@code CONFIRMADA}.</li>
     *   <li>El momento actual debe ser anterior a las 16:00 del día de la reserva (CA-01).</li>
     *   <li>Aplica las mismas validaciones de horario, bloqueos y disponibilidad que la creación.</li>
     * </ul>
     *
     * <p>Cuando el campo {@code requiereWhatsApp} de la respuesta es {@code true}, el frontend
     * debe redirigir al cliente al chat de WhatsApp de la empresa con el mensaje precompuesto.
     *
     * @param reservaId      identificador de la reserva a modificar
     * @param request        nuevos datos de la reserva
     * @param authentication contexto de seguridad del request
     * @return {@code 200 OK} con los datos de la reserva resultante
     */
    @PutMapping("/{reservaId}")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMIN')")
    @Operation(summary = "Modificar una reserva futura del cliente")
    public ResponseEntity<ApiResponse<ModificarReservaResponse>> modificarReserva(
            @PathVariable Long reservaId,
            @Valid @RequestBody ModificarReservaRequest request,
            Authentication authentication) {

        ModificarReservaResponse response = reservaService.modificarReserva(reservaId, emailSiCliente(authentication), request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Cancela una reserva futura del cliente autenticado.
     *
     * <p>Solo el cliente propietario puede cancelar su reserva ({@code ROLE_CLIENTE}).
     * El email del cliente se toma del token de autenticación, nunca del body ni de
     * query params.
     *
     * <p>Cuando {@code requiereWhatsApp} es {@code true} en la respuesta, el frontend
     * debe redirigir al cliente al chat de WhatsApp de la empresa con el mensaje
     * precompuesto para gestionar el reembolso del abono.
     *
     * <p>A diferencia de la modificación, no existe hora límite para cancelar: se puede
     * cancelar en cualquier momento mientras el estado sea {@code PENDIENTE} o
     * {@code CONFIRMADA}.
     *
     * @param reservaId      identificador de la reserva a cancelar
     * @param authentication contexto de seguridad del request
     * @return {@code 200 OK} con el estado final de la reserva cancelada y el indicador
     *         de redirección a WhatsApp
     */
    @PatchMapping("/{reservaId}/cancelar")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMIN')")
    @Operation(summary = "Cancelar una reserva futura del cliente")
    public ResponseEntity<ApiResponse<CancelarReservaResponse>> cancelarReserva(
            @PathVariable Long reservaId,
            Authentication authentication) {

        CancelarReservaResponse response = reservaService.cancelarReserva(reservaId, emailSiCliente(authentication));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Marca una reserva confirmada como inasistencia tras 30 minutos de tolerancia.
     *
     * <p>Solo meseros y administradores pueden ejecutar esta acción. No se requiere
     * ownership validation: cualquier mesero puede marcar inasistencia de cualquier reserva.
     *
     * <p>Reglas de negocio (HE-03-HU-02-CA-04):
     * <ul>
     *   <li>La reserva debe estar en estado {@code CONFIRMADA}.</li>
     *   <li>Deben haber transcurrido al menos 30 minutos desde la hora de llegada programada.</li>
     *   <li>El cambio es irreversible: estado {@code INASISTENCIA} es terminal.</li>
     *   <li>Libera zona y decoración automáticamente (se excluye de cálculos de disponibilidad).</li>
     * </ul>
     *
     * @param reservaId      identificador de la reserva a marcar como inasistencia
     * @param authentication contexto de seguridad del request
     * @return {@code 200 OK} con confirmación y recursos liberados
     * @throws ResourceNotFoundException si la reserva no existe (404)
     * @throws BusinessException         si la reserva no es CONFIRMADA o no han transcurrido
     *                                   30 minutos (422 UNPROCESSABLE_ENTITY)
     */
    @PatchMapping("/{reservaId}/marcar-inasistencia")
    @PreAuthorize("hasAnyRole('MESERO', 'ADMIN')")
    @Operation(summary = "Marcar reserva como inasistencia tras 30 minutos de tolerancia")
    public ResponseEntity<ApiResponse<MarcarInasistenciaResponse>> marcarInasistencia(
            @PathVariable Long reservaId,
            Authentication authentication) {

        MarcarInasistenciaResponse response = reservaService.marcarInasistencia(reservaId, authentication);
        return ResponseEntity.ok(ApiResponse.ok("Reserva cancelada por inasistencia",response));
    }

    /**
     * Retorna las reservas futuras activas del cliente, ordenadas de la más próxima a la más lejana.
     *
     * <p>Solo se incluyen reservas con estado {@code PENDIENTE} o {@code CONFIRMADA}
     * cuya fecha de llegada sea posterior al momento de la consulta.
     * Si el solicitante tiene rol {@code CLIENTE}, solo puede consultar sus propias reservas.
     *
     * @param emailCliente   correo del cliente a consultar
     * @param authentication contexto de seguridad del request
     * @return lista de reservas futuras ordenada ascendentemente por fecha de llegada;
     *         vacía si no hay reservas futuras
     */
    @GetMapping("/cliente/futuras")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMIN')")
    @Operation(summary = "Obtener resumen de reservas futuras del cliente (dashboard)")
    public ResponseEntity<ApiResponse<List<ReservaDetalleResponse>>> obtenerReservasFuturasCliente(
            @RequestParam String emailCliente,
            Authentication authentication) {

        validarOwnershipCliente(emailCliente, authentication);

        List<ReservaDetalleResponse> response = reservaService.obtenerReservasFuturas(emailCliente);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // TODO: obtener todas las reservas futuras

    /**
     * Retorna las reservas canceladas o devueltas del cliente, ordenadas de la más reciente
     * a la más antigua.
     *
     * <p>Si el solicitante tiene rol {@code CLIENTE}, solo puede consultar sus propias reservas.
     *
     * @param emailCliente   correo del cliente a consultar
     * @param authentication contexto de seguridad del request
     * @return lista de reservas canceladas o devueltas; vacía si no hay ninguna
     */
    @GetMapping("/cliente/canceladas-devueltas")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMIN')")
    @Operation(summary = "Obtener reservas canceladas o devueltas del cliente")
    public ResponseEntity<ApiResponse<List<ReservaDetalleResponse>>> obtenerReservasCanceladasODevueltas(
            @RequestParam String emailCliente,
            Authentication authentication) {

        validarOwnershipCliente(emailCliente, authentication);

        List<ReservaDetalleResponse> response = reservaService.obtenerReservasCanceladasODevueltas(emailCliente);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Retorna el detalle completo de una reserva específica.
     *
     * <p>Si el solicitante tiene rol {@code CLIENTE}, el servicio valida que la reserva
     * pertenezca al cliente autenticado antes de retornar el detalle.
     * Otros roles acceden sin restricción de propiedad.
     *
     * @param reservaId      identificador de la reserva a consultar
     * @param authentication contexto de seguridad del request
     * @return detalle completo de la reserva, incluyendo pre-orden y abonos si aplican
     */
    @GetMapping("/{reservaId}/detalle")
    @PreAuthorize("hasAnyRole('CLIENTE', 'MESERO', 'CAJERO', 'ADMIN')")
    @Operation(summary = "Obtener detalle completo de una reserva del cliente")
    public ResponseEntity<ApiResponse<ReservaDetalleResponse>> obtenerDetalleReserva(
            @PathVariable Long reservaId,
            Authentication authentication) {

        ReservaDetalleResponse response = reservaService.obtenerDetalleReserva(reservaId, emailSiCliente(authentication));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Valida que el cliente autenticado sea el propietario del recurso solicitado.
     *
     * <p>Solo aplica restricción cuando el solicitante tiene rol {@code CLIENTE}. Otros roles
     * pueden acceder a recursos de cualquier cliente sin restricción.
     *
     * @param emailPropietario email del propietario del recurso
     * @param authentication contexto de autenticación del request actual
     * @throws BusinessException con HTTP 403 si el cliente autenticado no es el propietario
     */
    private void validarOwnershipCliente(String emailPropietario, Authentication authentication) {
        if (emailSiCliente(authentication) != null
                && !authentication.getName().equalsIgnoreCase(emailPropietario)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED,
                    "Solo puedes consultar tus propias reservas.", HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Retorna el email del usuario autenticado si tiene rol {@code CLIENTE}; {@code null} en
     * caso contrario. Permite que los endpoints con múltiples roles distingan si deben aplicar
     * restricción de ownership (CLIENTE) o acceso libre (otros roles).
     */
    private String emailSiCliente(Authentication authentication) {
        boolean esCliente = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENTE"));
        return esCliente ? authentication.getName() : null;
    }
}