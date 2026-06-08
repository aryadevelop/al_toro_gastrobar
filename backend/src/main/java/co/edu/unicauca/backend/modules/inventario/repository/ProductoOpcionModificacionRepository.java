package co.edu.unicauca.backend.modules.inventario.repository;

import co.edu.unicauca.backend.modules.inventario.entity.OpcionModificacion;
import co.edu.unicauca.backend.modules.inventario.entity.ProductoOpcionModificacion;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductoOpcionModificacionRepository
        extends JpaRepository<ProductoOpcionModificacion, ProductoOpcionModificacion.ProductoOpcionModificacionId> {

    /**
     * Retorna las opciones de modificación activas para un menú especial dado,
     * ordenadas por tipo de componente y nombre.
     */
    @Query("SELECT pom.opcion FROM ProductoOpcionModificacion pom " +
           "WHERE pom.producto.productoId = :productoId " +
           "AND pom.opcion.opcionEstado = :estado " +
           "ORDER BY pom.opcion.tipoComponente ASC, pom.opcion.opcionNombre ASC")
    List<OpcionModificacion> findOpcionesActivasByProductoId(
            @Param("productoId") Long productoId,
            @Param("estado") EstadoGenerico estado);

    /**
     * Verifica si una opción de modificación está vinculada a un producto específico.
     * Usado para validar que las opciones enviadas en la pre-orden pertenecen al menú elegido.
     */
    @Query("SELECT COUNT(pom) > 0 FROM ProductoOpcionModificacion pom " +
           "WHERE pom.producto.productoId = :productoId " +
           "AND pom.opcion.opcionId = :opcionId")
    boolean existsByProductoIdAndOpcionId(
            @Param("productoId") Long productoId,
            @Param("opcionId") Long opcionId);
}
