package co.edu.unicauca.backend.modules.notificaciones.controller;

import co.edu.unicauca.backend.modules.notificaciones.service.NotificacionService;
import co.edu.unicauca.backend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para operaciones sobre notificaciones de mesa.
 *
 * @see NotificacionService
 */
@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
@Tag(name = "Notificaciones", description = "Gestión de notificaciones de mesa para empleados")
public class NotificacionController {

    private final NotificacionService notificacionService;

    /**
     * Marca una solicitud de asistencia como atendida.
     *
     * <p>El mesero llama a este endpoint desde el mapa de mesas.
     * Al completar, el cliente recibe un evento WS que re-habilita el botón
     * "Solicitar asistencia" en su dashboard.
     *
     * @param notificacionId identificador de la notificación a atender
     * @param authentication contexto de seguridad del request
     * @return 200 OK con mensaje de confirmación
     */
    @PatchMapping("/{notificacionId}/atender")
    @PreAuthorize("hasAnyRole('MESERO', 'ADMIN')")
    @Operation(summary = "Marcar solicitud de asistencia como atendida")
    public ResponseEntity<ApiResponse<Void>> atenderAsistencia(
            @PathVariable Long notificacionId,
            Authentication authentication) {

        notificacionService.atenderAsistencia(notificacionId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.message("Asistencia atendida."));
    }
}
