package co.edu.unicauca.backend.modules.auth.entity;

import co.edu.unicauca.backend.shared.entity.AuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Entidad Usuario - Tabla principal de autenticación
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usuario_id")
    private Long usuarioId;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    @Size(max = 150, message = "El email no debe exceder 150 caracteres")
    @Column(name = "usuario_email", unique = true, nullable = false, length = 150)
    private String usuarioEmail;

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
