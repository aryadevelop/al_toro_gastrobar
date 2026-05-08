package co.edu.unicauca.backend.modules.inventario.entity;

import co.edu.unicauca.backend.shared.entity.AuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Entidad que representa una categoría de la carta del restaurante.
 *
 * <p>Agrupa los productos ({@link Producto}) de la carta bajo un nombre común.
 * El campo {@code orden} determina la posición de la categoría dentro de la carta.
 *
 * <p>El campo {@code activo} permite deshabilitar una categoría sin eliminarla,
 * ocultándola de la carta visible al cliente.
 */
@Entity
@Table(name = "categoriacarta", schema = "restaurante")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoriaCarta extends AuditableEntity {

    /** Identificador único de la categoría de carta. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "categoriacarta_id")
    private Integer categoriacartaId;

    /** Nombre de la categoría (único, máximo 100 caracteres); obligatorio. */
    @NotBlank(message = "El nombre de la categoría es obligatorio")
    @Size(max = 100, message = "El nombre no debe exceder 100 caracteres")
    @Column(name = "categoria_nombre", unique = true, nullable = false, length = 100)
    private String categoriaNombre;

    /**
     * Posición de visualización de la categoría en la carta.
     * Valores menores se muestran primero; por defecto {@code 0}.
     */
    @NotNull
    @Column(name = "orden", nullable = false)
    @Builder.Default
    private Integer orden = 0;

    /**
     * Indica si la categoría está habilitada en la carta visible al cliente.
     * {@code false} oculta la categoría sin eliminarla; por defecto {@code true}.
     */
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
