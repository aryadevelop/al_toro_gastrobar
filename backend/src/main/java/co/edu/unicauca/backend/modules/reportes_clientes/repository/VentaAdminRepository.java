package co.edu.unicauca.backend.modules.reportes_clientes.repository;

import co.edu.unicauca.backend.modules.pagos_caja.entity.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de ventas por cliente para reportes administrativos.
 */
public interface VentaAdminRepository extends JpaRepository<Venta, Long> {

    @Query("""
            SELECT v FROM Venta v
            JOIN FETCH v.cajero c
            JOIN v.visita vi
            JOIN vi.cliente cl
            WHERE cl.usuarioId = :clienteId
            ORDER BY v.ventaFechaHora DESC
            """)
    List<Venta> findByClienteIdOrderByVentaFechaHoraDesc(@Param("clienteId") Long clienteId);

    Optional<Venta> findFirstByVisita_Cliente_UsuarioIdOrderByVentaFechaHoraDesc(Long clienteId);

    @Query(value = """
            SELECT EXTRACT(YEAR FROM v.venta_fecha_hora) AS anio,
                   SUM(v.venta_total) AS total,
                   COUNT(*) AS cantidad
            FROM restaurante.venta v
            JOIN restaurante.visita vi ON vi.visita_id = v.visita_id
            WHERE vi.cliente_id = :clienteId
            GROUP BY EXTRACT(YEAR FROM v.venta_fecha_hora)
            ORDER BY anio DESC
            """, nativeQuery = true)
    List<VentaAgrupadaAnioView> obtenerTotalesPorAnio(@Param("clienteId") Long clienteId);

    @Query(value = """
            SELECT EXTRACT(YEAR FROM v.venta_fecha_hora) AS anio,
                   EXTRACT(MONTH FROM v.venta_fecha_hora) AS mes,
                   SUM(v.venta_total) AS total,
                   COUNT(*) AS cantidad
            FROM restaurante.venta v
            JOIN restaurante.visita vi ON vi.visita_id = v.visita_id
            WHERE vi.cliente_id = :clienteId
            GROUP BY EXTRACT(YEAR FROM v.venta_fecha_hora), EXTRACT(MONTH FROM v.venta_fecha_hora)
            ORDER BY anio DESC, mes DESC
            """, nativeQuery = true)
    List<VentaAgrupadaMesView> obtenerTotalesPorMes(@Param("clienteId") Long clienteId);
}
