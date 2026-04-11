package co.edu.unicauca.backend.modules.pagos_caja.entity;

import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.shared.enums.MetodoPago;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad Venta - Registro de ventas por visita
 */
@Entity
@Table(name = "venta", schema = "restaurante",
       indexes = {
           @Index(name = "idx_venta_cajero_id", columnList = "cajero_id"),
           @Index(name = "idx_venta_fecha_hora", columnList = "venta_fecha_hora"),
           @Index(name = "idx_venta_metodo", columnList = "venta_metodo")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Venta {

    @Id
    @Column(name = "visita_id")
    private Long visitaId;

    @NotNull(message = "La visita es obligatoria")
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "visita_id", foreignKey = @ForeignKey(name = "fk_venta_visita"))
    private Visita visita;

    @NotNull(message = "El cajero es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cajero_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_venta_cajero"))
    private Empleado cajero;

    @NotNull
    @Column(name = "venta_fecha_hora", nullable = false)
    @Builder.Default
    private LocalDateTime ventaFechaHora = LocalDateTime.now();

    @NotNull(message = "El subtotal es obligatorio")
    @DecimalMin(value = "0.00", message = "El subtotal no puede ser negativo")
    @Digits(integer = 10, fraction = 2, message = "El subtotal debe tener máximo 10 dígitos enteros y 2 decimales")
    @Column(name = "venta_subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal ventaSubtotal;

    @NotNull
    @DecimalMin(value = "0.00", message = "El descuento no puede ser negativo")
    @Digits(integer = 10, fraction = 2, message = "El descuento debe tener máximo 10 dígitos enteros y 2 decimales")
    @Column(name = "venta_descuento", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal ventaDescuento = BigDecimal.ZERO;

    @NotNull(message = "El total es obligatorio")
    @DecimalMin(value = "0.00", message = "El total no puede ser negativo")
    @Digits(integer = 10, fraction = 2, message = "El total debe tener máximo 10 dígitos enteros y 2 decimales")
    @Column(name = "venta_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal ventaTotal;

    @NotNull(message = "El método de pago es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "venta_metodo", nullable = false, length = 20)
    private MetodoPago ventaMetodo;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (ventaFechaHora == null) {
            ventaFechaHora = LocalDateTime.now();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Venta)) return false;
        Venta venta = (Venta) o;
        return visitaId != null && visitaId.equals(venta.getVisitaId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
