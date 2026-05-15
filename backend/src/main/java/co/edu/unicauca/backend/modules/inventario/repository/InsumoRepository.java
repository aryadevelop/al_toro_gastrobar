package co.edu.unicauca.backend.modules.inventario.repository;

import co.edu.unicauca.backend.modules.inventario.entity.Insumo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InsumoRepository extends JpaRepository<Insumo, Long> {
}
