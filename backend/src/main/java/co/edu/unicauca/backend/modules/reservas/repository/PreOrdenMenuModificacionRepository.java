package co.edu.unicauca.backend.modules.reservas.repository;

import co.edu.unicauca.backend.modules.reservas.entity.PreOrdenMenuModificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio de acceso a datos para la entidad {@link PreOrdenMenuModificacion}.
 *
 * <p>Provee las operaciones CRUD heredadas de {@code JpaRepository} y consultas
 * para recuperar las opciones de modificación seleccionadas por el cliente
 * para un ítem de menú especial dentro de la pre-orden.
 *
 * @see PreOrdenMenuModificacion
 * @see co.edu.unicauca.backend.modules.reservas.entity.PreOrdenDetalle
 */
public interface PreOrdenMenuModificacionRepository extends JpaRepository<PreOrdenMenuModificacion, Long> {

    /**
     * Devuelve todas las modificaciones seleccionadas para un ítem de pre-orden concreto.
     *
     * @param preordenDetalleId identificador del ítem de pre-orden
     * @return lista de modificaciones asociadas al ítem; vacía si no tiene ninguna
     */
    List<PreOrdenMenuModificacion> findByPreordenDetalle_PreordenDetalleId(Long preordenDetalleId);
}
