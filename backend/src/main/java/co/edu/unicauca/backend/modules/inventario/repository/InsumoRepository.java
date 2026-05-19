package co.edu.unicauca.backend.modules.inventario.repository;

import co.edu.unicauca.backend.modules.inventario.entity.Insumo;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio de acceso a datos para la entidad {@link Insumo}.
 */
@Repository
public interface InsumoRepository extends JpaRepository<Insumo, Long> {

    /**
     * Búsqueda parcial case-insensitive por nombre de insumos en el estado
     * indicado, ordenados alfabéticamente por nombre.
     *
     * @param nombre fragmento de nombre a buscar
     * @param estado estado a exigir (típicamente {@code ACTIVO})
     * @return insumos coincidentes ordenados por nombre asc
     */
    @Query("SELECT i FROM Insumo i " +
           "WHERE i.insumoEstado = :estado " +
           "AND LOWER(i.insumoNombre) LIKE LOWER(CONCAT('%', :nombre, '%')) " +
           "ORDER BY i.insumoNombre ASC")
    List<Insumo> buscarPorNombreActivo(@Param("nombre") String nombre,
                                       @Param("estado") EstadoGenerico estado);
}
