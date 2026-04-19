package co.edu.unicauca.backend.modules.reservas.controller;

import co.edu.unicauca.backend.modules.reservas.dto.request.CrearReservaRequest;
import co.edu.unicauca.backend.modules.reservas.dto.request.ModificarReservaRequest;
import co.edu.unicauca.backend.modules.reservas.dto.response.DisponibilidadResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ModificarReservaResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ReservaDetalleResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ReservaResponse;
import co.edu.unicauca.backend.modules.reservas.service.ReservaService;
import co.edu.unicauca.backend.shared.dto.ApiResponse;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
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
 * <p><b>Regla de ownership:</b> Los endpoints que operan sobre datos de un cliente específico
 * aplican la siguiente lógica: si el solicitante tiene rol {@code CLIENTE}, solo puede acceder
 * a sus propios datos (el email/ID se toma del token, no del body o query param). Otros roles
 * ({@code CAJERO}, {@code ADM}, etc.) pueden acceder a datos de cualquier cliente. Esta lógica
 * está codificada aunque el endpoint hoy solo permita {@code CLIENTE}, para que cuando se amplíe
 * el {@code @PreAuthorize} en el futuro, el acceso multi-cliente funcione sin cambios adicionales.
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
     * Otros roles pueden especificar el email en el body
     * para crear reservas a nombre de otro cliente.
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

        boolean esCliente = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENTE"));
        String emailEfectivo = esCliente ? authentication.getName() : request.getEmailCliente();

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
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Modificar una reserva futura del cliente")
    public ResponseEntity<ApiResponse<ModificarReservaResponse>> modificarReserva(
            @PathVariable Long reservaId,
            @Valid @RequestBody ModificarReservaRequest request,
            Authentication authentication) {

        String emailCliente = authentication.getName();
        ModificarReservaResponse response = reservaService.modificarReserva(reservaId, emailCliente, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // TODO: cancelar reservas futuras

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

        boolean esCliente = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENTE"));
        if (esCliente && !emailCliente.equalsIgnoreCase(authentication.getName())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED,
                    "Solo puedes consultar tus propias reservas.", HttpStatus.FORBIDDEN);
        }

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

        boolean esCliente = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENTE"));
        if (esCliente && !emailCliente.equalsIgnoreCase(authentication.getName())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED,
                    "Solo puedes consultar tus propias reservas.", HttpStatus.FORBIDDEN);
        }

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

        boolean esCliente = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENTE"));
        String emailAutenticado = esCliente ? authentication.getName() : null;

        ReservaDetalleResponse response = reservaService.obtenerDetalleReserva(reservaId, emailAutenticado);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
    
}