package co.edu.unicauca.backend.modules.usuarios.entity;

import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.shared.entity.AuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidad Cliente - Información específica de clientes
 */
@Entity
@Table(name = "cliente", schema = "restaurante",
       indexes = {
           @Index(name = "idx_cliente_telefono", columnList = "cliente_telefono"),
           @Index(name = "idx_cliente_puntos", columnList = "cliente_puntos")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente extends AuditableEntity {

    @Id
    @Column(name = "usuario_id")
    private Long usuarioId;

    @NotNull(message = "El usuario es obligatorio")
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "usuario_id", foreignKey = @ForeignKey(name = "fk_cliente_usuario"))
    private Usuario usuario;

    @NotBlank(message = "El nombre del cliente es obligatorio")
    @Size(max = 100, message = "El nombre no debe exceder 100 caracteres")
    @Column(name = "cliente_nombre", nullable = false, length = 100)
    private String clienteNombre;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^[0-9]{10}$", message = "El teléfono debe tener 10 dígitos")
    @Column(name = "cliente_telefono", nullable = false, length = 10)
    private String clienteTelefono;

    @Size(max = 255, message = "La dirección no debe exceder 255 caracteres")
    @Column(name = "cliente_direccion", length = 255)
    private String clienteDireccion;

    @Column(name = "cliente_fecha_nacimiento")
    private LocalDate clienteFechaNacimiento;

    @NotNull
    @Min(value = 0, message = "Los puntos no pueden ser negativos")
    @Column(name = "cliente_puntos", nullable = false)
    @Builder.Default
    private Integer clientePuntos = 0;

    @NotNull(message = "La aceptación de términos es obligatoria")
    @Column(name = "cliente_acepta_terminos", nullable = false)
    private Boolean clienteAceptaTerminos;

    @NotNull(message = "La fecha de aceptación es obligatoria")
    @Column(name = "cliente_fecha_aceptacion", nullable = false)
    private LocalDateTime clienteFechaAceptacion;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cliente)) return false;
        Cliente cliente = (Cliente) o;
        return usuarioId != null && usuarioId.equals(cliente.getUsuarioId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
