package co.edu.unicauca.backend.modules.auth.entity;

import co.edu.unicauca.backend.shared.entity.AuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Entidad de autenticación base de todos los usuarios del sistema.
 *
 * <p>Almacena únicamente las credenciales de acceso (email + password hasheado).
 * Los datos de perfil específicos residen en las tablas especializadas
 * {@link co.edu.unicauca.backend.modules.usuarios.entity.Cliente} y
 * {@link co.edu.unicauca.backend.modules.usuarios.entity.Empleado}, vinculadas
 * mediante una relación {@code @OneToOne} con {@code @MapsId}.
 *
 * <p>El índice sobre {@code usuario_email} cubre las búsquedas por credencial
 * realizadas en cada request autenticado.
 *
 * @see co.edu.unicauca.backend.modules.auth.entity.Sesion
 * @see co.edu.unicauca.backend.modules.usuarios.entity.UsuarioRol
 */
@Entity
@Table(name = "usuario", schema = "restaurante",
       indexes = {
           @Index(name = "idx_usuario_email", columnList = "usuario_email")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario extends AuditableEntity {

    /** Identificador único generado por la base de datos. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usuario_id")
    private Long usuarioId;

    /**
     * Correo electrónico del usuario; actúa como nombre de usuario en la autenticación.
     * Único en el sistema; máximo 150 caracteres.
     */
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    @Size(max = 150, message = "El email no debe exceder 150 caracteres")
    @Column(name = "usuario_email", unique = true, nullable = false, length = 150)
    private String usuarioEmail;

    /**
     * Hash BCrypt de la contraseña. Nunca se almacena en texto plano.
     * Máximo 255 caracteres para acomodar el hash completo de BCrypt.
     */
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(max = 255, message = "La contraseña no debe exceder 255 caracteres")
    @Column(name = "usuario_password", nullable = false, length = 255)
    private String usuarioPassword;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Usuario)) return false;
        Usuario usuario = (Usuario) o;
        return usuarioId != null && usuarioId.equals(usuario.getUsuarioId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
