package co.edu.unicauca.backend.modules.mesas_comandas.repository;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio de acceso a datos para la entidad {@link ComandaItem}.
 *
 * <p>Provee las operaciones CRUD heredadas de {@code JpaRepository} y la consulta
 * para obtener todas las líneas de producto de una comanda específica.
 *
 * @see ComandaItem
 */
public interface ComandaItemRepository extends JpaRepository<ComandaItem, Long> {

    /**
     * Devuelve todos los ítems de una comanda.
     *
     * @param comandaId identificador de la comanda
     * @return lista de items de la comanda; vacía si la comanda no tiene ítems
     */
    List<ComandaItem> findByComanda_ComandaId(Long comandaId);
}
