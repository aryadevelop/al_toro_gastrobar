package co.edu.unicauca.backend.modules.reservas.controller;

import co.edu.unicauca.backend.modules.reservas.dto.request.CrearReservaRequest;
import co.edu.unicauca.backend.modules.reservas.dto.response.DisponibilidadResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.PreOrdenDetalleResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ReservaResponse;
import co.edu.unicauca.backend.modules.reservas.service.ReservaService;
import co.edu.unicauca.backend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;


/**
 * Controlador REST para la gestión de reservas de clientes.
 *
 * <p>Expone los endpoints bajo {@code /api/reservas} y delega toda la lógica
 * de negocio en {@link ReservaService}.
 *
 * <p>Comportamiento general:
 * <ul>
 *   <li><b>Disponibilidad:</b> verifica zonas y decoraciones libres para una
 *       fecha/hora; respeta el horario de atención y los bloqueos del administrador.</li>
 *   <li><b>Creación:</b> registra una reserva para el cliente autenticado,
 *       incluyendo opcionalmente una pre-orden de platos.</li>
 *   <li><b>Pre-orden:</b> permite a CAJERO, MESERO y ADMIN consultar los ítems
 *       anticipados por el cliente para pre-cargar la comanda.</li>
 *   <li><b>Historial:</b> CLIENTE y ADMIN visualizan todas las reservas del sistema.</li>
 * </ul>
 *
 * @see ReservaService
 */
@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
@Tag(name = "Reservas", description = "Gestión de reservas de clientes")
public class ReservaController {

    /** Servicio que contiene la lógica de negocio de reservas. */
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
     * <p>Solo accesible por usuarios con rol {@code CLIENTE}.
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
     * Crea una nueva reserva para el cliente autenticado.
     *
     * <p>El cliente se identifica a partir del {@link UserDetails} del contexto de seguridad,
     * por lo que no es necesario enviar el identificador de usuario en el cuerpo de la petición.
     *
     * <p>Solo accesible por usuarios con rol {@code CLIENTE}.
     *
     * @param request    datos de la reserva a crear (fecha, zona, decoración y pre-orden opcional)
     * @param userDetails principal autenticado del que se extrae el nombre de usuario
     * @return respuesta {@code 201 Created} con el detalle de la reserva registrada
     */
    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Crear nueva reserva")
    public ResponseEntity<ApiResponse<ReservaResponse>> crearReserva(
            @Valid @RequestBody CrearReservaRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        ReservaResponse response = reservaService.crearReserva(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Reserva creada exitosamente", response));
    }

    /**
     * Retorna el detalle completo de la pre-orden asociada a una reserva.
     *
     * <p>Permite a CAJERO, MESERO y ADMIN consultar los platos anticipados por
     * el cliente al momento de reservar, facilitando la pre-carga de la comanda
     * cuando el cliente llega al establecimiento.
     *
     * @param id identificador de la reserva cuya pre-orden se desea consultar
     * @return lista de ítems de la pre-orden con cantidades y detalles de cada plato
     */
    @GetMapping("/{id}/preorden")
    @PreAuthorize("hasAnyRole('CAJERO', 'MESERO', 'ADMIN')")
    @Operation(summary = "Obtener pre-orden de una reserva")
    public ResponseEntity<ApiResponse<List<PreOrdenDetalleResponse>>> obtenerPreOrden(
            @PathVariable Long id) {

        List<PreOrdenDetalleResponse> response = reservaService.obtenerPreOrden(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Retorna el historial de reservas del cliente autenticado.
     *
     * <p>El cliente se identifica a partir del {@link UserDetails} del contexto de seguridad;
     * el ADMIN puede consultar el historial de cualquier cliente pasando su nombre de usuario.
     *
     * <p>Solo accesible por usuarios con rol {@code CLIENTE} o {@code ADMIN}.
     *
     * @param userDetails principal autenticado del que se extrae el nombre de usuario
     * @return lista de reservas del cliente autenticado
     */
    @GetMapping("/cliente")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMIN')")
    @Operation(summary = "Obtener historial de reservas del cliente")
    public ResponseEntity<ApiResponse<List<ReservaResponse>>> obtenerHistorialCliente(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<ReservaResponse> response = reservaService.obtenerReservasCliente(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
