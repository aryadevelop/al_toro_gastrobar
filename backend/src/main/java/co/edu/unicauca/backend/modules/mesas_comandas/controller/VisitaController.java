package co.edu.unicauca.backend.modules.mesas_comandas.controller;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.VisitaDetalleResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.VisitaResumenResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.service.VisitaService;
import co.edu.unicauca.backend.shared.dto.ApiResponse;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la consulta del historial y detalle de visitas del cliente.
 *
 * <p>Expone los endpoints bajo {@code /api/visitas} y delega toda la lógica
 * de negocio en {@link VisitaService}.
 *
 * @see VisitaService
 */
@RestController
@RequestMapping("/api/visitas")
@RequiredArgsConstructor
@Tag(name = "Visitas", description = "Historial y detalle de visitas del cliente")
public class VisitaController {

    private final VisitaService visitaService;

    /**
     * Retorna el historial de visitas de un cliente, ordenadas de la más reciente a la más antigua.
     *
     * <p>Si el solicitante tiene rol {@code CLIENTE}, solo puede consultar su propio historial
     * (el {@code emailCliente} del parámetro debe coincidir con el email autenticado).
     * El rol {@code ADM} puede consultar el historial de cualquier cliente sin restricción.
     *
     * @param emailCliente   correo del cliente cuyo historial se solicita
     * @param authentication contexto de seguridad del request
     * @return lista de visitas históricas ordenada descendentemente por fecha; vacía si no hay historial
     */
    @GetMapping("/cliente/historial")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMIN')")
    @Operation(summary = "Obtener historial de visitas pasadas del cliente")
    public ResponseEntity<ApiResponse<List<VisitaResumenResponse>>> obtenerHistorialVisitas(
            @RequestParam String emailCliente,
            Authentication authentication) {

        boolean esCliente = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENTE"));

        if (esCliente && !emailCliente.equalsIgnoreCase(authentication.getName())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED,
                    "Solo puedes consultar tu propio historial de visitas.", HttpStatus.FORBIDDEN);
        }

        List<VisitaResumenResponse> response = visitaService.obtenerHistorialVisitas(emailCliente);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Retorna el detalle completo de una visita específica.
     *
     * <p>Si el solicitante tiene rol {@code CLIENTE}, el servicio valida que la visita
     * pertenezca al cliente autenticado antes de retornar el detalle.
     * Otros roles (CAJERO, MESERO, ADM) acceden sin restricción de propiedad.
     *
     * @param visitaId       identificador de la visita
     * @param authentication contexto de seguridad del request
     * @return detalle completo de la visita
     */
    @GetMapping("/cliente/{visitaId}/detalle")
    @PreAuthorize("hasAnyRole('CLIENTE', 'CAJERO', 'MESERO', 'ADMIN')")
    @Operation(summary = "Obtener detalle completo de una visita")
    public ResponseEntity<ApiResponse<VisitaDetalleResponse>> obtenerDetalleVisita(
            @PathVariable Long visitaId,
            Authentication authentication) {

        boolean esCliente = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENTE"));

        String emailAutenticado = esCliente ? authentication.getName() : null;

        VisitaDetalleResponse response = visitaService.obtenerDetalleVisita(visitaId, emailAutenticado);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
