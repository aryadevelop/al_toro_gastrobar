package co.edu.unicauca.backend.modules.mesas_comandas.repository;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad {@link Comanda}.
 *
 * <p>Provee las operaciones CRUD heredadas de {@code JpaRepository} y consultas
 * para recuperar comandas por visita o por reserva.
 *
 * @see Comanda
 */
public interface ComandaRepository extends JpaRepository<Comanda, Long> {

    /**
     * Devuelve todas las comandas (cocina y barra) asociadas a una visita.
     *
     * @param visitaId identificador de la visita
     * @return lista de comandas de la visita; vacía si no se generaron comandas
     */
    List<Comanda> findByVisita_VisitaId(Long visitaId);

    /**
     * Devuelve la comanda PRE_RESERVA de una reserva, si existe.
     *
     * @param reservaId identificador de la reserva
     * @param estado    estado a filtrar (típicamente {@code PRE_RESERVA})
     * @return comanda en ese estado asociada a la reserva; vacía si no existe
     */
    Optional<Comanda> findByReserva_ReservaIdAndComandaEstado(Long reservaId, EstadoComanda estado);
}
