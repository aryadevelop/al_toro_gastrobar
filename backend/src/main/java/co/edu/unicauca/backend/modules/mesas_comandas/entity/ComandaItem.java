package co.edu.unicauca.backend.modules.mesas_comandas.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import co.edu.unicauca.backend.modules.inventario.entity.Producto;

/**
 * Línea de producto dentro de una {@link Comanda}.
 *
 * <p>Cada fila representa un ítem solicitado por el cliente: producto, cantidad y precio
 * capturado al momento del pedido.
 *
 * <p>Si el ítem pertenece a un menú especial, las opciones de personalización seleccionadas
 * por el cliente se almacenan en {@link ComandaMenuModificacion}.
 *
 * <p>Estrategia de índices:
 * <ul>
 *   <li>{@code idx_comanda_item_comanda_id} — carga de ítems por comanda.</li>
 *   <li>{@code idx_comanda_item_producto_id} — análisis de ventas por producto.</li>
 * </ul>
 *
 * @see Comanda
 * @see ComandaMenuModificacion
 */
@Entity
@Table(name = "comanda_item", schema = "restaurante",
       indexes = {
           @Index(name = "idx_comanda_item_comanda_id", columnList = "comanda_id"),
           @Index(name = "idx_comanda_item_producto_id", columnList = "producto_id")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComandaItem {

    /** Identificador único generado por la base de datos. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comanda_item_id", nullable = false, updatable = false)
    private Long comandaItemId;

    /** Comanda a la que pertenece este ítem. */
    @NotNull(message = "La comanda es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comanda_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_comanda_item_comanda"))
    private Comanda comanda;

    /** Producto pedido; referencia al catálogo de producción. */
    @NotNull(message = "El producto es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_comanda_item_producto"))
    private Producto producto;

    /** Número de unidades pedidas; mínimo 1. */
    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    @Column(name = "comanda_item_cantidad", nullable = false)
    private Integer comandaItemCantidad;

    /**
     * Precio unitario capturado al momento del pedido.
     * No varía aunque el catálogo se actualice posteriormente.
     */
    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
    @Digits(integer = 10, fraction = 2, message = "El precio debe tener máximo 10 dígitos enteros y 2 decimales")
    @Column(name = "comanda_item_precio", nullable = false, precision = 12, scale = 2)
    private BigDecimal comandaItemPrecio;

    /** Notas adicionales del cliente para este ítem (sin cebolla, extra picante, etc.). */
    @Size(max = 500, message = "La descripción no debe exceder 500 caracteres")
    @Column(name = "comanda_item_descripcion", length = 500)
    private String comandaItemDescripcion;

    /** UUID que agrupa los ítems COCINA+BARRA de un mismo menú especial. NULL para ítems de carta. */
    @Column(name = "comanda_item_menu_grupo", length = 36)
    private String comandaItemMenuGrupo;

    /** Modificaciones de menú especial seleccionadas para este ítem; vacía si no aplica. */
    @OneToMany(mappedBy = "comandaItem", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<ComandaMenuModificacion> modificaciones = new ArrayList<>();

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ComandaItem)) return false;
        ComandaItem that = (ComandaItem) o;
        return comandaItemId != null && comandaItemId.equals(that.getComandaItemId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
