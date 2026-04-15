package co.edu.unicauca.backend.modules.reservas.repository;

import co.edu.unicauca.backend.modules.reservas.entity.PreOrdenDetalle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PreOrdenDetalleRepository extends JpaRepository<PreOrdenDetalle, Long> {

    List<PreOrdenDetalle> findByReserva_ReservaIdOrderByCreatedAtAsc(Long reservaId);
}
