package co.edu.unicauca.backend.modules.pagos_caja.controller;

import co.edu.unicauca.backend.modules.pagos_caja.dto.response.CuentaPreliminarResponse;
import co.edu.unicauca.backend.modules.pagos_caja.service.CuentaService;
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

/**
 * Controlador REST de la cuenta preliminar para el cierre de venta.
 */
@RestController
@RequestMapping("/api/ventas")
@RequiredArgsConstructor
@Tag(name = "Ventas", description = "Cuenta y cierre de venta del cajero")
public class CuentaController {

    private final CuentaService cuentaService;

    /**
     * Devuelve la cuenta preliminar de una visita: ítems, totales, decoración,
     * abonos, saldo pendiente y puntos del cliente.
     *
     * @param visitaId identificador de la visita
     * @return la cuenta preliminar
     */
    @GetMapping("/{visitaId}/cuenta")
    @PreAuthorize("hasRole('CAJERO')")
    @Operation(summary = "Obtener la cuenta preliminar de una visita")
    public ResponseEntity<ApiResponse<CuentaPreliminarResponse>> obtenerCuenta(
            @PathVariable Long visitaId) {
        CuentaPreliminarResponse data = cuentaService.obtenerCuenta(visitaId);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}
