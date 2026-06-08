package co.edu.unicauca.backend.modules.reportes.ventas_detalle.repository;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de mesa para detalle de ventas.
 */
public interface MesaDetalleAdminRepository extends JpaRepository<Mesa, Long> {
}