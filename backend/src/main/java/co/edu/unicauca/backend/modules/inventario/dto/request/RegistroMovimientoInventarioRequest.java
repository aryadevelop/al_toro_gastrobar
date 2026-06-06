package co.edu.unicauca.backend.modules.inventario.dto.request;

import co.edu.unicauca.backend.shared.enums.TipoMovimiento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request para registrar un movimiento de inventario con fecha opcional.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistroMovimientoInventarioRequest {

    /** Identificador del producto afectado; nulo si el movimiento es sobre un insumo. */
    private Long productoId;

    /** Identificador del insumo afectado; nulo si el movimiento es sobre un producto. */
    private Long insumoId;

    /** Cantidad del movimiento; debe ser mayor a {@code 0.000}. */
    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.001", message = "La cantidad debe ser mayor a 0")
    @Digits(integer = 9, fraction = 3, message = "La cantidad debe tener máximo 9 dígitos enteros y 3 decimales")
    private BigDecimal cantidad;

    /** Dirección del movimiento: {@code INGRESO} o {@code EGRESO}. */
    @NotNull(message = "El tipo de movimiento es obligatorio")
    private TipoMovimiento tipo;

    /** Nombre del proveedor; aplica solo en ingresos por compra. */
    @Size(max = 150, message = "El proveedor no debe exceder 150 caracteres")
    private String proveedor;

    /** Número de factura del proveedor; aplica solo en ingresos por compra. */
    @Size(max = 150, message = "El número de factura no debe exceder 150 caracteres")
    private String numeroFactura;

    /** Observaciones adicionales sobre el movimiento. */
    private String observaciones;

    /** Fecha y hora del movimiento; opcional, por defecto se usa fecha/hora del servidor. */
    private LocalDateTime fecha;
}
