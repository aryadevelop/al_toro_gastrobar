package co.edu.unicauca.backend.modules.mesas_comandas.entity;

import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.shared.entity.AuditableEntity;
import co.edu.unicauca.backend.shared.enums.EstacionComanda;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Orden de producción asociada a una visita en curso o a una reserva con pre-orden.
 *
 * <p>Una comanda en estado {@code PRE_RESERVA} representa la pre-orden registrada por el
 * cliente al crear una reserva. En este estado {@code visita} y {@code comanda_estacion}
 * son {@code null}; ambos se asignan cuando el cliente llega y se abre la visita.
 * Las comandas generadas en mesa durante la visita tienen {@code visita} obligatorio y
 * {@code reserva} opcional (solo si la visita proviene de una reserva).
 *
 * <p>Ciclo de vida del estado ({@link EstadoComanda}):
 * <pre>
 *   PRE_RESERVA → PENDIENTE → EN_PREPARACION → LISTO → COMPLETADO
 * </pre>
 * La transición de {@code PRE_RESERVA} a {@code PENDIENTE} ocurre al convertir la
 * pre-comanda en comanda activa al iniciar la visita. Al pasar a {@code LISTO} se
 * registra {@code comandaFechaHoraListo} para medir tiempos de producción.
 *
 * <p>Estrategia de índices:
 * <ul>
 *   <li>{@code idx_comanda_visita_id} — carga de todas las comandas de una visita.</li>
 *   <li>{@code idx_comanda_reserva_id} — localización de la pre-comanda por reserva.</li>
 *   <li>{@code idx_comanda_estacion} — filtro por estación en pantallas de producción.</li>
 *   <li>{@code idx_comanda_estado} — filtro de comandas pendientes o en preparación.</li>
 *   <li>{@code idx_comanda_fecha_inicio} — ordenamiento FIFO en la cola de producción.</li>
 *   <li>{@code idx_comanda_pendientes} — índice compuesto para la vista de cola por estación.</li>
 *   <li>{@code idx_comanda_prereserva} — índice parcial para recuperar pre-comandas activas.</li>
 * </ul>
 *
 * @see co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem
 * @see co.edu.unicauca.backend.shared.enums.EstacionComanda
 * @see co.edu.unicauca.backend.shared.enums.EstadoComanda
 */
@Entity
@Table(name = "comanda", schema = "restaurante",
       indexes = {
           @Index(name = "idx_comanda_visita_id", columnList = "visita_id"),
           @Index(name = "idx_comanda_estacion", columnList = "comanda_estacion"),
           @Index(name = "idx_comanda_estado", columnList = "comanda_estado"),
           @Index(name = "idx_comanda_fecha_inicio", columnList = "comanda_fecha_hora_inicio"),
           @Index(name = "idx_comanda_pendientes", columnList = "comanda_estacion, comanda_fecha_hora_inicio")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comanda extends AuditableEntity {

    /** Identificador único generado por la base de datos. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comanda_id")
    private Long comandaId;

    /**
     * Visita durante la cual se generó esta comanda.
     * {@code null} mientras la comanda está en estado {@code PRE_RESERVA};
     * se asigna al iniciar la visita.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visita_id",
                foreignKey = @ForeignKey(name = "fk_comanda_visita"))
    private Visita visita;

    /**
     * Reserva a la que pertenece esta pre-comanda.
     * {@code null} en comandas de walk-in o de mesa sin reserva previa.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id",
                foreignKey = @ForeignKey(name = "fk_comanda_reserva"))
    private Reserva reserva;

    /**
     * Estación de producción destino (cocina o barra).
     * {@code null} en estado {@code PRE_RESERVA}; se define al convertir la pre-comanda.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "comanda_estacion", length = 20)
    private EstacionComanda comandaEstacion;

    /** Fecha y hora en que se envió la comanda a producción. */
    @NotNull
    @Column(name = "comanda_fecha_hora_inicio", nullable = false)
    @Builder.Default
    private LocalDateTime comandaFechaHoraInicio = LocalDateTime.now();

    /**
     * Fecha y hora en que la estación marcó la comanda como lista.
     * {@code null} mientras no ha sido completada; se registra al pasar a {@code LISTO}.
     */
    @Column(name = "comanda_fecha_hora_listo")
    private LocalDateTime comandaFechaHoraListo;

    /** Instrucciones especiales para la estación de producción; {@code null} si no aplica. */
    @Column(name = "comanda_notas", columnDefinition = "TEXT")
    private String comandaNotas;

    /** Estado actual en el ciclo de preparación. */
    @NotNull(message = "El estado de la comanda es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "comanda_estado", nullable = false, length = 20)
    private EstadoComanda comandaEstado;

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (comandaFechaHoraInicio == null) {
            comandaFechaHoraInicio = LocalDateTime.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Comanda)) return false;
        Comanda comanda = (Comanda) o;
        return comandaId != null && comandaId.equals(comanda.getComandaId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
