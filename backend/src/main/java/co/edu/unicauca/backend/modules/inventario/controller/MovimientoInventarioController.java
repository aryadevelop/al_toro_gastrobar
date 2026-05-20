package co.edu.unicauca.backend.modules.inventario.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unicauca.backend.modules.inventario.dto.request.AjusteInventarioRequest;
import co.edu.unicauca.backend.modules.inventario.dto.response.AjusteInventarioResponse;
import co.edu.unicauca.backend.modules.inventario.dto.response.ItemAjusteInventarioResponse;
import co.edu.unicauca.backend.modules.inventario.service.MovimientoInventarioService;
import co.edu.unicauca.backend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controlador REST para la gestión de movimientos manuales de inventario.
 *
 * <p>Provee el buscador de productos e insumos para el formulario de ajuste y el
 * registro de movimientos de ingreso o egreso. Accesible para los roles
 * {@code PRODUCCION} y {@code ADMIN}.
 *
 * @see MovimientoInventarioService
 */
@RestController
@RequestMapping("/api/inventario")
@RequiredArgsConstructor
@Tag(name = "Inventario", description = "Ajuste manual de inventario")
public class MovimientoInventarioController {

    private final MovimientoInventarioService movimientoService;

    /**
     * Busca productos "venta_directa" e insumos activos por nombre para el formulario de ajuste.
     *
     * @param q fragmento de nombre a buscar
     * @return lista unificada de productos e insumos, ordenada alfabéticamente
     */
    @GetMapping("/buscar")
    @PreAuthorize("hasAnyRole('PRODUCCION', 'ADMIN')")
    @Operation(summary = "Buscar productos e insumos para ajuste de inventario")
    public ResponseEntity<ApiResponse<List<ItemAjusteInventarioResponse>>> buscarItemsAjuste(
            @Parameter(description = "Fragmento de nombre a buscar")
            @RequestParam("q") String q) {
        return ResponseEntity.ok(ApiResponse.ok(movimientoService.buscarItemsAjuste(q)));
    }

    /**
     * Registra un movimiento de ingreso o egreso manual en el inventario.
     *
     * @param request datos del ajuste
     * @param auth    contexto del usuario autenticado
     * @return identificador del movimiento, stock resultante y comandas notificadas
     */
    @PostMapping("/movimientos")
    @PreAuthorize("hasAnyRole('PRODUCCION', 'ADMIN')")
    @Operation(summary = "Registrar ajuste manual de inventario")
    public ResponseEntity<ApiResponse<AjusteInventarioResponse>> registrarAjuste(
            @Valid @RequestBody AjusteInventarioRequest request,
            Authentication auth) {
        AjusteInventarioResponse response = movimientoService.registrarAjuste(request, auth);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Ajuste de inventario registrado correctamente.", response));
    }
}