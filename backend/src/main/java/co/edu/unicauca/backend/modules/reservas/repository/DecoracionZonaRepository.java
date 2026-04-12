package co.edu.unicauca.backend.modules.reservas.repository;

import co.edu.unicauca.backend.modules.reservas.entity.DecoracionZona;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DecoracionZonaRepository extends JpaRepository<DecoracionZona, DecoracionZona.DecoracionZonaId> {
    List<DecoracionZona> findByDecoracionId(Long decoracionId);
}
