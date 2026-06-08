package co.edu.unicauca.backend.modules.usuarios.entity;

import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.shared.entity.AuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Perfil extendido de un empleado del restaurante.
 *
 * <p>Complementa la entidad {@link Usuario} (que almacena credenciales) con los
 * datos laborales y de contacto del empleado. La relación es {@code @OneToOne}
 * con {@code @MapsId}, por lo que comparte la PK con {@link Usuario}.
 *
 * <p>Un empleado puede cumplir varios roles operativos según los roles asignados
 * en {@link Usuario}: cajero, mesero, cocinero, bartender o administrador.
 * La lógica de negocio no discrimina por tipo de empleado en esta entidad;
 * eso lo gestiona el sistema de roles.
 *
 * <p>Estrategia de índices:
 * <ul>
 *   <li>{@code idx_empleado_telefono} — búsqueda de empleados por teléfono en RRHH.</li>
 *   <li>{@code idx_empleado_fecha_ingreso} — ordenamiento por antigüedad en reportes.</li>
 * </ul>
 *
 * @see co.edu.unicauca.backend.modules.auth.entity.Usuario
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

    /** PK compartida con {@link Usuario} vía {@code @MapsId}. */
    @Id
    @Column(name = "usuario_id")
    private Long usuarioId;

    /** Entidad de autenticación asociada; establece la PK mediante {@code @MapsId}. */
    @NotNull(message = "El usuario es obligatorio")
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "usuario_id", foreignKey = @ForeignKey(name = "fk_empleado_usuario"))
    private Usuario usuario;

    /** Nombre completo del empleado para mostrar en pantalla; máximo 100 caracteres. */
    @NotBlank(message = "El nombre del empleado es obligatorio")
    @Size(max = 100, message = "El nombre no debe exceder 100 caracteres")
    @Column(name = "empleado_nombre", nullable = false, length = 100)
    private String empleadoNombre;

    /** Dirección de domicilio del empleado; {@code null} si no fue proporcionada. */
    @Size(max = 255, message = "La dirección no debe exceder 255 caracteres")
    @Column(name = "empleado_direccion", length = 255)
    private String empleadoDireccion;

    /** Número de contacto de 10 dígitos; formato colombiano sin indicativo. */
    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^[0-9]{10}$", message = "El teléfono debe tener 10 dígitos")
    @Column(name = "empleado_telefono", nullable = false, length = 10)
    private String empleadoTelefono;

    /** Fecha en que el empleado inició labores; sirve para calcular antigüedad. */
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
