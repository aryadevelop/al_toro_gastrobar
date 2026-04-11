package co.edu.unicauca.backend.modules.auth.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad Sesion - Gestión de sesiones activas de usuarios
 */
@Entity
@Table(name = "sesion", schema = "restaurante",
       indexes = {
           @Index(name = "idx_sesion_usuario_id", columnList = "usuario_id"),
           @Index(name = "idx_sesion_token", columnList = "sesion_token"),
           @Index(name = "idx_sesion_fecha_creacion", columnList = "sesion_fecha_creacion"),
           @Index(name = "idx_sesion_activa", columnList = "sesion_activa")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sesion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sesion_id")
    private Long sesionId;

    @NotNull(message = "El usuario es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, foreignKey = @ForeignKey(name = "fk_sesion_usuario"))
    private Usuario usuario;

    @NotBlank(message = "El token de sesión es obligatorio")
    @Column(name = "sesion_token", unique = true, nullable = false, length = 512)
    private String sesionToken;

    @NotNull
    @Column(name = "sesion_fecha_creacion", nullable = false)
    @Builder.Default
    private LocalDateTime sesionFechaCreacion = LocalDateTime.now();

    @NotNull
    @Column(name = "sesion_activa", nullable = false)
    @Builder.Default
    private Boolean sesionActiva = true;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Sesion)) return false;
        Sesion sesion = (Sesion) o;
        return sesionId != null && sesionId.equals(sesion.getSesionId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
