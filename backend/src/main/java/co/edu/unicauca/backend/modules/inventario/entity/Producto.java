package co.edu.unicauca.backend.modules.inventario.entity;

import co.edu.unicauca.backend.shared.entity.AuditableEntity;
import co.edu.unicauca.backend.shared.enums.CategoriaProducto;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import co.edu.unicauca.backend.shared.enums.TipoProducto;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Entidad que representa un producto del catálogo del restaurante.
 *
 * <p>Un producto puede ser un plato, una bebida u otro artículo de la carta.
 * Cuando el campo {@code menuEspecial} es {@code true}, el producto forma parte
 * de los menús especiales disponibles para grupos de más de 10 personas y expone
 * opciones de modificación por tipo de componente.
 *
 * <p>Los productos se organizan bajo una {@link CategoriaCarta} y se clasifican
 * adicionalmente mediante los enumerados {@link co.edu.unicauca.backend.shared.enums.TipoProducto}
 * y {@link co.edu.unicauca.backend.shared.enums.CategoriaProducto}.
 *
 */
@Entity
@Table(name = "producto", schema = "restaurante",
       indexes = {
           @Index(name = "idx_producto_categoria_id", columnList = "categoriacarta_id"),
           @Index(name = "idx_producto_estado", columnList = "producto_estado"),
           @Index(name = "idx_producto_tipo", columnList = "producto_tipo"),
           @Index(name = "idx_producto_categoria", columnList = "producto_categoria"),
           @Index(name = "idx_producto_nombre", columnList = "producto_nombre"),
           @Index(name = "idx_producto_activos", columnList = "categoriacarta_id, producto_nombre")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto extends AuditableEntity {

    /** Identificador único del producto. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "producto_id")
    private Long productoId;

    /** Categoría de carta a la que pertenece el producto; obligatoria. */
    @NotNull(message = "La categoría de carta es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoriacarta_id", nullable = false, foreignKey = @ForeignKey(name = "fk_producto_categoria"))
    private CategoriaCarta categoriaCarta;

    /** Nombre del producto (máximo 100 caracteres); obligatorio. */
    @NotBlank(message = "El nombre del producto es obligatorio")
    @Size(max = 100, message = "El nombre no debe exceder 100 caracteres")
    @Column(name = "producto_nombre", nullable = false, length = 100)
    private String productoNombre;

    /**
     * Estado de disponibilidad del producto (p. ej. {@code ACTIVO}, {@code INACTIVO}).
     * Obligatorio; controla si el producto aparece en la carta.
     */
    @NotNull(message = "El estado del producto es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "producto_estado", nullable = false, length = 20)
    private EstadoGenerico productoEstado;

    /**
     * Precio unitario del producto; no puede ser negativo.
     * Máximo 10 dígitos enteros y 2 decimales; obligatorio.
     */
    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
    @Digits(integer = 10, fraction = 2, message = "El precio debe tener máximo 10 dígitos enteros y 2 decimales")
    @Column(name = "producto_precio", nullable = false, precision = 12, scale = 2)
    private BigDecimal productoPrecio;

    /**
     * Tipo de producto según su naturaleza culinaria
     * (ver {@link co.edu.unicauca.backend.shared.enums.TipoProducto}); obligatorio.
     */
    @NotNull(message = "El tipo de producto es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "producto_tipo", nullable = false, length = 20)
    private TipoProducto productoTipo;

    /**
     * Categoría de producto para clasificación en carta y pre-orden
     * (ver {@link co.edu.unicauca.backend.shared.enums.CategoriaProducto}); obligatoria.
     */
    @NotNull(message = "La categoría de producto es obligatoria")
    @Enumerated(EnumType.STRING)
    @Column(name = "producto_categoria", nullable = false, length = 20)
    private CategoriaProducto productoCategoria;

    /** Descripción del producto (máximo 500 caracteres); {@code null} si no está registrada. */
    @Size(max = 500, message = "La descripción no debe exceder 500 caracteres")
    @Column(name = "producto_descripcion", length = 500)
    private String productoDescripcion;

    /**
     * Indica si el producto forma parte de los menús especiales para grupos de más de 10 personas.
     * {@code true} lo incluye en la consulta de menús especiales; {@code null} equivale a {@code false}.
     */
    @Column(name = "menu_especial")
    private Boolean menuEspecial;

    /**
     * Stock actual del producto en inventario; {@code null} si no se gestiona stock.
     * No puede ser negativo; máximo 9 dígitos enteros y 3 decimales.
     */
    @DecimalMin(value = "0.000", message = "El stock no puede ser negativo")
    @Digits(integer = 9, fraction = 3, message = "El stock debe tener máximo 9 dígitos enteros y 3 decimales")
    @Column(name = "stock_actual", precision = 12, scale = 3)
    private BigDecimal stockActual;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Producto)) return false;
        Producto producto = (Producto) o;
        return productoId != null && productoId.equals(producto.getProductoId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
