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
 * Entidad Reserva - Reservas de clientes (básicas y especiales)
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reserva_id")
    private Long reservaId;

    @NotNull(message = "El cliente es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false, 
                foreignKey = @ForeignKey(name = "fk_reserva_cliente"))
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zona_id", foreignKey = @ForeignKey(name = "fk_reserva_zona"))
    private Zona zona;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decoracion_id", foreignKey = @ForeignKey(name = "fk_reserva_decoracion"))
    private Decoracion decoracion;

    @NotNull(message = "La fecha y hora de llegada es obligatoria")
    @Column(name = "reserva_fecha_hora_llegada", nullable = false)
    private LocalDateTime reservaFechaHoraLlegada;

    @NotNull(message = "El número de personas es obligatorio")
    @Min(value = 1, message = "El número de personas debe ser al menos 1")
    @Column(name = "reserva_numero_personas", nullable = false)
    private Integer reservaNumeroPersonas;

    @Column(name = "reserva_notas", columnDefinition = "TEXT")
    private String reservaNotas;

    @NotNull(message = "El estado de la reserva es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "reserva_estado", nullable = false, length = 20)
    private EstadoReserva reservaEstado;

    @NotNull(message = "El tipo de reserva es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "reserva_tipo", nullable = false, length = 20)
    private TipoReserva reservaTipo;

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
