package co.edu.unicauca.backend.modules.reportes.ventas_detalle.controller;


import co.edu.unicauca.backend.modules.reportes.ventas_detalle.dto.response.DashboardDiarioResponse;
import co.edu.unicauca.backend.modules.reportes.ventas_detalle.dto.response.VentaDetalleResponse;
import co.edu.unicauca.backend.modules.reportes.ventas_detalle.dto.response.VentaListadoResponse;
import co.edu.unicauca.backend.modules.reportes.ventas_detalle.service.VentaDetalleAdminService;
import co.edu.unicauca.backend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar ventas con filtros administrativos")
    public ResponseEntity<ApiResponse<VentaListadoResponse>> listarVentas(
            @RequestParam(value = "ventaId", required = false) String ventaId,
            @RequestParam(value = "desdeFecha", required = false) String desdeFecha,
            @RequestParam(value = "hastaFecha", required = false) String hastaFecha,
            @RequestParam(value = "metodoPago", required = false) String metodoPago) {
        VentaListadoResponse response = detalleService.listarVentas(ventaId, desdeFecha, hastaFecha, metodoPago);
        if (response.getVentas().isEmpty()) {
            String message = generarMensajeSinResultados(ventaId, desdeFecha, hastaFecha, metodoPago);
            return ResponseEntity.ok(ApiResponse.ok(message, response));
        }
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Resumen diario del dashboard administrativo")
    public ResponseEntity<ApiResponse<DashboardDiarioResponse>> obtenerDashboardDiario(
            @RequestParam(value = "fecha", required = false) String fecha) {
        DashboardDiarioResponse response = detalleService.obtenerDashboardDiario(fecha);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    private String generarMensajeSinResultados(String ventaId, String desdeFecha, String hastaFecha, String metodoPago) {
        if (ventaId != null && !ventaId.isBlank()) {
            return "No existe una venta con el identificador ingresado";
        }
        return "No se encontraron ventas con los filtros seleccionados. Intenta ampliar el rango de búsqueda.";
    }
}
