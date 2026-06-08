package co.edu.unicauca.backend.modules.inventario.entity;

import co.edu.unicauca.backend.shared.entity.AuditableEntity;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import co.edu.unicauca.backend.shared.enums.TipoInsumo;
import co.edu.unicauca.backend.shared.enums.UnidadMedida;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Ingrediente o preparación intermedia utilizada en las recetas del restaurante.
 *
 * <p>Los insumos se clasifican según su nivel de procesamiento mediante {@link TipoInsumo}:
 * {@code MATERIA_PRIMA} para ingredientes comprados directamente, y {@code SEMIELABORADO}
 * para preparaciones de cocina elaboradas en batch. El stock se descuenta al registrar
 * movimientos de tipo {@code EGRESO} en {@link co.edu.unicauca.backend.modules.inventario.entity.MovimientoInventario}.
 *
 * <p>Índices: {@code insumo_nombre}, {@code insumo_estado}, {@code insumo_stock_actual}.
 *
 * @see co.edu.unicauca.backend.modules.inventario.entity.Receta
 * @see co.edu.unicauca.backend.modules.inventario.entity.MovimientoInventario
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

    /** Identificador único generado por la base de datos. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "insumo_id")
    private Long insumoId;

    /** Nombre del insumo; máximo 100 caracteres. */
    @NotBlank(message = "El nombre del insumo es obligatorio")
    @Size(max = 100, message = "El nombre no debe exceder 100 caracteres")
    @Column(name = "insumo_nombre", nullable = false, length = 100)
    private String insumoNombre;

    /** Unidad en la que se mide el stock de este insumo. */
    @NotNull(message = "La unidad de medida es obligatoria")
    @Enumerated(EnumType.STRING)
    @Column(name = "insumo_unidad", nullable = false, length = 20)
    private UnidadMedida insumoUnidad;

    /** Cantidad disponible en inventario; mínimo {@code 0.000}. Por defecto {@code 0}. */
    @NotNull
    @DecimalMin(value = "0.000", message = "El stock no puede ser negativo")
    @Digits(integer = 9, fraction = 3, message = "El stock debe tener máximo 9 dígitos enteros y 3 decimales")
    @Column(name = "insumo_stock_actual", nullable = false, precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal insumoStockActual = BigDecimal.ZERO;

    /** Estado operativo del insumo; {@code INACTIVO} lo excluye de los flujos de producción. */
    @NotNull(message = "El estado del insumo es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "insumo_estado", nullable = false, length = 20)
    private EstadoGenerico insumoEstado;

    /** Nivel de procesamiento del insumo; por defecto {@code MATERIA_PRIMA}. */
    @NotNull(message = "El tipo de insumo es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_insumo", nullable = false, length = 20)
    @Builder.Default
    private TipoInsumo tipoInsumo = TipoInsumo.MATERIA_PRIMA;

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
