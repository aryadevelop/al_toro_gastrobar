package co.edu.unicauca.backend.modules.mesas_comandas.entity;

import co.edu.unicauca.backend.modules.produccion.entity.Producto;
import co.edu.unicauca.backend.modules.reservas.entity.PreOrdenDetalle;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad ComandaDetalle - Detalle de productos en cada comanda
 */
@Entity
@Table(name = "comanda_detalle", schema = "restaurante",
       indexes = {
           @Index(name = "idx_comanda_detalle_comanda_id", columnList = "comanda_id"),
           @Index(name = "idx_comanda_detalle_producto_id", columnList = "producto_id")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComandaDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comanda_detalle_id")
    private Long comandaDetalleId;

    @NotNull(message = "La comanda es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comanda_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_comanda_detalle_comanda"))
    private Comanda comanda;

    @NotNull(message = "El producto es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_comanda_detalle_producto"))
    private Producto producto;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    @Column(name = "comanda_detalle_cantidad", nullable = false)
    private Integer comandaDetalleCantidad;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
    @Digits(integer = 10, fraction = 2, message = "El precio debe tener máximo 10 dígitos enteros y 2 decimales")
    @Column(name = "comanda_detalle_precio", nullable = false, precision = 12, scale = 2)
    private BigDecimal comandaDetallePrecio;

    @Size(max = 500, message = "El nombre no debe exceder 500 caracteres")
    @Column(name = "comanda_detalle_nombre", length = 500)
    private String comandaDetalleNombre;

    /** Origen en pre-orden (nullable). Presente cuando la línea fue generada
     *  desde una pre-orden de reserva (CA-07). Null = pedido tomado en mesa. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preorden_detalle_id",
                foreignKey = @ForeignKey(name = "fk_comanda_detalle_preorden"))
    private PreOrdenDetalle preordenDetalle;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ComandaDetalle)) return false;
        ComandaDetalle that = (ComandaDetalle) o;
        return comandaDetalleId != null && comandaDetalleId.equals(that.getComandaDetalleId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
