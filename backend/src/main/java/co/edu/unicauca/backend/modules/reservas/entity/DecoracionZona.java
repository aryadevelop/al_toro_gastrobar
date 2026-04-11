package co.edu.unicauca.backend.modules.reservas.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona;

/**
 * Entidad DecoracionZona - Relación muchos a muchos entre Decoracion y Zona
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

    @Id
    @Column(name = "decoracion_id")
    private Long decoracionId;

    @Id
    @Column(name = "zona_id")
    private Long zonaId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decoracion_id", insertable = false, updatable = false,
                foreignKey = @ForeignKey(name = "fk_decoracion_zona_decoracion"))
    private Decoracion decoracion;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zona_id", insertable = false, updatable = false,
                foreignKey = @ForeignKey(name = "fk_decoracion_zona_zona"))
    private Zona zona;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Clase interna para la clave compuesta
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DecoracionZonaId implements Serializable {
        private Long decoracionId;
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
