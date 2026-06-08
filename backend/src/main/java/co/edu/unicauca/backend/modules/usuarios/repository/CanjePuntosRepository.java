package co.edu.unicauca.backend.modules.usuarios.repository;

import co.edu.unicauca.backend.modules.usuarios.entity.CanjePuntos;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de acceso a datos para la entidad {@link CanjePuntos}.
 *
 * <p>Provee las operaciones CRUD heredadas de {@code JpaRepository}.
 * Los registros de canje se crean exclusivamente desde {@link co.edu.unicauca.backend.modules.usuarios.service.PuntosService}
 * como auditoría inmutable al ejecutar un canje de puntos.
 *
 * @see CanjePuntos
 */
public interface CanjePuntosRepository extends JpaRepository<CanjePuntos, Long> {
}
