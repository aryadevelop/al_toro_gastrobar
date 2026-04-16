package co.edu.unicauca.backend.modules.reservas.repository;

import co.edu.unicauca.backend.modules.reservas.entity.PreOrdenMenuModificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PreOrdenMenuModificacionRepository extends JpaRepository<PreOrdenMenuModificacion, Long> {

    List<PreOrdenMenuModificacion> findByPreordenDetalle_PreordenDetalleId(Long preordenDetalleId);
}
