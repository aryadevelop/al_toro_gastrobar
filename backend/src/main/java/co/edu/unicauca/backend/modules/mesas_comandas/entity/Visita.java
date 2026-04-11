package co.edu.unicauca.backend.modules.mesas_comandas.entity;

import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad Visita - Registro de visitas al restaurante (con o sin reserva)
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "visita_id")
    private Long visitaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", foreignKey = @ForeignKey(name = "fk_visita_cliente"))
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id", foreignKey = @ForeignKey(name = "fk_visita_reserva"))
    private Reserva reserva;

    @Column(name = "visita_fecha_hora_inicio", nullable = false)
    @Builder.Default
    private LocalDateTime visitaFechaHoraInicio = LocalDateTime.now();

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
