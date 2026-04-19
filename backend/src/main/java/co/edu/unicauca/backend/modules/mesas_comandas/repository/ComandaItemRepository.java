package co.edu.unicauca.backend.modules.mesas_comandas.repository;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
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

    /**
     * Calcula el total monetario del pre-orden de una reserva directamente en la base de datos.
     *
     * <p>Multiplica {@code productoPrecio × comandaItemCantidad} por cada ítem y suma el resultado.
     * {@code COALESCE} garantiza retornar {@code 0} si la reserva no tiene ítems en su comanda.</p>
     *
     * <p><strong>Nota de diseño:</strong> se usa {@code i.producto.productoPrecio} (precio actual
     * del catálogo) en lugar de {@code i.comandaItemPrecio} (precio capturado al pedido),
     * por especificación del pre-orden. No modificar sin revisar el spec.</p>
     *
     * @param reservaId identificador de la reserva cuya comanda pre-orden se totaliza
     * @return total monetario del pre-orden; {@code 0} si no hay ítems
     */
    @Query("SELECT COALESCE(SUM(i.producto.productoPrecio * i.comandaItemCantidad), 0) " +
           "FROM ComandaItem i WHERE i.comanda.reserva.reservaId = :reservaId")
    BigDecimal sumTotalPreOrdenByReservaId(@Param("reservaId") Long reservaId);
}
