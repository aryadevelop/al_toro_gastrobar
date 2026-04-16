package co.edu.unicauca.backend.modules.reservas.repository;

import co.edu.unicauca.backend.modules.reservas.entity.PreOrdenDetalle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio de acceso a datos para la entidad {@link PreOrdenDetalle}.
 *
 * <p>Provee las operaciones CRUD heredadas de {@code JpaRepository} y consultas
 * para recuperar los ítems de la pre-orden asociados a una reserva concreta,
 * usados al construir el resumen de pre-orden en la respuesta al cliente.
 *
 * @see PreOrdenDetalle
 * @see co.edu.unicauca.backend.modules.reservas.entity.Reserva
 */
public interface PreOrdenDetalleRepository extends JpaRepository<PreOrdenDetalle, Long> {

    /**
     * Devuelve los ítems de pre-orden de una reserva ordenados por fecha de creación ascendente.
     *
     * @param reservaId identificador de la reserva
     * @return lista de ítems de pre-orden ordenada cronológicamente; vacía si la reserva no tiene pre-orden
     */
    List<PreOrdenDetalle> findByReserva_ReservaIdOrderByCreatedAtAsc(Long reservaId);
}
