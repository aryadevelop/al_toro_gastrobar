package co.edu.unicauca.backend.modules.usuarios.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Auditoría de canjes de puntos de fidelización.
 *
 * <p>Cada fila registra un canje ejecutado por un empleado (CAJERO o ADMIN):
 * cuántos puntos tenía el cliente, quién los canjeó y cuándo.
 * La tabla crece solo con inserts; nunca se actualiza ni elimina.
 */
@Entity
@Table(name = "canje_puntos", schema = "restaurante",
       indexes = {
           @Index(name = "idx_canje_cliente_id",  columnList = "cliente_id"),
           @Index(name = "idx_canje_empleado_id", columnList = "empleado_id"),
           @Index(name = "idx_canje_fecha_hora",  columnList = "canje_fecha_hora")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CanjePuntos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "canje_id")
    private Long canjeId;

    /** Cliente cuyos puntos fueron canjeados. */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_canje_cliente"))
    private Cliente cliente;

    /** Empleado que ejecutó el canje. */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_canje_empleado"))
    private Empleado empleado;

    /** Puntos que tenía el cliente en el momento del canje; siempre &gt; 0. */
    @NotNull
    @Min(value = 1, message = "Los puntos canjeados deben ser al menos 1")
    @Column(name = "canje_puntos_canjeados", nullable = false)
    private Integer canjePuntosCanjeados;

    /** Fecha y hora en que se realizó el canje. */
    @NotNull
    @Column(name = "canje_fecha_hora", nullable = false)
    @Builder.Default
    private LocalDateTime canjeFechaHora = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (canjeFechaHora == null) canjeFechaHora = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CanjePuntos)) return false;
        CanjePuntos that = (CanjePuntos) o;
        return canjeId != null && canjeId.equals(that.getCanjeId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
