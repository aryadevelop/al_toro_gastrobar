package co.edu.unicauca.backend.modules.mesas_comandas.controller;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ComandaProduccionDetalleResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ComandaProduccionResumenResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.TableroProduccionResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.service.ComandaProduccionService;
import co.edu.unicauca.backend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST que expone las pantallas de producción de comandas para
 * los roles de cocina y barra.
 *
 * <p>Todos los endpoints requieren rol funcional {@code PRODUCCION} y derivan
 * la(s) estación(es) accesibles a partir del usuario autenticado mediante
 * {@link co.edu.unicauca.backend.modules.mesas_comandas.service.EstacionResolver}.
 */
@RestController
@RequestMapping("/api/comandas/produccion")
@RequiredArgsConstructor
@Tag(name = "Comandas - Producción",
     description = "Vista en tiempo real de las comandas asignadas a la estación del usuario")
public class ComandaProduccionController {

    private final ComandaProduccionService comandaProduccionService;

    /**
     * Devuelve el tablero de comandas de la(s) estación(es) del usuario,
     * separadas en tres columnas según su estado.
     *
     * @param auth contexto del usuario autenticado
     * @return tablero con las columnas {@code pendientes},
     *         {@code enPreparacion} y {@code listos}
     */
    @GetMapping
    @PreAuthorize("hasRole('PRODUCCION')")
    @Operation(summary = "Tablero de producción",
            description = "Lista las comandas pendientes, en preparación y listas para la estación del usuario")
    public ResponseEntity<ApiResponse<TableroProduccionResponse>> obtenerTablero(Authentication auth) {
        TableroProduccionResponse data = comandaProduccionService.obtenerTableroProduccion(auth);
        return ResponseEntity.ok(ApiResponse.ok("Tablero de producción obtenido exitosamente", data));
    }

    /**
     * Devuelve el detalle de una comanda visible para la estación del usuario.
     *
     * @param comandaId identificador de la comanda
     * @param auth      contexto del usuario autenticado
     * @return detalle con los ítems agrupados por categoría
     */
    @GetMapping("/{comandaId}")
    @PreAuthorize("hasRole('PRODUCCION')")
    @Operation(summary = "Detalle de comanda en producción",
            description = "Carga la comanda completa con ítems agrupados por categoría")
    public ResponseEntity<ApiResponse<ComandaProduccionDetalleResponse>> obtenerDetalle(
            @Parameter(description = "Identificador de la comanda") @PathVariable Long comandaId,
            Authentication auth) {
        ComandaProduccionDetalleResponse data = comandaProduccionService.obtenerDetalleComanda(comandaId, auth);
        return ResponseEntity.ok(ApiResponse.ok("Detalle de comanda obtenido exitosamente", data));
    }

    /**
     * Transiciona la comanda indicada de {@code PENDIENTE} a
     * {@code EN_PREPARACION}, ejecutando el descuento de inventario asociado.
     *
     * @param comandaId identificador de la comanda
     * @param auth      contexto del usuario autenticado
     * @return resumen actualizado de la comanda
     */
    @PostMapping("/{comandaId}/iniciar")
    @PreAuthorize("hasRole('PRODUCCION')")
    @Operation(summary = "Iniciar preparación de una comanda",
            description = "Cambia el estado de la comanda a EN_PREPARACION y descuenta el inventario consumido")
    public ResponseEntity<ApiResponse<ComandaProduccionResumenResponse>> iniciar(
            @Parameter(description = "Identificador de la comanda") @PathVariable Long comandaId,
            Authentication auth) {
        ComandaProduccionResumenResponse data = comandaProduccionService.iniciarPreparacion(comandaId, auth);
        return ResponseEntity.ok(ApiResponse.ok("Comanda iniciada exitosamente", data));
    }

    /**
     * Marca la comanda indicada como {@code LISTO} y crea la notificación al
     * mesero según la estación.
     *
     * @param comandaId identificador de la comanda
     * @param auth      contexto del usuario autenticado
     * @return resumen actualizado de la comanda
     */
    @PostMapping("/{comandaId}/listo")
    @PreAuthorize("hasRole('PRODUCCION')")
    @Operation(summary = "Marcar comanda como lista",
            description = "Cambia el estado de la comanda a LISTO y crea la notificación PLATOS_LISTOS o BEBIDAS_LISTAS según la estación")
    public ResponseEntity<ApiResponse<ComandaProduccionResumenResponse>> marcarListo(
            @Parameter(description = "Identificador de la comanda") @PathVariable Long comandaId,
            Authentication auth) {
        ComandaProduccionResumenResponse data = comandaProduccionService.marcarListo(comandaId, auth);
        return ResponseEntity.ok(ApiResponse.ok("Comanda marcada como lista", data));
    }
}
