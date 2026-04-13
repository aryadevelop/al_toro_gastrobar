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
     * Endpoint público: no requiere autenticación para facilitar la precarga del formulario.
     */
    @GetMapping("/disponibilidad")
    @Operation(summary = "Consultar disponibilidad para una fecha y hora")
    public ResponseEntity<ApiResponse<DisponibilidadResponse>> consultarDisponibilidad(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHora) {

        DisponibilidadResponse response = reservaService.consultarDisponibilidad(fechaHora);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Crea una nueva reserva.
     *
     * <p><b>DEV:</b> mientras el módulo de auth no está implementado, el email se toma
     * del campo {@code emailCliente} del body. Cuando auth esté listo, eliminar ese campo
     * y usar únicamente {@code userDetails.getUsername()}.
     */
    @PostMapping
    @Operation(summary = "Crear nueva reserva")
    public ResponseEntity<ApiResponse<ReservaResponse>> crearReserva(
            @Valid @RequestBody CrearReservaRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        // DEV: fallback al campo del body cuando no hay token JWT
        String email = (userDetails != null) ? userDetails.getUsername() : request.getEmailCliente();
        ReservaResponse response = reservaService.crearReserva(email, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Reserva creada exitosamente", response));
    }

    /**
     * Retorna el historial de reservas del cliente.
     *
     * <p><b>DEV:</b> mientras el módulo de auth no está implementado, el email se toma
     * del query param {@code emailCliente}. Cuando auth esté listo, usar únicamente
     * {@code userDetails.getUsername()} y eliminar el parámetro.
     */
    @GetMapping("/cliente")
    @Operation(summary = "Obtener reservas del cliente autenticado")
    public ResponseEntity<ApiResponse<List<ReservaResponse>>> obtenerMisReservas(
            @RequestParam(required = false) String emailCliente,
            @AuthenticationPrincipal UserDetails userDetails) {

        // DEV: fallback al query param cuando no hay token JWT
        String email = (userDetails != null) ? userDetails.getUsername() : emailCliente;
        List<ReservaResponse> response = reservaService.obtenerReservasCliente(email);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
