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
     * Retorna el historial de reservas del cliente autenticado.
     * Solo accesible por usuarios con rol CLIENTE.
     */
    @GetMapping("/cliente")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Obtener reservas del cliente autenticado")
    public ResponseEntity<ApiResponse<List<ReservaResponse>>> obtenerMisReservas(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<ReservaResponse> response = reservaService.obtenerReservasCliente(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
