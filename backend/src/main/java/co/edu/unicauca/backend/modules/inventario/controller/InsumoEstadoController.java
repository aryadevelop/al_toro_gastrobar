package co.edu.unicauca.backend.modules.inventario.controller;

import co.edu.unicauca.backend.modules.inventario.dto.request.CambioEstadoRequest;
import co.edu.unicauca.backend.modules.inventario.dto.response.CambioEstadoImplicacionesResponse;
import co.edu.unicauca.backend.modules.inventario.dto.response.CambioEstadoResponse;
import co.edu.unicauca.backend.modules.inventario.service.EstadoInventarioService;
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

/**
 * Controlador REST para cambiar el estado de insumos en inventario.
 */
@RestController
@RequestMapping("/api/inventario/insumos")
@RequiredArgsConstructor
@Tag(name = "Insumos", description = "Gestión de estado de insumos")
public class InsumoEstadoController {

    private final EstadoInventarioService estadoInventarioService;

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
