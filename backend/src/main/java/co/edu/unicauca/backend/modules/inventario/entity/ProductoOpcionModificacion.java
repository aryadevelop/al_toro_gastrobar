package co.edu.unicauca.backend.modules.inventario.entity;

import co.edu.unicauca.backend.modules.produccion.entity.Producto;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entidad ProductoOpcionModificacion — tabla linking entre Producto (menuEspecial=true)
 * y OpcionModificacion. Define qué opciones de modificación están disponibles para
 * cada menú especial. Al consultar un menú, sus opciones se agrupan por
 * tipo_componente para mostrar checkboxes en el formulario de pre-orden.
 */
@Entity
@Table(name = "producto_opcion_modificacion", schema = "restaurante",
       indexes = {
           @Index(name = "idx_prod_opcion_producto_id", columnList = "producto_id")
       })
@IdClass(ProductoOpcionModificacion.ProductoOpcionModificacionId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoOpcionModificacion {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_prod_opcion_producto"))
    private Producto producto;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opcion_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_prod_opcion_opcion"))
    private OpcionModificacion opcion;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class ProductoOpcionModificacionId implements Serializable {
        private Long producto;
        private Long opcion;
    }
}
