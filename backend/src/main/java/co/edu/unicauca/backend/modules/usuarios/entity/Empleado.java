package co.edu.unicauca.backend.modules.usuarios.entity;

import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.shared.entity.AuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Entidad Empleado - Información específica de empleados
 */
@Entity
@Table(name = "empleado", schema = "restaurante",
       indexes = {
           @Index(name = "idx_empleado_telefono", columnList = "empleado_telefono"),
           @Index(name = "idx_empleado_fecha_ingreso", columnList = "empleado_fecha_ingreso")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Empleado extends AuditableEntity {

    @Id
    @Column(name = "usuario_id")
    private Long usuarioId;

    @NotNull(message = "El usuario es obligatorio")
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "usuario_id", foreignKey = @ForeignKey(name = "fk_empleado_usuario"))
    private Usuario usuario;

    @NotBlank(message = "El nombre del empleado es obligatorio")
    @Size(max = 100, message = "El nombre no debe exceder 100 caracteres")
    @Column(name = "empleado_nombre", nullable = false, length = 100)
    private String empleadoNombre;

    @Size(max = 255, message = "La dirección no debe exceder 255 caracteres")
    @Column(name = "empleado_direccion", length = 255)
    private String empleadoDireccion;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^[0-9]{10}$", message = "El teléfono debe tener 10 dígitos")
    @Column(name = "empleado_telefono", nullable = false, length = 10)
    private String empleadoTelefono;

    @NotNull(message = "La fecha de ingreso es obligatoria")
    @Column(name = "empleado_fecha_ingreso", nullable = false)
    private LocalDate empleadoFechaIngreso;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Empleado)) return false;
        Empleado empleado = (Empleado) o;
        return usuarioId != null && usuarioId.equals(empleado.getUsuarioId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
