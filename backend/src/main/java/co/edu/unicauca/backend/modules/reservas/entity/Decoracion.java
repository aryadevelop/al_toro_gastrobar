package co.edu.unicauca.backend.modules.reservas.entity;

import co.edu.unicauca.backend.shared.entity.AuditableEntity;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Entidad que representa un tipo de decoración disponible para reservas especiales.
 *
 * <p>Una decoración es un servicio opcional que el cliente puede seleccionar al crear
 * una reserva. Cada decoración tiene un costo adicional que se suma al valor de la reserva
 * y puede estar disponible solo en ciertas zonas del restaurante (ver {@link DecoracionZona}).
 *
 * <p>Reglas de uso:
 * <ul>
 *   <li>Solo las decoraciones con {@code decoracionEstado = ACTIVO} pueden ser seleccionadas
 *       al crear una reserva.</li>
 *   <li>{@code decoracionCostoAdicional} {@code null} equivale a costo cero; no puede ser negativo.</li>
 *   <li>{@code decoracionImagenUrl} es opcional; su ausencia no invalida la decoración.</li>
 * </ul>
 *
 * @see DecoracionZona
 * @see co.edu.unicauca.backend.shared.enums.EstadoGenerico
 */
@Entity
@Table(name = "decoracion", schema = "restaurante")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Decoracion extends AuditableEntity {

    /** Identificador único de la decoración. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "decoracion_id")
    private Long decoracionId;

    /**
     * Nombre descriptivo de la decoración.
     * Máximo 100 caracteres; obligatorio.
     */
    @NotBlank(message = "El nombre de la decoración es obligatorio")
    @Size(max = 100, message = "El nombre no debe exceder 100 caracteres")
    @Column(name = "decoracion_nombre", nullable = false, length = 100)
    private String decoracionNombre;

    /**
     * Estado actual de la decoración ({@code ACTIVO} o {@code INACTIVO}).
     * Solo las decoraciones {@code ACTIVO} pueden asignarse a nuevas reservas; obligatorio.
     */
    @NotNull(message = "El estado es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "decoracion_estado", nullable = false, length = 20)
    private EstadoGenerico decoracionEstado;

    /**
     * Costo adicional que se cobra al cliente por seleccionar esta decoración.
     * No puede ser negativo; {@code null} equivale a sin costo adicional.
     */
    @DecimalMin(value = "0.00", message = "El costo adicional no puede ser negativo")
    @Digits(integer = 10, fraction = 2, message = "El costo debe tener máximo 10 dígitos enteros y 2 decimales")
    @Column(name = "decoracion_costo_adicional", precision = 12, scale = 2)
    private BigDecimal decoracionCostoAdicional;

    /** URL de la imagen ilustrativa de la decoración; {@code null} si no se ha cargado imagen. */
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
