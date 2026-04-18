package co.edu.unicauca.backend.modules.mesas_comandas.repository;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de acceso a datos para la entidad {@link Zona}.
 *
 * <p>Provee las operaciones CRUD heredadas de {@code JpaRepository}.
 * La lista completa de zonas se usa en los flujos de disponibilidad y creación de reservas.
 *
 * @see Zona
 */
public interface ZonaRepository extends JpaRepository<Zona, Long> {
}
