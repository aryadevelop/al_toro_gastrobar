package co.edu.unicauca.backend.modules.inventario.entity;

import co.edu.unicauca.backend.shared.entity.AuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Entidad Receta - Relación insumo-producto con cantidades
 */
@Entity
@Table(name = "receta", schema = "restaurante",
       indexes = {
           @Index(name = "idx_receta_producto_id", columnList = "producto_id")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(Receta.RecetaId.class)
public class Receta extends AuditableEntity {

    @Id
    @Column(name = "insumo_id")
    private Long insumoId;

    @Id
    @Column(name = "producto_id")
    private Long productoId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insumo_id", insertable = false, updatable = false,
                foreignKey = @ForeignKey(name = "fk_receta_insumo"))
    private Insumo insumo;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", insertable = false, updatable = false,
                foreignKey = @ForeignKey(name = "fk_receta_producto"))
    private Producto producto;

    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.001", message = "La cantidad debe ser mayor a 0")
    @Digits(integer = 9, fraction = 3, message = "La cantidad debe tener máximo 9 dígitos enteros y 3 decimales")
    @Column(name = "receta_cantidad", nullable = false, precision = 12, scale = 3)
    private BigDecimal recetaCantidad;

    /**
     * Clase interna para la clave compuesta
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecetaId implements Serializable {
        private Long insumoId;
        private Long productoId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Receta)) return false;
        Receta receta = (Receta) o;
        return insumoId != null && insumoId.equals(receta.insumoId) &&
               productoId != null && productoId.equals(receta.productoId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
