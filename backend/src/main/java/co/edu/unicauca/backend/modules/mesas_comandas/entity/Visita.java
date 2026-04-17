package co.edu.unicauca.backend.modules.mesas_comandas.entity;

import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Estancia activa de uno o más comensales en el restaurante.
 *
 * <p>Una visita representa la ocupación física de una mesa desde que el cliente
 * se sienta hasta que cierra la cuenta. Existen dos orígenes posibles:
 * <ul>
 *   <li><strong>Con reserva</strong> — {@code reserva} no es {@code null}; la visita se crea
 *       al convertir una reserva confirmada en ocupación real.</li>
 *   <li><strong>Walk-in</strong> — {@code reserva} es {@code null}; el cliente llega sin
 *       reserva previa y el host abre la visita directamente.</li>
 * </ul>
 *
 * <p>La visita actúa como raíz de agregado para todo lo que ocurre durante la estadía:
 * la {@link Mesa} asignada, las {@link Comanda}s generadas y la {@link co.edu.unicauca.backend.modules.pagos_caja.entity.Venta}
 * al cerrar la cuenta comparten su PK o tienen FK hacia {@code visita_id}.
 *
 * <p>La visita finaliza cuando {@code visitaFechaHoraFin} recibe un valor (al cierre de cuenta);
 * mientras sea {@code null} la visita se considera activa.
 *
 * <p>Estrategia de índices:
 * <ul>
 *   <li>{@code idx_visita_cliente_id} — historial de visitas por cliente.</li>
 *   <li>{@code idx_visita_reserva_id} — verificación de si una reserva ya fue convertida.</li>
 *   <li>{@code idx_visita_fecha_inicio} — ordenamiento cronológico en reportes de ocupación.</li>
 *   <li>{@code idx_visita_fecha_fin} — filtro de visitas activas ({@code fecha_fin IS NULL}).</li>
 * </ul>
 *
 * @see Mesa
 * @see Comanda
 * @see co.edu.unicauca.backend.modules.pagos_caja.entity.Venta
 */
@Entity
@Table(name = "visita", schema = "restaurante",
       indexes = {
           @Index(name = "idx_visita_cliente_id", columnList = "cliente_id"),
           @Index(name = "idx_visita_reserva_id", columnList = "reserva_id"),
           @Index(name = "idx_visita_fecha_inicio", columnList = "visita_fecha_hora_inicio"),
           @Index(name = "idx_visita_fecha_fin", columnList = "visita_fecha_hora_fin")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Visita {

    /** Identificador único generado por la base de datos. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "visita_id")
    private Long visitaId;

    /**
     * Cliente asociado a la visita; {@code null} en visitas anónimas.
     * Obligatorio para acumular puntos de fidelización al cerrar cuenta.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", foreignKey = @ForeignKey(name = "fk_visita_cliente"))
    private Cliente cliente;

    /**
     * Reserva de origen; {@code null} si es walk-in.
     * Cuando está presente, la reserva debe estar en estado {@code CONFIRMADA}
     * antes de crear la visita.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id", foreignKey = @ForeignKey(name = "fk_visita_reserva"))
    private Reserva reserva;

    /** Fecha y hora en que el cliente ocupó la mesa; defecto: momento de creación. */
    @Column(name = "visita_fecha_hora_inicio", nullable = false)
    @Builder.Default
    private LocalDateTime visitaFechaHoraInicio = LocalDateTime.now();

    /**
     * Fecha y hora en que se cerró la cuenta y finalizó la visita.
     * {@code null} mientras la visita está activa.
     */
    @Column(name = "visita_fecha_hora_fin")
    private LocalDateTime visitaFechaHoraFin;

    @PrePersist
    protected void onCreate() {
        if (visitaFechaHoraInicio == null) {
            visitaFechaHoraInicio = LocalDateTime.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Visita)) return false;
        Visita visita = (Visita) o;
        return visitaId != null && visitaId.equals(visita.getVisitaId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
