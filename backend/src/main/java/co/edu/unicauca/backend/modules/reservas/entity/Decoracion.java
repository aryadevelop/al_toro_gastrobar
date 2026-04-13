package co.edu.unicauca.backend.modules.reservas.entity;

import co.edu.unicauca.backend.shared.entity.AuditableEntity;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Entidad Decoracion - Tipos de decoración disponibles para reservas especiales
 */
@Entity
@Table(name = "decoracion", schema = "restaurante")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Decoracion extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "decoracion_id")
    private Long decoracionId;

    @NotBlank(message = "El nombre de la decoración es obligatorio")
    @Size(max = 100, message = "El nombre no debe exceder 100 caracteres")
    @Column(name = "decoracion_nombre", nullable = false, length = 100)
    private String decoracionNombre;

    @NotNull(message = "El estado es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "decoracion_estado", nullable = false, length = 20)
    private EstadoGenerico decoracionEstado;

    @DecimalMin(value = "0.00", message = "El costo adicional no puede ser negativo")
    @Digits(integer = 10, fraction = 2, message = "El costo debe tener máximo 10 dígitos enteros y 2 decimales")
    @Column(name = "decoracion_costo_adicional", precision = 12, scale = 2)
    private BigDecimal decoracionCostoAdicional;

    @Size(max = 500)
    @Column(name = "decoracion_imagen_url", length = 500)
    private String decoracionImagenUrl;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Decoracion)) return false;
        Decoracion that = (Decoracion) o;
        return decoracionId != null && decoracionId.equals(that.getDecoracionId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
