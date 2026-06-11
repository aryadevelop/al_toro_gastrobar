package co.edu.unicauca.backend.modules.inventario.controller;

import co.edu.unicauca.backend.modules.inventario.dto.request.ActualizarInsumoRequest;
import co.edu.unicauca.backend.modules.inventario.dto.request.CambioEstadoRequest;
import co.edu.unicauca.backend.modules.inventario.dto.response.CambioEstadoImplicacionesResponse;
import co.edu.unicauca.backend.modules.inventario.dto.response.CambioEstadoResponse;
import co.edu.unicauca.backend.modules.inventario.dto.response.InsumoAdminListResponse;
import co.edu.unicauca.backend.modules.inventario.dto.response.InsumoDetalleResponse;
import co.edu.unicauca.backend.modules.inventario.service.EstadoInventarioService;
import co.edu.unicauca.backend.modules.inventario.service.InsumoAdminService;
import co.edu.unicauca.backend.shared.dto.ApiResponse;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST para la gestión de insumos en inventario (admin).
 *
 * <p>Expone operaciones de listado, detalle, actualización y cambio de estado
 * accesibles únicamente para el rol {@code ADMIN}.
 */
@RestController
@RequestMapping("/api/inventario/insumos")
@RequiredArgsConstructor
@Tag(name = "Insumos", description = "Gestión de insumos – administrador")
public class InsumoEstadoController {

    private final EstadoInventarioService estadoInventarioService;
    private final InsumoAdminService insumoAdminService;

    // ── Listado y detalle ────────────────────────────────────────────────────

    /**
     * Lista todos los insumos con su stock, unidad, estado y alerta de vencimiento.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar insumos del inventario")
    public ResponseEntity<ApiResponse<List<InsumoAdminListResponse>>> listarInsumos() {
        return ResponseEntity.ok(ApiResponse.ok(insumoAdminService.listarInsumos()));
    }

    /**
     * Retorna el detalle completo de un insumo, incluyendo costo unitario y fecha de
     * vencimiento. El campo {@code vencimientoProximo} indica si la fecha de
     * vencimiento está dentro de los próximos 7 días.
     */
    @GetMapping("/{insumoId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtener detalle de un insumo")
    public ResponseEntity<ApiResponse<InsumoDetalleResponse>> obtenerDetalle(
            @PathVariable("insumoId") Long insumoId) {
        return ResponseEntity.ok(ApiResponse.ok(insumoAdminService.obtenerDetalle(insumoId)));
    }

    /**
     * Actualiza el nombre, costo unitario y fecha de vencimiento de un insumo.
     */
    @PutMapping("/{insumoId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar datos de un insumo")
    public ResponseEntity<ApiResponse<InsumoDetalleResponse>> actualizarInsumo(
            @PathVariable("insumoId") Long insumoId,
            @Valid @RequestBody ActualizarInsumoRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(insumoAdminService.actualizarInsumo(insumoId, request)));
    }

    // ── Cambio de estado ─────────────────────────────────────────────────────

    @GetMapping("/{insumoId}/estado/implicaciones")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Evaluar implicaciones del cambio de estado de un insumo")
    public ResponseEntity<ApiResponse<CambioEstadoImplicacionesResponse>> evaluarCambioEstado(
            @PathVariable("insumoId") Long insumoId,
            @Parameter(description = "Estado al que se desea cambiar el insumo")
            @RequestParam("estado") EstadoGenerico estado) {
        CambioEstadoImplicacionesResponse response = estadoInventarioService
                .evaluarCambioEstadoInsumo(insumoId, estado);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/{insumoId}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cambiar el estado de un insumo")
    public ResponseEntity<ApiResponse<CambioEstadoResponse>> cambiarEstado(
            @PathVariable("insumoId") Long insumoId,
            @Valid @RequestBody CambioEstadoRequest request) {
        CambioEstadoResponse response = estadoInventarioService.cambiarEstadoInsumo(insumoId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
