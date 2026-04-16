package co.edu.unicauca.backend.modules.reservas.entity;

import co.edu.unicauca.backend.modules.produccion.entity.Producto;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad PreOrdenDetalle - Pre-orden de productos asociados a una reserva
 */
@Entity
@Table(name = "preorden_detalle", schema = "restaurante",
       indexes = {
           @Index(name = "idx_preorden_reserva_id", columnList = "reserva_id"),
           @Index(name = "idx_preorden_producto_id", columnList = "producto_id")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreOrdenDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "preorden_detalle_id")
    private Long preordenDetalleId;

    @NotNull(message = "La reserva es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_preorden_reserva"))
    private Reserva reserva;

    @NotNull(message = "El producto es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_preorden_producto"))
    private Producto producto;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    @Column(name = "preorden_detalle_cantidad", nullable = false)
    private Integer preordenDetalleCantidad;

    @Size(max = 500, message = "La descripción no debe exceder 500 caracteres")
    @Column(name = "preorden_detalle_descripcion", length = 500)
    private String preordenDetalleDescripcion;

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
        if (!(o instanceof PreOrdenDetalle)) return false;
        PreOrdenDetalle that = (PreOrdenDetalle) o;
        return preordenDetalleId != null && preordenDetalleId.equals(that.getPreordenDetalleId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
