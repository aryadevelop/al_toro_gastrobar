package co.edu.unicauca.backend.modules.inventario.repository;

import co.edu.unicauca.backend.modules.inventario.entity.OpcionModificacion;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OpcionModificacionRepository extends JpaRepository<OpcionModificacion, Long> {

    List<OpcionModificacion> findByOpcionEstadoOrderByTipoComponenteAscOpcionNombreAsc(EstadoGenerico estado);
}
