package co.edu.unicauca.backend.modules.reservas.entity;

import co.edu.unicauca.backend.modules.inventario.entity.OpcionModificacion;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad que registra una opción de modificación seleccionada por el cliente
 * para un ítem de menú especial en la pre-orden.
 *
 * <p>Cada fila representa una opción elegida (p. ej. {@code "Arroz marinero"},
 * {@code "Sin pechuga"}) vinculada a un {@link PreOrdenDetalle} específico.
 * Un mismo detalle puede tener múltiples modificaciones asociadas.
 *
 * <p>Reglas de uso:
 * <ul>
 *   <li>Aplica únicamente a ítems de pre-orden cuyo producto es de tipo menú especial
 *       con opciones de modificación configuradas.</li>
 *   <li>Las opciones disponibles provienen de
 *       {@link co.edu.unicauca.backend.modules.inventario.entity.OpcionModificacion}.</li>
 *   <li>{@code createdAt} se asigna automáticamente en {@code @PrePersist}.</li>
 * </ul>
 *
 * @see PreOrdenDetalle
 * @see co.edu.unicauca.backend.modules.inventario.entity.OpcionModificacion
 */
@Entity
@Table(name = "preorden_menu_modificacion", schema = "restaurante",
       indexes = {
           @Index(name = "idx_preorden_menu_mod_detalle", columnList = "preorden_detalle_id"),
           @Index(name = "idx_preorden_menu_mod_opcion", columnList = "opcion_id")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreOrdenMenuModificacion {

    /** Identificador único de la modificación de pre-orden. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Ítem de pre-orden al que pertenece esta modificación; obligatorio. */
    @NotNull(message = "El detalle de pre-orden es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preorden_detalle_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_preorden_menu_mod_detalle"))
    private PreOrdenDetalle preordenDetalle;

    /** Opción de modificación seleccionada por el cliente; obligatorio. */
    @NotNull(message = "La opción de modificación es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opcion_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_preorden_menu_mod_opcion"))
    private OpcionModificacion opcion;

    /** Fecha y hora en que se registró la modificación; asignada automáticamente. */
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PreOrdenMenuModificacion)) return false;
        PreOrdenMenuModificacion that = (PreOrdenMenuModificacion) o;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
