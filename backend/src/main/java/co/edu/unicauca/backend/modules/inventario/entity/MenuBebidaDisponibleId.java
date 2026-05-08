package co.edu.unicauca.backend.modules.inventario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class MenuBebidaDisponibleId implements Serializable {

    @Column(name = "producto_menu_id")
    private Long productoMenuId;

    @Column(name = "producto_bebida_id")
    private Long productoBebidaId;

    public MenuBebidaDisponibleId() {}

    public MenuBebidaDisponibleId(Long productoMenuId, Long productoBebidaId) {
        this.productoMenuId = productoMenuId;
        this.productoBebidaId = productoBebidaId;
    }

    public Long getProductoMenuId() { return productoMenuId; }
    public void setProductoMenuId(Long v) { this.productoMenuId = v; }
    public Long getProductoBebidaId() { return productoBebidaId; }
    public void setProductoBebidaId(Long v) { this.productoBebidaId = v; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MenuBebidaDisponibleId that)) return false;
        return Objects.equals(productoMenuId, that.productoMenuId)
            && Objects.equals(productoBebidaId, that.productoBebidaId);
    }

    @Override
    public int hashCode() { return Objects.hash(productoMenuId, productoBebidaId); }
}
