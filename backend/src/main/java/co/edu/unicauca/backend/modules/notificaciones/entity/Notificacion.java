package co.edu.unicauca.backend.modules.notificaciones.entity;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.shared.enums.EstadoNotificacion;
import co.edu.unicauca.backend.shared.enums.TipoNotificacion;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Notificación enviada a un empleado del restaurante sobre el estado de una mesa.
 *
 * @see co.edu.unicauca.backend.shared.enums.TipoNotificacion
 * @see co.edu.unicauca.backend.shared.enums.EstadoNotificacion
 */
@Entity
@Table(name = "notificacion", schema = "restaurante",
       indexes = {
           @Index(name = "idx_notificacion_mesa_id", columnList = "mesa_id"),
           @Index(name = "idx_notificacion_empleado_id", columnList = "empleado_id"),
           @Index(name = "idx_notificacion_estado", columnList = "notificacion_estado"),
           @Index(name = "idx_notificacion_fecha_hora", columnList = "notificacion_fecha_hora"),
           @Index(name = "idx_notificacion_activas", columnList = "empleado_id, notificacion_fecha_hora")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notificacion {

    /** Identificador único generado por la base de datos. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notificacion_id")
    private Long notificacionId;

    /** Mesa sobre la que se emite la notificación. */
    @NotNull(message = "La mesa es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mesa_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_notificacion_mesa"))
    private Mesa mesa;

    /** Empleado destinatario de la notificación. */
    @NotNull(message = "El empleado es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_notificacion_empleado"))
    private Empleado empleado;

    /**
     * Comanda asociada a la notificación.
     *
     * <p>Aplica solo a notificaciones generadas desde producción:
     * <ul>
     *   <li>{@code PLATOS_LISTOS} — comanda de cocina cuyos platos están listos para servir.</li>
     *   <li>{@code BEBIDAS_LISTAS} — comanda de barra cuyas bebidas están listas para servir.</li>
     *   <li>{@code CAMBIO} — comanda que el cliente solicitó modificar.</li>
     * </ul>
     *
     * <p>Es {@code null} para notificaciones {@code ATENCION} (solicitadas por el cliente,
     * sin relación con una comanda específica).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comanda_id",
                foreignKey = @ForeignKey(name = "fk_notificacion_comanda"))
    private Comanda comanda;

    /** Estado actual: {@code ACTIVA} si está pendiente de atención, {@code ATENDIDA} si fue resuelta. */
    @NotNull(message = "El estado de la notificación es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "notificacion_estado", nullable = false, length = 20)
    private EstadoNotificacion notificacionEstado;

    /** Motivo de la notificación; determina el mensaje mostrado al empleado. */
    @NotNull(message = "El tipo de notificación es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "notificacion_tipo", nullable = false, length = 20)
    private TipoNotificacion notificacionTipo;

    /** Fecha y hora en que se emitió la notificación; por defecto la fecha y hora de creación. */
    @NotNull
    @Column(name = "notificacion_fecha_hora", nullable = false)
    @Builder.Default
    private LocalDateTime notificacionFechaHora = LocalDateTime.now();

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (notificacionFechaHora == null) {
            notificacionFechaHora = LocalDateTime.now();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Notificacion)) return false;
        Notificacion that = (Notificacion) o;
        return notificacionId != null && notificacionId.equals(that.getNotificacionId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
