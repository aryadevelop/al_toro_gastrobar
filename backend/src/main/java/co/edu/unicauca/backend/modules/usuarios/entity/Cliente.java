package co.edu.unicauca.backend.modules.usuarios.entity;

import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.shared.entity.AuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Perfil extendido de un cliente del restaurante.
 *
 * <p>Complementa la entidad {@link Usuario} (que almacena credenciales) con los
 * datos de contacto, preferencias y el programa de fidelización. La relación es
 * {@code @OneToOne} con {@code @MapsId}, por lo que comparte la PK con {@link Usuario}.
 *
 * <p>Programa de fidelización:
 * <ul>
 *   <li>{@code clientePuntos} — saldo canjeable actual; se incrementa en 1 al cerrar
 *       cuenta y se resetea a 0 en cada canje.</li>
 *   <li>{@code clientePuntosAcumulados} — contador de vida; solo crece, nunca disminuye.
 *       Sirve para estadísticas y recompensas por volumen histórico.</li>
 * </ul>
 *
 * <p>Estrategia de índices:
 * <ul>
 *   <li>{@code idx_cliente_telefono} — búsqueda de clientes por teléfono en recepción.</li>
 *   <li>{@code idx_cliente_puntos} — ordenamiento por saldo de puntos en dashboards.</li>
 * </ul>
 *
 * @see co.edu.unicauca.backend.modules.usuarios.entity.CanjePuntos
 * @see co.edu.unicauca.backend.modules.usuarios.service.PuntosService
 */
@Entity
@Table(name = "cliente", schema = "restaurante",
       indexes = {
           @Index(name = "idx_cliente_telefono", columnList = "cliente_telefono"),
           @Index(name = "idx_cliente_puntos",   columnList = "cliente_puntos")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente extends AuditableEntity {

    /** PK compartida con {@link Usuario} vía {@code @MapsId}. */
    @Id
    @Column(name = "usuario_id")
    private Long usuarioId;

    /** Entidad de autenticación asociada; establece la PK mediante {@code @MapsId}. */
    @NotNull(message = "El usuario es obligatorio")
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "usuario_id", foreignKey = @ForeignKey(name = "fk_cliente_usuario"))
    private Usuario usuario;

    /** Nombre completo del cliente para mostrar en pantalla; máximo 100 caracteres. */
    @NotBlank(message = "El nombre del cliente es obligatorio")
    @Size(max = 100, message = "El nombre no debe exceder 100 caracteres")
    @Column(name = "cliente_nombre", nullable = false, length = 100)
    private String clienteNombre;

    /** Número de contacto de 10 dígitos; formato colombiano sin indicativo. */
    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^[0-9]{10}$", message = "El teléfono debe tener 10 dígitos")
    @Column(name = "cliente_telefono", nullable = false, length = 10)
    private String clienteTelefono;

    /** Dirección de domicilio del cliente; {@code null} si no fue proporcionada. */
    @Size(max = 255, message = "La dirección no debe exceder 255 caracteres")
    @Column(name = "cliente_direccion", length = 255)
    private String clienteDireccion;

    /** Fecha de nacimiento del cliente; {@code null} si no fue proporcionada. */
    @Column(name = "cliente_fecha_nacimiento")
    private LocalDate clienteFechaNacimiento;

    /**
     * Saldo actual de puntos canjeables. Inicia en {@code 0}.
     * Se incrementa en 1 al cerrar cuenta ({@link co.edu.unicauca.backend.modules.pagos_caja.service.VentaService})
     * y se resetea a {@code 0} al canjear ({@link co.edu.unicauca.backend.modules.usuarios.service.PuntosService}).
     */
    @NotNull
    @Min(value = 0, message = "Los puntos no pueden ser negativos")
    @Column(name = "cliente_puntos", nullable = false)
    @Builder.Default
    private Integer clientePuntos = 0;

    /**
     * Contador acumulado de puntos de por vida; nunca disminuye.
     * Se incrementa junto con {@code clientePuntos} al cerrar cuenta,
     * pero no cambia en el canje.
     */
    @NotNull
    @Min(value = 0, message = "Los puntos acumulados no pueden ser negativos")
    @Column(name = "cliente_puntos_acumulados", nullable = false)
    @Builder.Default
    private Integer clientePuntosAcumulados = 0;

    /** {@code true} si el cliente aceptó los términos y condiciones al registrarse. */
    @NotNull(message = "La aceptación de términos es obligatoria")
    @Column(name = "cliente_acepta_terminos", nullable = false)
    private Boolean clienteAceptaTerminos;

    /** Fecha y hora en que el cliente aceptó los términos y condiciones. */
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
