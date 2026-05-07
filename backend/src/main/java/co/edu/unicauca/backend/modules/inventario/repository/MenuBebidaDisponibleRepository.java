package co.edu.unicauca.backend.modules.inventario.repository;

import co.edu.unicauca.backend.modules.inventario.entity.MenuBebidaDisponible;
import co.edu.unicauca.backend.modules.inventario.entity.MenuBebidaDisponibleId;
import co.edu.unicauca.backend.modules.produccion.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuBebidaDisponibleRepository
        extends JpaRepository<MenuBebidaDisponible, MenuBebidaDisponibleId> {

    @Query("""
        SELECT mbd.bebida
          FROM MenuBebidaDisponible mbd
         WHERE mbd.id.productoMenuId = :menuId
           AND mbd.bebida.productoEstado = co.edu.unicauca.backend.shared.enums.EstadoGenerico.ACTIVO
         ORDER BY mbd.bebida.productoNombre
        """)
    List<Producto> findBebidasByMenuId(@Param("menuId") Long menuId);

    boolean existsByIdProductoMenuIdAndIdProductoBebidaId(Long menuId, Long bebidaId);

    default boolean existsByMenuIdAndBebidaId(Long menuId, Long bebidaId) {
        return existsByIdProductoMenuIdAndIdProductoBebidaId(menuId, bebidaId);
    }
}
