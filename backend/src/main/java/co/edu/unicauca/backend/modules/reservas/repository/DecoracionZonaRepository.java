package co.edu.unicauca.backend.modules.reservas.repository;

import co.edu.unicauca.backend.modules.reservas.entity.DecoracionZona;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio de acceso a datos para la entidad {@link DecoracionZona}.
 *
 * <p>Provee las operaciones CRUD heredadas de {@code JpaRepository} para gestionar
 * la asociación muchos-a-muchos entre decoraciones y zonas del restaurante.
 * La clave primaria compuesta está representada por {@link DecoracionZona.DecoracionZonaId}.
 *
 * @see DecoracionZona
 */
public interface DecoracionZonaRepository extends JpaRepository<DecoracionZona, DecoracionZona.DecoracionZonaId> {

    /**
     * Devuelve todas las asociaciones zona-decoración para una decoración concreta.
     *
     * <p>Útil para consultar en qué zonas del restaurante está disponible
     * una decoración específica.
     *
     * @param decoracionId identificador de la decoración
     * @return lista de asociaciones para la decoración indicada; vacía si no tiene zonas asignadas
     */
    List<DecoracionZona> findByDecoracionId(Long decoracionId);

    /**
     * Elimina todas las asociaciones de zonas para la decoración indicada.
     *
     * @param decoracionId identificador de la decoración
     */
    void deleteByDecoracionId(Long decoracionId);
}
