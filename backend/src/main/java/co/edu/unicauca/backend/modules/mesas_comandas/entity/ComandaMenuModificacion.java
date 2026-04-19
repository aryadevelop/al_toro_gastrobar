package co.edu.unicauca.backend.modules.mesas_comandas.entity;

import co.edu.unicauca.backend.modules.inventario.entity.OpcionModificacion;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Opción de personalización de menú especial seleccionada para un ítem de {@link ComandaItem}.
 *
 * <p>Solo existe para ítems marcados como menú especial. Cada fila vincula un
 * {@link ComandaItem} con una {@link OpcionModificacion} válida para el producto
 * de ese ítem, verificada previamente contra el catálogo de opciones del producto.
 *
 * <p>Estrategia de índices:
 * <ul>
 *   <li>{@code idx_cmd_menu_mod_detalle} — carga de modificaciones por detalle de comanda.</li>
 *   <li>{@code idx_cmd_menu_mod_opcion} — análisis de uso de opciones de modificación.</li>
 * </ul>
 *
 * @see ComandaItem
 * @see co.edu.unicauca.backend.modules.inventario.entity.OpcionModificacion
 */
@Entity
@Table(name = "comanda_menu_modificacion", schema = "restaurante",
       indexes = {
           @Index(name = "idx_cmd_menu_mod_detalle", columnList = "comanda_item_id"),
           @Index(name = "idx_cmd_menu_mod_opcion", columnList = "opcion_id")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComandaMenuModificacion {

    /** Identificador único generado por la base de datos. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Ítem de comanda al que pertenece esta modificación; obligatorio. */
    @NotNull(message = "El ítem es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comanda_item_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_cmd_menu_mod_detalle"))
    private ComandaItem comandaItem;

    /** Opción de modificación seleccionada; debe pertenecer al menú del producto del ítem. */
    @NotNull(message = "La opción de modificación es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opcion_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_cmd_menu_mod_opcion"))
    private OpcionModificacion opcion;

    @Column(name = "created_at", nullable = false)
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
        if (!(o instanceof ComandaMenuModificacion)) return false;
        ComandaMenuModificacion that = (ComandaMenuModificacion) o;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
