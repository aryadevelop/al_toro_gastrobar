package co.edu.unicauca.backend.modules.reportes.ventas_detalle.repository;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
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
}
