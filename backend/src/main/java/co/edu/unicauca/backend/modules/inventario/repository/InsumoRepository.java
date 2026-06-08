package co.edu.unicauca.backend.modules.inventario.repository;

import co.edu.unicauca.backend.modules.inventario.entity.Insumo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad {@link Insumo}.
 */
@Repository
public interface InsumoRepository extends JpaRepository<Insumo, Long> {

    /**
     * Búsqueda parcial accent-insensitive y case-insensitive por nombre de insumos
     * activos.
     *
     * @param nombre fragmento de nombre a buscar
     * @param estado estado a exigir como {@link String} (típicamente {@code "ACTIVO"})
     * @return insumos coincidentes ordenados por nombre asc
     */
    @Query(value = """
            SELECT * FROM restaurante.Insumo i
            WHERE i.insumo_estado = :estado
            AND unaccent(lower(i.insumo_nombre)) LIKE '%' || unaccent(lower(:nombre)) || '%'
            ORDER BY i.insumo_nombre ASC
            """, nativeQuery = true)
            
    List<Insumo> buscarPorNombreActivo(@Param("nombre") String nombre,
                                       @Param("estado") String estado);

    /**
     * Adquiere un bloqueo de escritura pesimista sobre la fila del insumo indicado.
     *
     * @param id identificador del insumo
     * @return insumo bloqueado, o {@link Optional#empty()} si no existe
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Insumo i WHERE i.insumoId = :id")
    Optional<Insumo> findByIdForUpdate(@Param("id") Long id);
}
