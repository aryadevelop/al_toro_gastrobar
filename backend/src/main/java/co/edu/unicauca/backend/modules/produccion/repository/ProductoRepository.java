package co.edu.unicauca.backend.modules.produccion.repository;

import co.edu.unicauca.backend.modules.produccion.entity.Producto;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repositorio JPA para la entidad {@link Producto}.
 *
 * <p>Extiende {@link JpaRepository} para las operaciones CRUD estándar y define
 * consultas personalizadas para las dos vistas principales del catálogo:
 * <ul>
 *   <li>Carta de platos y bebidas (excluye menús especiales).</li>
 *   <li>Menús especiales disponibles para grupos grandes.</li>
 * </ul>
 */
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    /**
     * Retorna los productos activos de la carta, excluyendo los menús especiales.
     *
     * <p>Incluye productos cuyo campo {@code menuEspecial} es {@code false} o {@code null}.
     * Los resultados se ordenan por el campo {@code orden} de la categoría ascendente y
     * luego por nombre de producto ascendente.
     *
     * @param estado estado de los productos a filtrar (normalmente {@code ACTIVO})
     * @return lista de productos activos de la carta ordenada por categoría y nombre
     */
    @Query("SELECT p FROM Producto p " +
           "WHERE p.productoEstado = :estado " +
           "AND (p.menuEspecial IS NULL OR p.menuEspecial = false) " +
           "ORDER BY p.categoriaCarta.orden ASC, p.productoNombre ASC")
    List<Producto> findProductosCarta(@Param("estado") EstadoGenerico estado);

    /**
     * Retorna los productos activos marcados como menú especial, ordenados
     * alfabéticamente por nombre ascendente.
     *
     * @param estado estado de los productos a filtrar (normalmente {@code ACTIVO})
     * @return lista de menús especiales activos ordenada por nombre
     */
    List<Producto> findByProductoEstadoAndMenuEspecialTrueOrderByProductoNombreAsc(EstadoGenerico estado);
}
