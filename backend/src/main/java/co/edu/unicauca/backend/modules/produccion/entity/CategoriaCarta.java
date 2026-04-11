package co.edu.unicauca.backend.modules.produccion.entity;

import co.edu.unicauca.backend.shared.entity.AuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Entidad CategoriaCarta - Categorías para organizar productos en la carta
 */
@Entity
@Table(name = "categoriacarta", schema = "restaurante")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoriaCarta extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "categoriacarta_id")
    private Integer categoriacartaId;

    @NotBlank(message = "El nombre de la categoría es obligatorio")
    @Size(max = 100, message = "El nombre no debe exceder 100 caracteres")
    @Column(name = "categoria_nombre", unique = true, nullable = false, length = 100)
    private String categoriaNombre;

    @NotNull
    @Column(name = "orden", nullable = false)
    @Builder.Default
    private Integer orden = 0;

    @NotNull
    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CategoriaCarta)) return false;
        CategoriaCarta that = (CategoriaCarta) o;
        return categoriacartaId != null && categoriacartaId.equals(that.getCategoriacartaId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
