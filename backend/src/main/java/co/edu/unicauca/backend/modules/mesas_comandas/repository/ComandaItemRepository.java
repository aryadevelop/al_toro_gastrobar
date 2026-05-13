package co.edu.unicauca.backend.modules.mesas_comandas.repository;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
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

    /**
     * Suma las cantidades comprometidas de un producto que aún no se descontaron de
     * {@code stockActual}.
     *
     * <p>"Comprometido" = ítems en comandas en estado {@code BORRADOR} o
     * {@code PENDIENTE} que NO sean parte de un menú especial. 
     * 
     * Se excluyen 
     *   -  Items en estado: EN_PREPARACION, LISTO y COMPLETADO porque ya decrementaron {@code stockActual}
     *      al transicionar PENDIENTE → EN_PREPARACION. 
     *   -  Items con {@code comandaItemMenuGrupo IS NOT NULL}: los menús especiales nunca decrementan inventario
     *
     * <p>Fórmula de disponibilidad: {@code disponible = stockActual - comprometido + cantidadAnterior}.
     *
     * @param productoId producto a evaluar
     * @return suma de cantidades; 0 si no hay ítems comprometidos
     */
    @Query("""
        SELECT COALESCE(SUM(ci.comandaItemCantidad), 0)
        FROM ComandaItem ci
        WHERE ci.producto.productoId = :productoId
        AND ci.comandaItemMenuGrupo IS NULL
        AND ci.comanda.comandaEstado IN (
            co.edu.unicauca.backend.shared.enums.EstadoComanda.BORRADOR,
            co.edu.unicauca.backend.shared.enums.EstadoComanda.PENDIENTE
        )
        """)
    Long sumCantidadComprometidaByProducto(@Param("productoId") Long productoId);

    /**
     * Devuelve todos los items de una comanda ordenados alfabéticamente por nombre de producto.
     */
    @Query("""
        SELECT ci FROM ComandaItem ci
        JOIN FETCH ci.producto p
        WHERE ci.comanda.comandaId = :comandaId
        ORDER BY p.productoNombre ASC, ci.comandaItemDescripcion ASC NULLS FIRST
        """)
    List<ComandaItem> findByComanda_ComandaIdOrderByProductoNombreAsc(@Param("comandaId") Long comandaId);

    /**
     * Devuelve todos los ítems modificados ({@code descripcion != null}) de un producto
     * en una comanda.
     *
     * @param comandaId  comanda BORRADOR
     * @param productoId producto cuyo ítem base se está eliminando
     */
    List<ComandaItem> findByComanda_ComandaIdAndProducto_ProductoIdAndComandaItemDescripcionIsNotNull(
            Long comandaId, Long productoId);

    /**
     * Devuelve todos los ítems de un producto dentro de una comanda. La capa
     * de servicio aplica una comparación normalizada sobre {@code descripcion}
     * para detectar coincidencias insensibles a mayúsculas y espacios.
     *
     * @param comandaId  identificador de la comanda
     * @param productoId identificador del producto
     * @return lista de ítems del producto en esa comanda; vacía si no hay
     */
    List<ComandaItem> findByComanda_ComandaIdAndProducto_ProductoId(Long comandaId, Long productoId);

    /**
     * Suma el total monetario acumulado de todos los ítems de la visita.
     *
     * <p>Los ítems con precio nulo (bebidas del menú especial) se ignoran y aportan
     * cero al total.
     *
     * @param visitaId   identificador de la visita
     * @return suma total; {@link BigDecimal#ZERO} si no hay ítems activos
     */
    @Query("""
        SELECT COALESCE(SUM(ci.comandaItemPrecio * ci.comandaItemCantidad), 0)
        FROM ComandaItem ci
        WHERE ci.comanda.visita.visitaId = :visitaId
        AND ci.comandaItemPrecio IS NOT NULL
        """)
    BigDecimal sumTotalActivosByVisita(@Param("visitaId") Long visitaId);

    /**
     * Calcula la cantidad total de ítems agrupada por comanda para el conjunto
     * de identificadores indicado.
     *
     * <p>El resultado se entrega como tuplas {@code [comandaId, totalCantidad]}
     * para permitir su transformación a {@link java.util.Map} en la capa de
     * servicio sin necesidad de consultas adicionales por comanda.
     *
     * @param comandaIds identificadores de comandas a consultar
     * @return lista de tuplas con la suma de cantidades por comanda; vacía
     *         cuando ninguno de los identificadores tiene ítems asociados
     */
    @Query("""
        SELECT ci.comanda.comandaId, COALESCE(SUM(ci.comandaItemCantidad), 0)
        FROM ComandaItem ci
        WHERE ci.comanda.comandaId IN :comandaIds
        GROUP BY ci.comanda.comandaId
        """)
    List<Object[]> sumCantidadByComandaIdIn(@Param("comandaIds") Collection<Long> comandaIds);
}
