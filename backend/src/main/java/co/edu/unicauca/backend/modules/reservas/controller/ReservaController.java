package co.edu.unicauca.backend.modules.reservas.controller;

import co.edu.unicauca.backend.modules.reservas.dto.request.CrearReservaRequest;
import co.edu.unicauca.backend.modules.reservas.dto.response.DisponibilidadResponse;
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


@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
@Tag(name = "Reservas", description = "Gestión de reservas de clientes")
public class ReservaController {

    private final ReservaService reservaService;

    /**
     * Consulta disponibilidad y retorna decoraciones y zonas libres para una fecha/hora dada.
     * Retorna {@code disponible=false} si la hora está fuera del horario de atención
     * (lunes–domingo 5 PM–10 PM) o si existe un bloqueo activo del administrador.
     * Solo accesible por usuarios con rol CLIENTE.
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
     * Solo accesible por usuarios con rol CLIENTE.
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
     * Retorna el historial de reservas.
     * - CLIENTE: sus propias reservas.
     * - CAJERO, ADM, MESERO: todas las reservas del sistema.
     */
    @GetMapping("/cliente")
    @PreAuthorize("hasAnyRole('CLIENTE', 'CAJERO', 'ADMIN', 'MESERO')")
    @Operation(summary = "Obtener historial de reservas")
    public ResponseEntity<ApiResponse<List<ReservaResponse>>> obtenerHistorial(
            @AuthenticationPrincipal UserDetails userDetails) {

        boolean esCliente = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENTE"));

        List<ReservaResponse> response = esCliente
                ? reservaService.obtenerReservasCliente(userDetails.getUsername())
                : reservaService.obtenerTodasLasReservas();

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
