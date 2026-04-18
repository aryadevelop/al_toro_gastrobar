package co.edu.unicauca.backend.modules.pagos_caja.controller;

import co.edu.unicauca.backend.modules.pagos_caja.dto.request.CerrarCuentaRequest;
import co.edu.unicauca.backend.modules.pagos_caja.service.VentaService;
import co.edu.unicauca.backend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para el registro de ventas y cierre de cuentas de visitas.
 *
 * <p>Expone los endpoints bajo {@code /api/ventas} y delega toda la lógica
 * de negocio en {@link VentaService}.
 *
 * @see VentaService
 */
@RestController
@RequestMapping("/api/ventas")
@RequiredArgsConstructor
@Tag(name = "Ventas", description = "Registro de ventas y cierre de cuentas de visitas")
public class VentaController {

    private final VentaService ventaService;

    /**
     * Registra la venta final de una visita y cierra su cuenta.
     *
     * @param request     datos de la venta: identificador de visita, subtotal,
     *                    descuento, total y método de pago
     * @param userDetails principal autenticado del que se extrae el correo del cajero
     * @return respuesta {@code 201 Created} con mensaje de confirmación
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('CAJERO')")
    @Operation(summary = "Registrar venta y cerrar cuenta de una visita")
    public ResponseEntity<ApiResponse<Void>> cerrarCuenta(
            @Valid @RequestBody CerrarCuentaRequest request) {

        ventaService.cerrarCuenta(request, request.getEmailCajero());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.message("Cuenta cerrada exitosamente. Los puntos del cliente han sido actualizados."));
    }
}
