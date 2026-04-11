package co.edu.unicauca.backend.modules.mesas_comandas.entity;

import co.edu.unicauca.backend.shared.entity.AuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Entidad Zona - Zonas del restaurante (terraza, salón, VIP, etc.)
 */
@Entity
@Table(name = "zona", schema = "restaurante")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Zona extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "zona_id")
    private Long zonaId;

    @NotBlank(message = "El nombre de la zona es obligatorio")
    @Size(max = 100, message = "El nombre no debe exceder 100 caracteres")
    @Column(name = "zona_nombre", nullable = false, length = 100)
    private String zonaNombre;

    @NotNull(message = "La capacidad de personas es obligatoria")
    @Min(value = 1, message = "La capacidad debe ser al menos 1 persona")
    @Column(name = "zona_capacidad_personas", nullable = false)
    private Integer zonaCapacidadPersonas;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Zona)) return false;
        Zona zona = (Zona) o;
        return zonaId != null && zonaId.equals(zona.getZonaId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
