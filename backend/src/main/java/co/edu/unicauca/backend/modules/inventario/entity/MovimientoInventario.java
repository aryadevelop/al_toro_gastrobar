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
 * Registro inmutable de un ingreso o egreso en el inventario del restaurante.
 *
 * <p>Cada movimiento está vinculado a un empleado responsable y a un único objetivo:
 * un {@link Insumo} o un {@link Producto}, nunca ambos al mismo tiempo.
 * Los campos {@code movimientoProveedor} y {@code movimientoNumeroFactura} aplican
 * únicamente a movimientos de tipo {@code INGRESO} provenientes de compras externas.
 *
 * <p>Índices: {@code empleado_id}, {@code producto_id}, {@code insumo_id},
 * {@code movimiento_fecha_hora}, {@code movimiento_tipo}.
 *
 * @see co.edu.unicauca.backend.shared.enums.TipoMovimiento
 * @see Insumo
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

    /** Identificador único generado por la base de datos. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "movimiento_id")
    private Long movimientoId;

    /** Empleado que registró el movimiento. */
    @NotNull(message = "El empleado es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_movimiento_empleado"))
    private Empleado empleado;

    /** Producto afectado; {@code null} si el movimiento es sobre un insumo. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", foreignKey = @ForeignKey(name = "fk_movimiento_producto"))
    private Producto producto;

    /** Insumo afectado; {@code null} si el movimiento es sobre un producto. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insumo_id", foreignKey = @ForeignKey(name = "fk_movimiento_insumo"))
    private Insumo insumo;

    /** Cantidad del movimiento; debe ser mayor a {@code 0.000}. */
    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.001", message = "La cantidad debe ser mayor a 0")
    @Digits(integer = 9, fraction = 3, message = "La cantidad debe tener máximo 9 dígitos enteros y 3 decimales")
    @Column(name = "movimiento_cantidad", nullable = false, precision = 12, scale = 3)
    private BigDecimal movimientoCantidad;

    /** Dirección del movimiento: {@code INGRESO} o {@code EGRESO}. */
    @NotNull(message = "El tipo de movimiento es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "movimiento_tipo", nullable = false, length = 20)
    private TipoMovimiento movimientoTipo;

    /** Nombre del proveedor; aplica solo en ingresos por compra. Máximo 150 caracteres. */
    @Size(max = 150, message = "El proveedor no debe exceder 150 caracteres")
    @Column(name = "movimiento_proveedor", length = 150)
    private String movimientoProveedor;

    /** Número de factura del proveedor; aplica solo en ingresos por compra. Máximo 150 caracteres. */
    @Size(max = 150, message = "El número de factura no debe exceder 150 caracteres")
    @Column(name = "movimiento_numero_factura", length = 150)
    private String movimientoNumeroFactura;

    /** Observaciones adicionales sobre el movimiento; sin límite de longitud. */
    @Column(name = "movimiento_observaciones", columnDefinition = "TEXT")
    private String movimientoObservaciones;

    /** Fecha y hora en que se realizó el movimiento; por defecto la fecha y hora de creación. */
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
