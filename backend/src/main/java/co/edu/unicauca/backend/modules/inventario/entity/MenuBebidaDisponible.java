package co.edu.unicauca.backend.modules.inventario.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Asociación M:N entre un menú especial y las bebidas que puede llevar.
 */
@Entity
@Table(name = "menu_bebida_disponible", schema = "restaurante")
public class MenuBebidaDisponible {

    @EmbeddedId
    private MenuBebidaDisponibleId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productoMenuId")
    @JoinColumn(name = "producto_menu_id")
    private Producto menu;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productoBebidaId")
    @JoinColumn(name = "producto_bebida_id")
    private Producto bebida;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { this.createdAt = LocalDateTime.now(); }

    public MenuBebidaDisponibleId getId() { return id; }
    public void setId(MenuBebidaDisponibleId id) { this.id = id; }
    public Producto getMenu() { return menu; }
    public void setMenu(Producto m) { this.menu = m; }
    public Producto getBebida() { return bebida; }
    public void setBebida(Producto b) { this.bebida = b; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
