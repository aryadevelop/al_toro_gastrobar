package co.edu.unicauca.backend.modules.mesas_comandas.repository;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad {@link Visita}.
 *
 * <p>Provee las operaciones CRUD heredadas de {@code JpaRepository} y consultas
 * para recuperar visitas por reserva o por cliente (incluyendo walk-ins).
 *
 * @see Visita
 */
public interface VisitaRepository extends JpaRepository<Visita, Long> {

    /**
     * Busca la visita asociada a una reserva concreta.
     *
     * <p>Una reserva tiene a lo sumo una visita (el cliente se presenta una sola vez);
     * por eso el resultado es {@link Optional}.
     *
     * @param reservaId identificador de la reserva
     * @return la visita vinculada a la reserva, o {@link Optional#empty()} si no existe
     */
    Optional<Visita> findByReserva_ReservaId(Long reservaId);

    /**
     * Devuelve todas las visitas de un cliente,
     * ordenadas de la más reciente a la más antigua.
     *
     * @param clienteId identificador del cliente
     * @return lista de todas las visitas del cliente
     */
    List<Visita> findByCliente_UsuarioIdOrderByVisitaFechaHoraInicioDesc(Long clienteId);

}