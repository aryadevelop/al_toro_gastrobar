package co.edu.unicauca.backend.modules.mesas_comandas.entity;

import co.edu.unicauca.backend.shared.entity.AuditableEntity;
import co.edu.unicauca.backend.shared.enums.EstacionComanda;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad Comanda - Comandas de cocina o barra
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comanda_id")
    private Long comandaId;

    @NotNull(message = "La visita es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visita_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_comanda_visita"))
    private Visita visita;

    @NotNull(message = "La estación es obligatoria")
    @Enumerated(EnumType.STRING)
    @Column(name = "comanda_estacion", nullable = false, length = 20)
    private EstacionComanda comandaEstacion;

    @NotNull
    @Column(name = "comanda_fecha_hora_inicio", nullable = false)
    @Builder.Default
    private LocalDateTime comandaFechaHoraInicio = LocalDateTime.now();

    @Column(name = "comanda_fecha_hora_listo")
    private LocalDateTime comandaFechaHoraListo;

    @Column(name = "comanda_notas", columnDefinition = "TEXT")
    private String comandaNotas;

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
