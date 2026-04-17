package co.edu.unicauca.backend.modules.auth.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Registro de una sesión activa de usuario en el sistema.
 *
 * <p>Cada fila representa un par de tokens (access + refresh) emitidos para un usuario.
 * El filtro JWT ({@link co.edu.unicauca.backend.modules.auth.security.JwtAuthenticationFilter})
 * consulta esta tabla para verificar que el token recibido corresponda a una sesión todavía
 * activa, implementando así la revocación de tokens sin listas negras externas.
 *
 * <p>Estrategia de índices:
 * <ul>
 *   <li>{@code idx_sesion_token} — búsqueda por access token en cada request autenticado.</li>
 *   <li>{@code idx_sesion_usuario_id} — cierre masivo de sesiones del usuario en logout.</li>
 *   <li>{@code idx_sesion_activa} — filtro de sesiones vigentes en todas las consultas.</li>
 *   <li>{@code idx_sesion_fecha_creacion} — soporte para auditoría y limpieza periódica.</li>
 * </ul>
 *
 * @see co.edu.unicauca.backend.modules.auth.service.AuthService
 */
@Entity
@Table(name = "sesion", schema = "restaurante",
       indexes = {
           @Index(name = "idx_sesion_usuario_id",    columnList = "usuario_id"),
           @Index(name = "idx_sesion_token",         columnList = "sesion_token"),
           @Index(name = "idx_sesion_fecha_creacion",columnList = "sesion_fecha_creacion"),
           @Index(name = "idx_sesion_activa",        columnList = "sesion_activa")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sesion {

    /** Identificador único generado por la base de datos. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sesion_id")
    private Long sesionId;

    /** Usuario propietario de la sesión. */
    @NotNull(message = "El usuario es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, foreignKey = @ForeignKey(name = "fk_sesion_usuario"))
    private Usuario usuario;

    /**
     * Access token JWT emitido para esta sesión.
     * Único en el sistema; longitud máxima de 1024 caracteres para acomodar tokens largos.
     */
    @NotBlank(message = "El token de sesión es obligatorio")
    @Column(name = "sesion_token", unique = true, nullable = false, length = 1024)
    private String sesionToken;

    /**
     * Refresh token JWT asociado a esta sesión.
     * Se rota en cada llamada a {@code /api/auth/refresh}; único en el sistema.
     */
    @NotBlank(message = "El refresh token de sesión es obligatorio")
    @Column(name = "sesion_refresh_token", unique = true, nullable = false, length = 1024)
    private String sesionRefreshToken;

    /**
     * Fecha y hora de creación o última rotación de los tokens.
     * Se actualiza al hacer refresh; permite auditar la actividad de la sesión.
     */
    @NotNull
    @Column(name = "sesion_fecha_creacion", nullable = false)
    @Builder.Default
    private LocalDateTime sesionFechaCreacion = LocalDateTime.now();

    /**
     * Indica si la sesión sigue vigente.
     * Se pone en {@code false} en logout o cuando se fuerza el cierre por sesión concurrente.
     */
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
