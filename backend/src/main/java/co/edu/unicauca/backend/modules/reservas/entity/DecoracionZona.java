package co.edu.unicauca.backend.modules.reservas.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona;

/**
 * Entidad que representa la relación entre una {@link Decoracion} y una {@link Zona}.
 *
 * <p>Modela la asociación muchos-a-muchos que indica en qué zonas del restaurante
 * puede ofrecerse cada tipo de decoración. Una decoración sin zonas asociadas
 * no está disponible para ninguna zona en particular.
 *
 * <p>Reglas de uso:
 * <ul>
 *   <li>La clave primaria compuesta ({@code decoracionId}, {@code zonaId}) garantiza
 *       que una decoración no se asocie dos veces a la misma zona.</li>
 *   <li>{@code createdAt} se asigna automáticamente en {@code @PrePersist}; no debe
 *       establecerse manualmente.</li>
 * </ul>
 *
 * @see Decoracion
 * @see co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona
 */
@Entity
@Table(name = "decoracion_zona", schema = "restaurante")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(DecoracionZona.DecoracionZonaId.class)
public class DecoracionZona {

    /** Parte de la clave compuesta: identificador de la decoración. */
    @Id
    @Column(name = "decoracion_id")
    private Long decoracionId;

    /** Parte de la clave compuesta: identificador de la zona. */
    @Id
    @Column(name = "zona_id")
    private Long zonaId;

    /** Decoración asociada; cargada de forma lazy. */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decoracion_id", insertable = false, updatable = false,
                foreignKey = @ForeignKey(name = "fk_decoracion_zona_decoracion"))
    private Decoracion decoracion;

    /** Zona del restaurante asociada a la decoración; cargada de forma lazy. */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zona_id", insertable = false, updatable = false,
                foreignKey = @ForeignKey(name = "fk_decoracion_zona_zona"))
    private Zona zona;

    /** Fecha y hora en que se registró la asociación; asignada automáticamente. */
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Clave primaria compuesta de {@link DecoracionZona}.
     * Combina el identificador de la decoración y el de la zona.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DecoracionZonaId implements Serializable {
        /** Identificador de la decoración. */
        private Long decoracionId;
        /** Identificador de la zona. */
        private Long zonaId;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DecoracionZona)) return false;
        DecoracionZona that = (DecoracionZona) o;
        return decoracionId != null && decoracionId.equals(that.decoracionId) &&
               zonaId != null && zonaId.equals(that.zonaId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
