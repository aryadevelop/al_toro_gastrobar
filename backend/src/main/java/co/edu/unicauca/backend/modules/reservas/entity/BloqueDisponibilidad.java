package co.edu.unicauca.backend.modules.reservas.entity;

import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.shared.entity.AuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Bloqueo de disponibilidad creado por un empleado administrador.
 * Permite inhabilitar franjas de horas o días completos para las reservas.
 * Si horaInicio/horaFin son null, el bloqueo aplica al día completo.
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bloque_id")
    private Long bloqueId;

    @NotNull(message = "La fecha de inicio es obligatoria")
    @Column(name = "bloque_fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @NotNull(message = "La fecha de fin es obligatoria")
    @Column(name = "bloque_fecha_fin", nullable = false)
    private LocalDate fechaFin;

    /**
     * Hora de inicio del bloqueo (inclusive). Si es null, bloquea el día completo.
     */
    @Column(name = "bloque_hora_inicio")
    private LocalTime horaInicio;

    /**
     * Hora de fin del bloqueo (exclusiva). Si es null, bloquea el día completo.
     */
    @Column(name = "bloque_hora_fin")
    private LocalTime horaFin;

    @Column(name = "bloque_motivo", length = 255)
    private String motivo;

    /**
     * Empleado administrador que creó el bloqueo.
     * Solo empleados con rol ADM activo pueden ser referenciados aquí
     * (validado por trigger en BD y por lógica de servicio al crear).
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
