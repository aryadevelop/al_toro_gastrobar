package co.edu.unicauca.backend.modules.reservas.entity;

import co.edu.unicauca.backend.shared.entity.AuditableEntity;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import co.edu.unicauca.backend.shared.enums.TipoReserva;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad principal que representa una reserva realizada por un cliente.
 *
 * <p>Una reserva agrupa la solicitud de fecha, zona y decoración del cliente,
 * junto con la pre-orden de productos opcionales. Su ciclo de vida se gestiona
 * a través del campo {@code reservaEstado}.
 *
 * <p>Reglas de uso:
 * <ul>
 *   <li>{@code zona} y {@code decoracion} son opcionales; {@code null} indica que el
 *       cliente no realizó preferencia.</li>
 *   <li>{@code reservaNotas} es opcional; {@code null} si no hay observaciones.</li>
 *   <li>{@code reservaNumeroPersonas} debe ser al menos {@code 1}; obligatorio.</li>
 *   <li>{@code reservaFechaCreacion} se asigna automáticamente en {@code @PrePersist}.</li>
 *   <li>Los ítems de pre-orden se almacenan en {@link PreOrdenDetalle}.</li>
 * </ul>
 *
 * @see PreOrdenDetalle
 * @see co.edu.unicauca.backend.shared.enums.EstadoReserva
 * @see co.edu.unicauca.backend.shared.enums.TipoReserva
 */
@Entity
@Table(name = "reserva", schema = "restaurante",
       indexes = {
           @Index(name = "idx_reserva_cliente_id", columnList = "cliente_id"),
           @Index(name = "idx_reserva_fecha_hora_llegada", columnList = "reserva_fecha_hora_llegada"),
           @Index(name = "idx_reserva_estado", columnList = "reserva_estado"),
           @Index(name = "idx_reserva_fecha_creacion", columnList = "reserva_fecha_creacion"),
           @Index(name = "idx_reserva_zona_id", columnList = "zona_id"),
           @Index(name = "idx_reserva_fecha_estado", columnList = "reserva_fecha_hora_llegada, reserva_estado")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reserva extends AuditableEntity {

    /** Identificador único de la reserva. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reserva_id")
    private Long reservaId;

    /** Cliente que realizó la reserva; obligatorio. */
    @NotNull(message = "El cliente es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_reserva_cliente"))
    private Cliente cliente;

    /** Zona del restaurante preferida por el cliente; {@code null} si no se especificó. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zona_id", foreignKey = @ForeignKey(name = "fk_reserva_zona"))
    private Zona zona;

    /** Decoración seleccionada por el cliente; {@code null} si no se solicitó decoración. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decoracion_id", foreignKey = @ForeignKey(name = "fk_reserva_decoracion"))
    private Decoracion decoracion;

    /**
     * Fecha y hora en que el cliente planea llegar al restaurante.
     * Debe ser una fecha/hora futura al momento de crear la reserva; obligatorio.
     */
    @NotNull(message = "La fecha y hora de llegada es obligatoria")
    @Column(name = "reserva_fecha_hora_llegada", nullable = false)
    private LocalDateTime reservaFechaHoraLlegada;

    /**
     * Número de comensales para la reserva.
     * Valor mínimo: {@code 1}; obligatorio.
     */
    @NotNull(message = "El número de personas es obligatorio")
    @Min(value = 1, message = "El número de personas debe ser al menos 1")
    @Column(name = "reserva_numero_personas", nullable = false)
    private Integer reservaNumeroPersonas;

    /** Observaciones o peticiones especiales del cliente; {@code null} si no hay notas. */
    @Column(name = "reserva_notas", columnDefinition = "TEXT")
    private String reservaNotas;

    /**
     * Estado actual del ciclo de vida de la reserva (p. ej. {@code PENDIENTE}, {@code CONFIRMADA}).
     * Obligatorio; gestionado por la capa de servicio.
     */
    @NotNull(message = "El estado de la reserva es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "reserva_estado", nullable = false, length = 20)
    private EstadoReserva reservaEstado;

    /**
     * Tipo de reserva: {@code BASICA} sin pre-orden o {@code CON_PREORDEN} con productos solicitados.
     * Obligatorio; determinado en la creación según si el cliente incluyó pre-orden.
     */
    @NotNull(message = "El tipo de reserva es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "reserva_tipo", nullable = false, length = 20)
    private TipoReserva reservaTipo;

    /** Fecha y hora en que se registró la reserva en el sistema; asignada automáticamente. */
    @NotNull
    @Column(name = "reserva_fecha_creacion", nullable = false)
    @Builder.Default
    private LocalDateTime reservaFechaCreacion = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (reservaFechaCreacion == null) {
            reservaFechaCreacion = LocalDateTime.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Reserva)) return false;
        Reserva reserva = (Reserva) o;
        return reservaId != null && reservaId.equals(reserva.getReservaId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
