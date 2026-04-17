package co.edu.unicauca.backend.modules.pagos_caja.repository;

import co.edu.unicauca.backend.modules.pagos_caja.entity.Abono;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio de acceso a datos para la entidad {@link Abono}.
 *
 * <p>Provee las operaciones CRUD heredadas de {@code JpaRepository} y la consulta
 * para recuperar todos los abonos y devoluciones asociados a una reserva.
 *
 * @see Abono
 */
public interface AbonoRepository extends JpaRepository<Abono, Long> {

    /**
     * Devuelve todos los abonos de una reserva, ordenados del más antiguo al más reciente.
     *
     * <p>Incluye tanto anticipos como devoluciones, en orden cronológico ascendente.
     *
     * @param reservaId identificador de la reserva
     * @return lista de abonos de la reserva; vacía si no se registró ningún abono
     */
    List<Abono> findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(Long reservaId);
}
