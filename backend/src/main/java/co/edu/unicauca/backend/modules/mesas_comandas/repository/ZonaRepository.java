package co.edu.unicauca.backend.modules.mesas_comandas.repository;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ZonaRepository extends JpaRepository<Zona, Long> {
}
