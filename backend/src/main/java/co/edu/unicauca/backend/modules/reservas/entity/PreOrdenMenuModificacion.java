package co.edu.unicauca.backend.modules.reservas.entity;

import co.edu.unicauca.backend.modules.inventario.entity.OpcionModificacion;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad PreOrdenMenuModificacion — registra los checkboxes de modificación
 * seleccionados por el cliente para un ítem de menú especial en la pre-orden.
 * Cada fila representa una opción elegida (ej. "Arroz marinero", "Sin pechuga").
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull(message = "El detalle de pre-orden es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preorden_detalle_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_preorden_menu_mod_detalle"))
    private PreOrdenDetalle preordenDetalle;

    @NotNull(message = "La opción de modificación es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opcion_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_preorden_menu_mod_opcion"))
    private OpcionModificacion opcion;

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
