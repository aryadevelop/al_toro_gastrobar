package co.edu.unicauca.backend.modules.inventario.repository;

import co.edu.unicauca.backend.modules.inventario.entity.OpcionModificacion;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio de acceso a datos para la entidad {@link OpcionModificacion}.
 *
 * <p>Provee las operaciones CRUD heredadas de {@code JpaRepository} y la consulta
 * de opciones por estado, usada para cargar los modificadores disponibles al
 * construir la carta de menús especiales.
 *
 * @see OpcionModificacion
 */
public interface OpcionModificacionRepository extends JpaRepository<OpcionModificacion, Long> {

    /**
     * Devuelve las opciones de modificación con el estado indicado, ordenadas por tipo de
     * componente y luego por nombre de opción, ambos de forma ascendente.
     *
     * @param estado estado por el que filtrar ({@code ACTIVO} o {@code INACTIVO})
     * @return lista de opciones ordenada; vacía si no hay ninguna con ese estado
     */
    List<OpcionModificacion> findByOpcionEstadoOrderByTipoComponenteAscOpcionNombreAsc(EstadoGenerico estado);
}
