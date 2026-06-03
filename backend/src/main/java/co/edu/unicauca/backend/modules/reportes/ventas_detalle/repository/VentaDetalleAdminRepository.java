package co.edu.unicauca.backend.modules.reportes.ventas_detalle.repository;

import co.edu.unicauca.backend.modules.pagos_caja.entity.Venta;
import co.edu.unicauca.backend.shared.enums.MetodoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de venta para consultas de detalle administrativo.
 */
public interface VentaDetalleAdminRepository extends JpaRepository<Venta, Long> {

    @Query("""
            SELECT v FROM Venta v
            JOIN FETCH v.cajero c
            JOIN FETCH v.visita vi
            LEFT JOIN FETCH vi.cliente cl
            LEFT JOIN FETCH vi.reserva r
            LEFT JOIN FETCH r.decoracion d
            WHERE v.visitaId = :visitaId
            """)
    Optional<Venta> findDetalleByVisitaId(@Param("visitaId") Long visitaId);

    @Query("""
            SELECT DISTINCT v FROM Venta v
            LEFT JOIN v.visita vi
            LEFT JOIN vi.cliente cl
            WHERE (COALESCE(:ventaId, v.visitaId) = v.visitaId)
              AND (COALESCE(:desde, v.ventaFechaHora) <= v.ventaFechaHora)
              AND (COALESCE(:hasta, v.ventaFechaHora) >= v.ventaFechaHora)
              AND (COALESCE(:metodoPago, v.ventaMetodo) = v.ventaMetodo)
            ORDER BY v.ventaFechaHora ASC
            """)
    List<Venta> buscarVentasPorFiltros(@Param("ventaId") Long ventaId,
                                       @Param("desde") LocalDateTime desde,
                                       @Param("hasta") LocalDateTime hasta,
                                       @Param("metodoPago") MetodoPago metodoPago);
}
