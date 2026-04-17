package co.edu.unicauca.backend.modules.reservas.entity;

import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.shared.entity.AuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Entidad que representa un bloqueo de disponibilidad creado por un administrador.
 *
 * <p>Permite inhabilitar franjas horarias o días completos para que no puedan
 * registrarse nuevas reservas en ese rango. El bloqueo abarca desde
 * {@code fechaInicio} hasta {@code fechaFin}.
 *
 * <p>Reglas de uso:
 * <ul>
 *   <li>Si {@code horaInicio} y {@code horaFin} son {@code null}, el bloqueo aplica
 *       al día completo para cada fecha del rango.</li>
 *   <li>{@code horaFin} es exclusiva: un bloqueo {@code 12:00–14:00} no cubre las 14:00.</li>
 *   <li>Solo un empleado con rol {@code ADM} y estado activo puede ser referenciado
 *       en {@code creadoPor} (validado por trigger en BD y por la capa de servicio).</li>
 *   <li>{@code motivo} es opcional; su ausencia no invalida el bloqueo.</li>
 * </ul>
 *
 * @see co.edu.unicauca.backend.shared.entity.AuditableEntity
 * @see co.edu.unicauca.backend.modules.usuarios.entity.Empleado
 */
@Entity
@Table(name = "bloque_disponibilidad", schema = "restaurante",
       indexes = {
           @Index(name = "idx_bloque_fecha_inicio", columnList = "bloque_fecha_inicio"),
           @Index(name = "idx_bloque_fecha_fin",   columnList = "bloque_fecha_fin")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BloqueDisponibilidad extends AuditableEntity {

    /** Identificador único del bloqueo de disponibilidad. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bloque_id")
    private Long bloqueId;

    /**
     * Fecha en que inicia el bloqueo.
     * Debe ser anterior o igual a {@code fechaFin}; obligatorio.
     */
    @NotNull(message = "La fecha de inicio es obligatoria")
    @Column(name = "bloque_fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    /**
     * Fecha en que termina el bloqueo.
     * Debe ser posterior o igual a {@code fechaInicio}; obligatorio.
     */
    @NotNull(message = "La fecha de fin es obligatoria")
    @Column(name = "bloque_fecha_fin", nullable = false)
    private LocalDate fechaFin;

    /**
     * Hora de inicio del bloqueo dentro del día.
     * {@code null} indica que el bloqueo cubre el día completo junto con {@code horaFin}.
     */
    @Column(name = "bloque_hora_inicio")
    private LocalTime horaInicio;

    /**
     * Hora de fin del bloqueo dentro del día.
     * {@code null} indica que el bloqueo cubre el día completo junto con {@code horaInicio}.
     */
    @Column(name = "bloque_hora_fin")
    private LocalTime horaFin;

    /** Descripción del motivo del bloqueo; {@code null} si no se especifica. */
    @Column(name = "bloque_motivo", length = 255)
    private String motivo;

    /**
     * Empleado administrador que registró el bloqueo.
     * Solo empleados con rol {@code ADM} y estado activo pueden ser referenciados aquí
     * (validado por trigger en BD y por la capa de servicio al crear el bloqueo).
     */
    @NotNull(message = "El administrador que crea el bloqueo es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_bloque_admin"))
    private Empleado creadoPor;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BloqueDisponibilidad)) return false;
        BloqueDisponibilidad that = (BloqueDisponibilidad) o;
        return bloqueId != null && bloqueId.equals(that.getBloqueId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
