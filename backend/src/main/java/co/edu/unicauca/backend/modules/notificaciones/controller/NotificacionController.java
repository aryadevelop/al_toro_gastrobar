package co.edu.unicauca.backend.modules.notificaciones.controller;

import co.edu.unicauca.backend.modules.notificaciones.dto.response.AtenderCambioResponse;
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

    /**
     * Marca como atendida una notificación de tipo {@code PLATOS_LISTOS}.
     *
     * <p>Confirma que el mesero recogió los platos listos en cocina y los
     * entregó a la mesa. Puede disparar una actualización del estado de mesa vía WS.
     *
     * @param notificacionId identificador de la notificación a atender
     * @param authentication contexto de seguridad del request
     * @return 200 OK con mensaje de confirmación
     */
    @PatchMapping("/{notificacionId}/servir-platos")
    @PreAuthorize("hasAnyRole('MESERO', 'ADMIN')")
    @Operation(summary = "Marcar notificación de platos listos como atendida")
    public ResponseEntity<ApiResponse<Void>> servirPlatos(
            @PathVariable Long notificacionId,
            Authentication authentication) {

        notificacionService.servirPlatos(notificacionId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.message("Platos servidos."));
    }

    /**
     * Marca como atendida una notificación de tipo {@code BEBIDAS_LISTAS}.
     *
     * @param notificacionId identificador de la notificación a atender
     * @param authentication contexto de seguridad del request
     * @return 200 OK con mensaje de confirmación
     */
    @PatchMapping("/{notificacionId}/servir-bebidas")
    @PreAuthorize("hasAnyRole('MESERO', 'ADMIN')")
    @Operation(summary = "Marcar notificación de bebidas listas como atendida")
    public ResponseEntity<ApiResponse<Void>> servirBebidas(
            @PathVariable Long notificacionId,
            Authentication authentication) {

        notificacionService.servirBebidas(notificacionId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.message("Bebidas servidas."));
    }

    /**
     * Atiende una notificación de tipo {@code CAMBIO} y devuelve la comanda
     * lista para ser editada por el mesero.
     *
     * <p>El mesero debe cargar la comanda devuelta en modo edición para
     * aplicar los cambios solicitados por el cliente.
     *
     * @param notificacionId identificador de la notificación a atender
     * @param authentication contexto de seguridad del request
     * @return 200 OK con {@link AtenderCambioResponse} que incluye el {@code comandaId}
     */
    @PatchMapping("/{notificacionId}/atender-cambio")
    @PreAuthorize("hasAnyRole('MESERO', 'ADMIN')")
    @Operation(summary = "Atender solicitud de cambio de comanda")
    public ResponseEntity<ApiResponse<AtenderCambioResponse>> atenderCambio(
            @PathVariable Long notificacionId,
            Authentication authentication) {

        AtenderCambioResponse response = notificacionService.atenderCambio(notificacionId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok("Comanda lista para modificar.", response));
    }
}
