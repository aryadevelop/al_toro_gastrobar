package co.edu.unicauca.backend.modules.mesas_comandas.controller;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.MapaMesasResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.MesaDetalleResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.MesaItemsProduccionResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.service.MesaService;
import co.edu.unicauca.backend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST para gestión del mapa de mesas.
 *
 */
@RestController
@RequestMapping("/api/mesas")
@RequiredArgsConstructor
@Tag(name = "Mesas", description = "API de mapa de mesas para meseros")
public class MesaController {

    private final MesaService mesaService;

    /**
     * Obtiene el mapa de mesas del restaurante.
     *
     * <p>Sin parámetro zonaId: devuelve todas las zonas con sus mesas activas.
     * Con parámetro zonaId: filtra solo las mesas de esa zona.
     *
     * <p>Las mesas propias del mesero se identifican con esMesaPropia=true.
     *
     * @param zonaId ID de zona (opcional)
     * @param authentication autenticación del mesero
     * @return MapaMesasResponse con zonas y mesas
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('MESERO', 'ADMIN')")
    @Operation(summary = "Obtener mapa de mesas",
               description = "Lista todas las zonas con sus mesas activas. Puede filtrarse por zona.")
    public ResponseEntity<ApiResponse<MapaMesasResponse>> obtenerMapaMesas(
            @Parameter(description = "ID de zona (null = todas las zonas)")
            @RequestParam(required = false) Long zonaId,
            Authentication authentication) {

        String emailMesero = authentication.getName();
        MapaMesasResponse mapa = mesaService.obtenerMapaMesas(zonaId, emailMesero);

        return ResponseEntity.ok(ApiResponse.ok("Mapa de mesas obtenido exitosamente", mapa));
    }

    /**
     * Obtiene el detalle completo de una mesa.
     *
     * @param mesaId ID de la mesa (visita_id)
     * @return MesaDetalleResponse con información completa
     */
    @GetMapping("/{mesaId}/detalle")
    @PreAuthorize("hasAnyRole('MESERO', 'ADMIN')")
    @Operation(summary = "Obtener detalle de mesa",
               description = "Devuelve información detallada de una mesa específica")
    public ResponseEntity<ApiResponse<MesaDetalleResponse>> obtenerDetalleMesa(
            @Parameter(description = "ID de la mesa (visita_id)")
            @PathVariable Long mesaId) {

        MesaDetalleResponse detalle = mesaService.obtenerDetalleMesa(mesaId);

        return ResponseEntity.ok(ApiResponse.ok("Detalle de mesa obtenido exitosamente", detalle));
    }

    /**
     * Obtiene información de items en producción de una mesa.
     *
     * <p><b>IMPORTANTE:</b> Solo el mesero asignado a la mesa puede acceder.
     * El rol ADMIN tiene acceso sin restricciones.
     *
     * @param mesaId ID de la mesa (visita_id)
     * @param authentication autenticación del usuario
     * @return MesaItemsProduccionResponse con identificador y items en producción
     */
    @GetMapping("/{mesaId}/items-produccion")
    @PreAuthorize("hasAnyRole('MESERO', 'ADMIN')")
    @Operation(summary = "Obtener items en producción",
               description = "Devuelve el resumen de items en producción de una mesa. Solo el mesero asignado o ADMIN pueden acceder.")
    public ResponseEntity<ApiResponse<MesaItemsProduccionResponse>> obtenerItemsProduccion(
            @Parameter(description = "ID de la mesa (visita_id)")
            @PathVariable Long mesaId,
            Authentication authentication) {

        MesaItemsProduccionResponse info = mesaService.obtenerItemsProduccion(mesaId, authentication);

        return ResponseEntity.ok(ApiResponse.ok(
                "Items en producción obtenidos exitosamente", info));
    }
}
