package co.edu.unicauca.backend.modules.usuarios.entity;

import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.shared.entity.AuditableEntity;
import co.edu.unicauca.backend.shared.enums.RolEstado;
import co.edu.unicauca.backend.shared.enums.RolNombre;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.io.Serializable;

/**
 * Asignación de un rol funcional a un usuario del sistema.
 *
 * <p>La clave primaria es compuesta: {@code (usuarioId, rolNombre)}, representada por
 * la clase interna {@link UsuarioRolId}. Un usuario puede tener varios roles simultáneos,
 * cada uno con su propio estado. Solo los roles con {@code rolEstado = ACTIVO} otorgan permisos.
 *
 * <p>Índices: {@code rol_nombre}, {@code rol_estado}, compuesto {@code (usuario_id, rol_nombre)}.
 *
 * @see co.edu.unicauca.backend.shared.enums.RolNombre
 * @see co.edu.unicauca.backend.shared.enums.RolEstado
 */
@Entity
@Table(name = "usuario_rol", schema = "restaurante",
       indexes = {
           @Index(name = "idx_usuario_rol_rol_nombre", columnList = "rol_nombre"),
           @Index(name = "idx_usuario_rol_estado", columnList = "rol_estado"),
           @Index(name = "idx_usuario_rol_activo", columnList = "usuario_id, rol_nombre")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(UsuarioRol.UsuarioRolId.class)
public class UsuarioRol extends AuditableEntity {

    /** Parte de la clave primaria compuesta: identificador del usuario. */
    @Id
    @Column(name = "usuario_id")
    private Long usuarioId;

    /** Parte de la clave primaria compuesta: nombre del rol asignado. */
    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "rol_nombre", length = 20)
    private RolNombre rolNombre;

    /** Referencia al usuario propietario del rol; de solo lectura en la columna {@code usuario_id}. */
    @NotNull(message = "El usuario es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", insertable = false, updatable = false,
                foreignKey = @ForeignKey(name = "fk_usuario_rol_usuario"))
    private Usuario usuario;

    /** Estado actual del rol; solo {@code ACTIVO} otorga permisos en los flujos de autenticación. */
    @NotNull(message = "El estado del rol es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "rol_estado", nullable = false, length = 20)
    private RolEstado rolEstado;

    /**
     * Clave primaria compuesta de {@link UsuarioRol}, formada por {@code usuarioId} y {@code rolNombre}.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsuarioRolId implements Serializable {
        private Long usuarioId;
        private RolNombre rolNombre;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UsuarioRol)) return false;
        UsuarioRol that = (UsuarioRol) o;
        return usuarioId != null && usuarioId.equals(that.usuarioId) &&
               rolNombre != null && rolNombre.equals(that.rolNombre);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
