package co.edu.unicauca.backend.modules.inventario.entity;

import co.edu.unicauca.backend.modules.produccion.entity.Producto;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.shared.enums.TipoMovimiento;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad MovimientoInventario - Historial de movimientos de inventario (ingresos y egresos)
 */
@Entity
@Table(name = "movimiento_inventario", schema = "restaurante",
       indexes = {
           @Index(name = "idx_movimiento_empleado_id", columnList = "empleado_id"),
           @Index(name = "idx_movimiento_producto_id", columnList = "producto_id"),
           @Index(name = "idx_movimiento_insumo_id", columnList = "insumo_id"),
           @Index(name = "idx_movimiento_fecha_hora", columnList = "movimiento_fecha_hora"),
           @Index(name = "idx_movimiento_tipo", columnList = "movimiento_tipo")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "movimiento_id")
    private Long movimientoId;

    @NotNull(message = "El empleado es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_movimiento_empleado"))
    private Empleado empleado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", foreignKey = @ForeignKey(name = "fk_movimiento_producto"))
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insumo_id", foreignKey = @ForeignKey(name = "fk_movimiento_insumo"))
    private Insumo insumo;

    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.001", message = "La cantidad debe ser mayor a 0")
    @Digits(integer = 9, fraction = 3, message = "La cantidad debe tener máximo 9 dígitos enteros y 3 decimales")
    @Column(name = "movimiento_cantidad", nullable = false, precision = 12, scale = 3)
    private BigDecimal movimientoCantidad;

    @NotNull(message = "El tipo de movimiento es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "movimiento_tipo", nullable = false, length = 20)
    private TipoMovimiento movimientoTipo;

    @Size(max = 150, message = "El proveedor no debe exceder 150 caracteres")
    @Column(name = "movimiento_proveedor", length = 150)
    private String movimientoProveedor;

    @Size(max = 150, message = "El número de factura no debe exceder 150 caracteres")
    @Column(name = "movimiento_numero_factura", length = 150)
    private String movimientoNumeroFactura;

    @Column(name = "movimiento_observaciones", columnDefinition = "TEXT")
    private String movimientoObservaciones;

    @NotNull
    @Column(name = "movimiento_fecha_hora", nullable = false)
    @Builder.Default
    private LocalDateTime movimientoFechaHora = LocalDateTime.now();

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (movimientoFechaHora == null) {
            movimientoFechaHora = LocalDateTime.now();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MovimientoInventario)) return false;
        MovimientoInventario that = (MovimientoInventario) o;
        return movimientoId != null && movimientoId.equals(that.getMovimientoId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
