package co.edu.unicauca.backend.modules.mesas_comandas.repository;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaMenuModificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio de acceso a datos para la entidad {@link ComandaMenuModificacion}.
 *
 * <p>Provee las operaciones CRUD heredadas de {@code JpaRepository} y la consulta
 * para recuperar todas las opciones de menú especial asociadas a un ítem de comanda.
 *
 * @see ComandaMenuModificacion
 */
public interface ComandaMenuModificacionRepository extends JpaRepository<ComandaMenuModificacion, Long> {

    /**
     * Devuelve todas las modificaciones de menú especial asociadas a un detalle de comanda.
     *
     * @param comandaItemId identificador del item de comanda
     * @return lista de modificaciones; vacía si el ítem no tiene modificaciones
     */
    List<ComandaMenuModificacion> findByComandaItem_ComandaItemId(Long comandaItemId);

    /**
     * Elimina todas las modificaciones asociadas a un ítem de comanda.
     *
     * <p>Usado al reemplazar la pre-orden de una reserva durante una modificación.
     *
     * @param comandaItemId identificador del item de comanda
     */
    void deleteByComandaItem_ComandaItemId(Long comandaItemId);
}
