package co.edu.unicauca.backend.modules.inventario.entity;

import co.edu.unicauca.backend.shared.entity.AuditableEntity;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import co.edu.unicauca.backend.shared.enums.TipoComponenteMenu;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Entidad OpcionModificacion — opción de componente para menú especial.
 * Las opciones se agrupan por tipo_componente. 
 */
@Entity
@Table(name = "opcion_modificacion", schema = "restaurante",
       indexes = {
           @Index(name = "idx_opcion_tipo_componente", columnList = "tipo_componente"),
           @Index(name = "idx_opcion_estado", columnList = "opcion_estado")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpcionModificacion extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "opcion_id")
    private Long opcionId;

    @NotNull(message = "El tipo de componente es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_componente", nullable = false, length = 30)
    private TipoComponenteMenu tipoComponente;

    @NotBlank(message = "El nombre de la opción es obligatorio")
    @Size(max = 150, message = "El nombre no debe exceder 150 caracteres")
    @Column(name = "opcion_nombre", nullable = false, length = 150)
    private String opcionNombre;

    @NotNull(message = "El estado de la opción es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "opcion_estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoGenerico opcionEstado = EstadoGenerico.ACTIVO;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OpcionModificacion)) return false;
        OpcionModificacion that = (OpcionModificacion) o;
        return opcionId != null && opcionId.equals(that.getOpcionId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
