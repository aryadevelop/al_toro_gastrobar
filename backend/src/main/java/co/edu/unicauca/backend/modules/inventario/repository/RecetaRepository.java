package co.edu.unicauca.backend.modules.inventario.repository;

import co.edu.unicauca.backend.modules.inventario.entity.Receta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Acceso a datos de {@link Receta}. Proporciona la lista de insumos y
 * cantidades necesarias para preparar un producto.
 */
@Repository
public interface RecetaRepository extends JpaRepository<Receta, Receta.RecetaId> {

    /**
     * Devuelve todas las recetas asociadas a un producto, con el insumo cargado
     * para evitar consultas adicionales al ejecutar el descuento.
     *
     * @param productoId identificador del producto
     * @return lista de recetas; vacía si el producto no tiene receta registrada
     */
    @Query("SELECT r FROM Receta r JOIN FETCH r.insumo WHERE r.productoId = :productoId")
    List<Receta> findByProductoIdFetchInsumo(@Param("productoId") Long productoId);
}
