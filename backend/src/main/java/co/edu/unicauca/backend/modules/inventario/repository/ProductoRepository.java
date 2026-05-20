package co.edu.unicauca.backend.modules.inventario.repository;

import co.edu.unicauca.backend.modules.inventario.entity.Producto;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

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

    /**
     * Búsqueda parcial accent-insensitive y case-insensitive por nombre, excluyendo
     * menús especiales, productos inactivos.
     *
     * @param nombre fragmento de nombre a buscar (no debe ser null ni vacío)
     * @param estado estado a exigir como {@link String} (típicamente {@code "ACTIVO"})
     * @return productos coincidentes con stock &ge; 1, ordenados por nombre asc
     */
    @Query(value = """
            SELECT * FROM restaurante.Producto p
            WHERE p.producto_estado = :estado
            AND (p.menu_especial IS NULL OR p.menu_especial = false)
            AND unaccent(lower(p.producto_nombre)) LIKE '%' || unaccent(lower(trim(:nombre))) || '%'
            ORDER BY p.producto_nombre ASC
            """, nativeQuery = true)
    List<Producto> buscarPorNombreSinMenu(@Param("nombre") String nombre,
                                          @Param("estado") String estado);


    /**
     * Búsqueda parcial accent-insensitive por nombre para el formulario de ajuste
     * manual de inventario. Incluye productos con {@code stockActual = 0}.
     * Excluye menús especiales y cuyo stock no sea null.
     *
     * @param nombre fragmento de nombre a buscar
     * @param estado estado a exigir como {@link String} (típicamente {@code "ACTIVO"})
     * @return productos coincidentes ordenados por nombre asc
     */
    @Query(value = """
            SELECT * FROM restaurante.Producto p
            WHERE p.producto_estado = :estado
            AND (p.menu_especial IS NULL OR p.menu_especial = false)
            AND (p.stock_actual IS NOT NULL)
            AND unaccent(lower(p.producto_nombre)) LIKE '%' || unaccent(lower(trim(:nombre))) || '%'
            ORDER BY p.producto_nombre ASC
            """, nativeQuery = true)
    List<Producto> buscarParaAjuste(@Param("nombre") String nombre,
                                    @Param("estado") String estado);


    /**
     * Adquiere un bloqueo de escritura pesimista sobre la fila del producto indicado.
     * Usar dentro de una transacción {@code @Transactional} para serializar
     * concurrencia en operaciones de descuento de stock.
     *
     * @param id identificador del producto
     * @return producto bloqueado, o {@link Optional#empty()} si no existe
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Producto p WHERE p.productoId = :id")
    Optional<Producto> findByIdForUpdate(@Param("id") Long id);
}
