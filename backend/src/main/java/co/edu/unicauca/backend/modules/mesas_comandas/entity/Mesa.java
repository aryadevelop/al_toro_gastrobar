package co.edu.unicauca.backend.modules.mesas_comandas.entity;

import co.edu.unicauca.backend.shared.entity.AuditableEntity;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.shared.enums.EstadoMesa;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Entidad Mesa - Información de mesa por visita con asignación de mesero
 */
@Entity
@Table(name = "mesa", schema = "restaurante",
       indexes = {
           @Index(name = "idx_mesa_mesero_id", columnList = "mesero_id"),
           @Index(name = "idx_mesa_zona_id", columnList = "zona_id"),
           @Index(name = "idx_mesa_estado", columnList = "mesa_estado"),
           @Index(name = "idx_mesa_identificador", columnList = "mesa_identificador"),
           @Index(name = "idx_mesa_activas", columnList = "mesero_id, mesa_estado")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mesa extends AuditableEntity {

    @Id
    @Column(name = "visita_id")
    private Long visitaId;

    @NotNull(message = "La visita es obligatoria")
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "visita_id", foreignKey = @ForeignKey(name = "fk_mesa_visita"))
    private Visita visita;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zona_id", foreignKey = @ForeignKey(name = "fk_mesa_zona"))
    private Zona zona;

    @NotNull(message = "El mesero es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mesero_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_mesa_mesero"))
    private Empleado mesero;

    @NotBlank(message = "El identificador de mesa es obligatorio")
    @Size(max = 20, message = "El identificador no debe exceder 20 caracteres")
    @Column(name = "mesa_identificador", nullable = false, length = 20)
    private String mesaIdentificador;

    @NotNull(message = "El número de personas es obligatorio")
    @Min(value = 1, message = "El número de personas debe ser al menos 1")
    @Column(name = "mesa_numero_personas", nullable = false)
    private Integer mesaNumeroPersonas;

    @NotNull(message = "El estado de la mesa es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "mesa_estado", nullable = false, length = 20)
    private EstadoMesa mesaEstado;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Mesa)) return false;
        Mesa mesa = (Mesa) o;
        return visitaId != null && visitaId.equals(mesa.getVisitaId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
