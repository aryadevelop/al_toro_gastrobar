package co.edu.unicauca.backend.modules.produccion.repository;

import co.edu.unicauca.backend.modules.produccion.entity.Producto;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    /**
     * Productos activos para la carta (excluye menús especiales).
     * Incluye productos cuyo campo menuEspecial es false o null.
     */
    @Query("SELECT p FROM Producto p " +
           "WHERE p.productoEstado = :estado " +
           "AND (p.menuEspecial IS NULL OR p.menuEspecial = false) " +
           "ORDER BY p.categoriaCarta.orden ASC, p.productoNombre ASC")
    List<Producto> findProductosCarta(@Param("estado") EstadoGenerico estado);

    /**
     * Productos activos marcados como menú especial.
     */
    List<Producto> findByProductoEstadoAndMenuEspecialTrueOrderByProductoNombreAsc(EstadoGenerico estado);
}
