package co.edu.unicauca.backend.modules.inventario.entity;

import co.edu.unicauca.backend.shared.entity.AuditableEntity;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import co.edu.unicauca.backend.shared.enums.UnidadMedida;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Entidad Insumo - Insumos para preparación de productos
 */
@Entity
@Table(name = "insumo", schema = "restaurante",
       indexes = {
           @Index(name = "idx_insumo_nombre", columnList = "insumo_nombre"),
           @Index(name = "idx_insumo_estado", columnList = "insumo_estado"),
           @Index(name = "idx_insumo_stock", columnList = "insumo_stock_actual")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Insumo extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "insumo_id")
    private Long insumoId;

    @NotBlank(message = "El nombre del insumo es obligatorio")
    @Size(max = 100, message = "El nombre no debe exceder 100 caracteres")
    @Column(name = "insumo_nombre", nullable = false, length = 100)
    private String insumoNombre;

    @NotNull(message = "La unidad de medida es obligatoria")
    @Enumerated(EnumType.STRING)
    @Column(name = "insumo_unidad", nullable = false, length = 20)
    private UnidadMedida insumoUnidad;

    @NotNull
    @DecimalMin(value = "0.000", message = "El stock no puede ser negativo")
    @Digits(integer = 9, fraction = 3, message = "El stock debe tener máximo 9 dígitos enteros y 3 decimales")
    @Column(name = "insumo_stock_actual", nullable = false, precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal insumoStockActual = BigDecimal.ZERO;

    @NotNull(message = "El estado del insumo es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "insumo_estado", nullable = false, length = 20)
    private EstadoGenerico insumoEstado;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Insumo)) return false;
        Insumo insumo = (Insumo) o;
        return insumoId != null && insumoId.equals(insumo.getInsumoId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
