package co.edu.unicauca.backend.modules.reportes_ventas_detalle.repository;

import co.edu.unicauca.backend.modules.pagos_caja.entity.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
