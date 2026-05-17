package co.edu.unicauca.backend.modules.reportes_ventas_detalle.controller;

import co.edu.unicauca.backend.modules.reportes_ventas_detalle.dto.response.VentaDetalleResponse;
import co.edu.unicauca.backend.modules.reportes_ventas_detalle.service.VentaDetalleAdminService;
import co.edu.unicauca.backend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ventas")
@RequiredArgsConstructor
@Tag(name = "Detalle de ventas", description = "Consulta detallada de ventas para administracion")
public class VentaDetalleAdminController {

    private final VentaDetalleAdminService detalleService;

    @GetMapping("/{visitaId}/detalle")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Detalle completo de una venta")
    public ResponseEntity<ApiResponse<VentaDetalleResponse>> obtenerDetalle(
            @PathVariable Long visitaId) {
        return ResponseEntity.ok(ApiResponse.ok(detalleService.obtenerDetalle(visitaId)));
    }
}
