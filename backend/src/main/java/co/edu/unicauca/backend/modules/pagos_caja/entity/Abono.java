package co.edu.unicauca.backend.modules.pagos_caja.entity;

import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.shared.enums.MetodoPago;
import co.edu.unicauca.backend.shared.enums.TipoAbono;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Movimiento de dinero asociado a una reserva: anticipo o devolución.
 *
 * <p>Registra dos flujos posibles según {@link TipoAbono}:
 * <ul>
 *   <li>{@code ANTICIPO} — pago parcial realizado por el cliente al crear o confirmar
 *       la reserva. Reduce el saldo pendiente al momento de cerrar cuenta.</li>
 *   <li>{@code DEVOLUCION} — reintegro al cliente cuando la reserva se cancela
 *       después de haber recibido anticipos.</li>
 * </ul>
 *
 * <p>Cada reserva puede tener múltiples abonos a lo largo de su ciclo de vida.
 * El total de anticipos activos menos devoluciones determina el saldo ya pagado.
 *
 * <p>Estrategia de índices:
 * <ul>
 *   <li>{@code idx_abono_reserva_id} — suma de abonos por reserva en el flujo de caja.</li>
 *   <li>{@code idx_abono_cajero_id} — auditoría de movimientos por cajero en turno.</li>
 *   <li>{@code idx_abono_fecha_hora} — ordenamiento cronológico en historial de pagos.</li>
 * </ul>
 *
 * @see co.edu.unicauca.backend.shared.enums.TipoAbono
 */
@Entity
@Table(name = "abono", schema = "restaurante",
       indexes = {
           @Index(name = "idx_abono_reserva_id", columnList = "reserva_id"),
           @Index(name = "idx_abono_cajero_id", columnList = "cajero_id"),
           @Index(name = "idx_abono_fecha_hora", columnList = "abono_fecha_hora")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Abono {

    /** Identificador único generado por la base de datos. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "abono_id")
    private Long abonoId;

    /** Empleado de caja que registró el movimiento. */
    @NotNull(message = "El cajero es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cajero_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_abono_cajero"))
    private Empleado cajero;

    /** Reserva a la que pertenece este movimiento de dinero. */
    @NotNull(message = "La reserva es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_abono_reserva"))
    private Reserva reserva;

    /** Importe del movimiento en moneda local; siempre positivo (mayor a 0). */
    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    @Digits(integer = 10, fraction = 2, message = "El monto debe tener máximo 10 dígitos enteros y 2 decimales")
    @Column(name = "abono_monto", nullable = false, precision = 12, scale = 2)
    private BigDecimal abonoMonto;

    /** Fecha y hora exacta del movimiento; defecto: momento de creación. */
    @NotNull
    @Column(name = "abono_fecha_hora", nullable = false)
    @Builder.Default
    private LocalDateTime abonoFechaHora = LocalDateTime.now();

    /** Medio de pago usado para el anticipo o canal de devolución. */
    @NotNull(message = "El método de pago es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "abono_metodo", nullable = false, length = 20)
    private MetodoPago abonoMetodo;

    /** Discrimina si el movimiento es un anticipo del cliente o una devolución. */
    @NotNull(message = "El tipo de abono es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "abono_tipo", nullable = false, length = 20)
    private TipoAbono abonoTipo;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (abonoFechaHora == null) {
            abonoFechaHora = LocalDateTime.now();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Abono)) return false;
        Abono abono = (Abono) o;
        return abonoId != null && abonoId.equals(abono.getAbonoId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
