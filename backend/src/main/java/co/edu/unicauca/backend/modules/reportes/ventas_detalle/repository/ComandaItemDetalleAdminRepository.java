package co.edu.unicauca.backend.modules.reportes.ventas_detalle.repository;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repositorio de items de comanda para detalle de ventas.
 */
public interface ComandaItemDetalleAdminRepository extends JpaRepository<ComandaItem, Long> {

    @Query("""
            SELECT ci FROM ComandaItem ci
            JOIN FETCH ci.producto p
            JOIN ci.comanda c
            JOIN c.visita v
            WHERE v.visitaId = :visitaId
            """)
    List<ComandaItem> findItemsByVisitaId(@Param("visitaId") Long visitaId);

    @Query("""
            SELECT ci FROM ComandaItem ci
            JOIN FETCH ci.producto p
            JOIN ci.comanda c
            JOIN c.reserva r
            WHERE r.reservaId = :reservaId
            """)
    List<ComandaItem> findItemsByReservaId(@Param("reservaId") Long reservaId);

    @Query("""
            SELECT DISTINCT ci.comanda.visita.visitaId
            FROM ComandaItem ci
            WHERE ci.comanda.visita.visitaId IN (
                SELECT v.visitaId FROM Venta v
                WHERE v.ventaFechaHora BETWEEN :inicio AND :fin
            )
            AND ci.producto.menuEspecial = true
            """)
    List<Long> findVisitasConMenuEspecialEnVentasDelDia(@Param("inicio") java.time.LocalDateTime inicio,
                                                         @Param("fin") java.time.LocalDateTime fin);

    @Query("""
            SELECT ci.producto.productoNombre, SUM(ci.comandaItemCantidad)
            FROM ComandaItem ci
            WHERE ci.comanda.visita.visitaId IN (
                SELECT v.visitaId FROM Venta v
                WHERE v.ventaFechaHora BETWEEN :inicio AND :fin
            )
            GROUP BY ci.producto.productoNombre
            ORDER BY SUM(ci.comandaItemCantidad) DESC
            """)
    List<Object[]> findTopProductosVendidosEnVentasDelDia(@Param("inicio") java.time.LocalDateTime inicio,
                                                           @Param("fin") java.time.LocalDateTime fin,
                                                           Pageable pageable);
}
