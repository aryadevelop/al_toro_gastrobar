package co.edu.unicauca.backend.modules.reservas.repository;

import co.edu.unicauca.backend.modules.reservas.entity.Decoracion;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DecoracionRepository extends JpaRepository<Decoracion, Long> {
    List<Decoracion> findByDecoracionEstado(EstadoGenerico estado);
}
